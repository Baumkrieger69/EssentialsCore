package com.essentialscore.util;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility-Klasse für JVM-Optimierungen und JIT-Tuning.
 * Ermöglicht das Vorwärmen von kritischen Methoden für bessere JIT-Kompilierung.
 */
public class JvmOptimizer {
    private static final Logger LOGGER = Logger.getLogger(JvmOptimizer.class.getName());
    private static final int WARM_UP_ITERATIONS = 10_000;
    private static final boolean JVM_COMPRESSED_REFS = isCompressedRefsEnabled();
    private static final boolean IS_SERVER_VM = isServerVm();
    private static final boolean HAS_CONCURRENT_GC = hasConcurrentGc();
    
    // Lock-freier Cache für vorgewärmte Methodenreferenzen
    private static final ConcurrentHashMap<String, Boolean> warmedUpMethods = new ConcurrentHashMap<>();
    
    /**
     * Liefert Informationen über die JVM-Konfiguration.
     */
    public static Map<String, String> getJvmInfo() {
        Map<String, String> info = new HashMap<>();
        
        // JVM-Version und Art
        String vmName = System.getProperty("java.vm.name", "Unknown");
        String vmVersion = System.getProperty("java.vm.version", "Unknown");
        String javaVersion = System.getProperty("java.version", "Unknown");
        
        info.put("javaVersion", javaVersion);
        info.put("vmName", vmName);
        info.put("vmVersion", vmVersion);
        info.put("isServerVm", String.valueOf(IS_SERVER_VM));
        info.put("compressedReferences", String.valueOf(JVM_COMPRESSED_REFS));
        info.put("concurrentGC", String.valueOf(HAS_CONCURRENT_GC));
        
        // Speicherkonfiguration
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemory = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemory = memoryBean.getNonHeapMemoryUsage();
        
        info.put("heapInit", formatBytes(heapMemory.getInit()));
        info.put("heapUsed", formatBytes(heapMemory.getUsed()));
        info.put("heapMax", formatBytes(heapMemory.getMax()));
        info.put("heapCommitted", formatBytes(heapMemory.getCommitted()));
        info.put("nonHeapInit", formatBytes(nonHeapMemory.getInit()));
        info.put("nonHeapUsed", formatBytes(nonHeapMemory.getUsed()));
        info.put("nonHeapMax", formatBytes(nonHeapMemory.getMax()));
        info.put("nonHeapCommitted", formatBytes(nonHeapMemory.getCommitted()));
        
        // Thread-Informationen
        int threadCount = ManagementFactory.getThreadMXBean().getThreadCount();
        int peakThreadCount = ManagementFactory.getThreadMXBean().getPeakThreadCount();
        long totalStartedThreads = ManagementFactory.getThreadMXBean().getTotalStartedThreadCount();
        
        info.put("threadCount", String.valueOf(threadCount));
        info.put("peakThreadCount", String.valueOf(peakThreadCount));
        info.put("totalStartedThreads", String.valueOf(totalStartedThreads));
        
        return info;
    }
    
    /**
     * Formatiert Bytes in menschenlesbare Form.
     */
    private static String formatBytes(long bytes) {
        if (bytes < 0) return "N/A";
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }
    
    /**
     * Prüft, ob die JVM komprimierte Referenzen verwendet.
     */
    private static boolean isCompressedRefsEnabled() {
        String compressedRefs = System.getProperty("UseCompressedOops");
        if (compressedRefs != null) {
            return Boolean.parseBoolean(compressedRefs);
        }
        
        // Standardmäßig aktiviert für 64-Bit JVMs mit weniger als 32GB Heap
        String arch = System.getProperty("sun.arch.data.model", "");
        return "64".equals(arch) && Runtime.getRuntime().maxMemory() < 32L * 1024 * 1024 * 1024;
    }
    
    /**
     * Prüft, ob eine Server-JVM verwendet wird.
     */
    private static boolean isServerVm() {
        String vmName = System.getProperty("java.vm.name", "").toLowerCase();
        return vmName.contains("server");
    }
    
    /**
     * Prüft, ob ein Concurrent-Garbage-Collector verfügbar ist.
     */
    private static boolean hasConcurrentGc() {
        String gcInfo = System.getProperty("java.vm.info", "").toLowerCase();
        String vmArgs = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().toLowerCase();
        
        return gcInfo.contains("gc") && 
               (vmArgs.contains("g1gc") || 
                vmArgs.contains("cmsgc") || 
                vmArgs.contains("zgc") || 
                vmArgs.contains("shenandoah"));
    }
    
    /**
     * Wärmt eine Methode für optimale JIT-Kompilierung vor.
     * 
     * @param methodKey Ein eindeutiger Schlüssel für die Methode
     * @param warmupSupplier Ein Supplier, der die vorzuwärmende Methode ausführt
     */
    public static <T> void warmupMethod(String methodKey, Supplier<T> warmupSupplier) {
        // Nur einmal pro JVM-Laufzeit vorwärmen
        if (warmedUpMethods.containsKey(methodKey)) {
            return;
        }
        
        try {
            LOGGER.fine("Vorwärmen der Methode: " + methodKey);
            
            // JIT-Heuristik - Einige Warmup-Runden für den C1-Compiler (interpretierter Modus)
            for (int i = 0; i < 20; i++) {
                warmupSupplier.get();
            }
            
            // Kurze Pause, um dem Compiler Zeit zu geben
            Thread.sleep(10);
            
            // Hauptwarmup für C2-Compiler (Server-Modus)
            for (int i = 0; i < WARM_UP_ITERATIONS; i++) {
                warmupSupplier.get();
                
                // Den GC-Druck verringern, indem wir ein häufigeres Sammeln vermeiden
                if (i % 1000 == 0) {
                    Thread.sleep(1);
                }
            }
            
            // Als vorgewärmt markieren
            warmedUpMethods.put(methodKey, Boolean.TRUE);
            
            LOGGER.fine("Vorwärmen abgeschlossen für: " + methodKey);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Vorwärmen von " + methodKey, e);
        }
    }
    
    /**
     * Optimiert die Speicherverwaltung der JVM.
     */
    public static void optimizeMemory() {
        try {
            // Hinweis an den GC, ungenutzten Speicher freizugeben
            System.gc();
            
            // Kurze Pause, um dem GC Zeit zu geben
            Thread.sleep(100);
            
            // Pufferreinigungs-Hinweis
            if (System.getProperty("java.version", "").startsWith("1.8")) {
                // In Java 8 DirectByteBuffers manuell sammeln
                try {
                    Class<?> clazz = Class.forName("sun.misc.Cleaner");
                    Method cleanerMethod = Class.forName("java.nio.DirectByteBuffer").getDeclaredMethod("cleaner");
                    cleanerMethod.setAccessible(true);
                    Method cleanMethod = clazz.getDeclaredMethod("clean");
                    cleanMethod.setAccessible(true);
                    
                    LOGGER.fine("DirectByteBuffer-Cleaner aufgerufen");
                } catch (Exception e) {
                    // Nichts tun, nur ein Optimierungsversuch
                }
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler bei Speicheroptimierung", e);
        }
    }
    
    /**
     * Erstellt optimierte Voreinstellungen für Thread-Gruppen.
     */
    public static ThreadGroup createOptimizedThreadGroup(String name, int priority) {
        ThreadGroup group = new ThreadGroup(name);
        group.setMaxPriority(priority);
        return group;
    }
    
    /**
     * Gibt JVM-Empfehlungen zur optimalen Konfiguration.
     */
    public static String getJvmRecommendations() {
        StringBuilder recommendations = new StringBuilder();
        recommendations.append("JVM-Optimierungsempfehlungen:\n");
        
        // Prüfen der aktuellen Konfiguration
        if (!IS_SERVER_VM) {
            recommendations.append("- Server-JVM verwenden mit -server Flag\n");
        }
        
        if (!JVM_COMPRESSED_REFS) {
            recommendations.append("- Komprimierte Referenzen aktivieren mit -XX:+UseCompressedOops\n");
        }
        
        if (!HAS_CONCURRENT_GC) {
            recommendations.append("- G1 Garbage Collector verwenden mit -XX:+UseG1GC\n");
        }
        
        // Allgemeine Empfehlungen
        recommendations.append("- JIT-Optimierungen aktivieren mit -XX:+OptimizeStringConcat -XX:+DoEscapeAnalysis\n");
        recommendations.append("- Heap-Größe anpassen: -Xms und -Xmx auf gleichen Wert setzen\n");
        recommendations.append("- Metaspace optimieren: -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=256m\n");
        recommendations.append("- Große Seiten verwenden: -XX:+UseLargePages (wenn unterstützt)\n");
        
        return recommendations.toString();
    }
    
    /**
     * Wärmt häufig verwendete Klassen für bessere Startzeit vor.
     */
    public static void warmupCommonClasses() {
        try {
            // Häufig verwendete Klassen vorwärmen
            HashMap<String, String> map = new HashMap<>();
            map.put("key", "value");
            map.get("key");
            
            ConcurrentHashMap<String, String> concurrentMap = new ConcurrentHashMap<>();
            concurrentMap.put("key", "value");
            concurrentMap.get("key");
            
            String str = "test";
            str.toLowerCase();
            str.toUpperCase();
            str.substring(1);
            
            StringBuilder sb = new StringBuilder();
            sb.append("test");
            sb.toString();
            
            // Systemklassen
            Thread.currentThread().getContextClassLoader();
            Class.forName("java.lang.String");
            System.currentTimeMillis();
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Fehler beim Vorwärmen der Klassen", e);
        }
    }
} 