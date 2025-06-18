package com.essentialscore.util;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;
import java.util.Collection;

/**
 * Eine speichereffiziente Datenstruktur zum Testen der Zugehörigkeit zu einer Menge.
 * BloomFilter kann sicher bestätigen, wenn ein Element NICHT in der Menge ist,
 * kann aber falsch-positive Ergebnisse liefern.
 * 
 * @param <T> Typ der zu speichernden Elemente
 */
public class BloomFilter<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private final BitSet bitset;
    private final int bitSetSize;
    private final int expectedNumberOfElements;
    private final int numberOfHashFunctions;
    
    private int elementsCount;
    
    /**
     * Erstellt einen neuen BloomFilter mit optimalen Parametern für die erwartete Anzahl von Elementen
     * und eine falsch-positiv Rate von 0.01.
     * 
     * @param expectedNumberOfElements Die erwartete Anzahl der Elemente
     * @return Ein neu konfigurierter BloomFilter
     */
    public static <T> BloomFilter<T> create(int expectedNumberOfElements) {
        return new BloomFilter<T>(expectedNumberOfElements, 0.01);
    }
    
    /**
     * Erstellt einen neuen BloomFilter mit optimalen Parametern für die erwartete Anzahl von Elementen
     * und die angegebene falsch-positiv Rate.
     * 
     * @param expectedNumberOfElements Die erwartete Anzahl der Elemente
     * @param falsePositiveProbability Die akzeptable falsch-positiv Rate
     */
    public BloomFilter(int expectedNumberOfElements, double falsePositiveProbability) {
        this.expectedNumberOfElements = expectedNumberOfElements;
        
        // Berechne optimale Größe des BitSets
        this.bitSetSize = optimalBitSetSize(expectedNumberOfElements, falsePositiveProbability);
        
        // Berechne optimale Anzahl der Hash-Funktionen
        this.numberOfHashFunctions = optimalNumberOfHashFunctions(expectedNumberOfElements, bitSetSize);
        
        this.bitset = new BitSet(bitSetSize);
        this.elementsCount = 0;
    }
    
    /**
     * Fügt ein Element zum BloomFilter hinzu
     * 
     * @param element Das hinzuzufügende Element
     */
    public void add(T element) {
        add(element.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Fügt mehrere Elemente zum BloomFilter hinzu
     * 
     * @param elements Die hinzuzufügenden Elemente
     */
    public void addAll(Collection<? extends T> elements) {
        for (T element : elements) {
            add(element);
        }
    }
    
    /**
     * Fügt ein Element zum BloomFilter hinzu, repräsentiert als Byte-Array
     * 
     * @param bytes Das hinzuzufügende Element als Byte-Array
     */
    public void add(byte[] bytes) {
        int[] hashes = createHashes(bytes, numberOfHashFunctions);
        for (int hash : hashes) {
            bitset.set(Math.abs(hash % bitSetSize), true);
        }
        elementsCount++;
    }
    
    /**
     * Prüft, ob ein Element wahrscheinlich im BloomFilter enthalten ist
     * 
     * @param element Das zu prüfende Element
     * @return true, wenn das Element wahrscheinlich enthalten ist, false wenn es sicher nicht enthalten ist
     */
    public boolean mightContain(T element) {
        return mightContain(element.toString().getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Prüft, ob ein Element wahrscheinlich im BloomFilter enthalten ist
     * 
     * @param bytes Das zu prüfende Element als Byte-Array
     * @return true, wenn das Element wahrscheinlich enthalten ist, false wenn es sicher nicht enthalten ist
     */
    public boolean mightContain(byte[] bytes) {
        int[] hashes = createHashes(bytes, numberOfHashFunctions);
        for (int hash : hashes) {
            if (!bitset.get(Math.abs(hash % bitSetSize))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Setzt den BloomFilter zurück (leert ihn)
     */
    public void clear() {
        bitset.clear();
        elementsCount = 0;
    }
    
    /**
     * Erstellt die Hash-Werte für die Daten
     * 
     * @param data Die zu hashenden Daten
     * @param numberOfHashes Die Anzahl der Hash-Werte
     * @return Array mit Hash-Werten
     */
    private int[] createHashes(byte[] data, int numberOfHashes) {
        int[] result = new int[numberOfHashes];
        
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data);
            
            // Verwende die ersten 4 Bytes als Seed für weitere Hashes
            int hash1 = (digest[0] & 0xFF) | ((digest[1] & 0xFF) << 8) | ((digest[2] & 0xFF) << 16) | ((digest[3] & 0xFF) << 24);
            // Verwende die nächsten 4 Bytes als zweiten Hash
            int hash2 = (digest[4] & 0xFF) | ((digest[5] & 0xFF) << 8) | ((digest[6] & 0xFF) << 16) | ((digest[7] & 0xFF) << 24);
            
            // Kombiniere die Hashes nach Kirsch-Mitzenmacher-Methode
            for (int i = 0; i < numberOfHashes; i++) {
                result[i] = hash1 + i * hash2;
            }
            
            return result;
        } catch (NoSuchAlgorithmException e) {
            // Fallback auf einfache Hash-Methode
            int hash1 = data.hashCode();
            int hash2 = hash1 >>> 16;
            
            for (int i = 0; i < numberOfHashes; i++) {
                result[i] = hash1 + i * hash2;
            }
            
            return result;
        }
    }
    
    /**
     * Berechnet die optimale Größe des BitSets basierend auf der erwarteten Anzahl von Elementen
     * und der gewünschten falsch-positiv Rate
     * 
     * @param n Anzahl der erwarteten Elemente
     * @param p Falsch-positiv-Wahrscheinlichkeit
     * @return Optimale Größe des BitSets
     */
    private int optimalBitSetSize(int n, double p) {
        return (int) Math.ceil((n * Math.log(p)) / Math.log(1 / Math.pow(2, Math.log(2))));
    }
    
    /**
     * Berechnet die optimale Anzahl der Hash-Funktionen
     * 
     * @param n Anzahl der erwarteten Elemente
     * @param m Größe des BitSets
     * @return Optimale Anzahl der Hash-Funktionen
     */
    private int optimalNumberOfHashFunctions(int n, int m) {
        return Math.max(1, (int) Math.round((m / n) * Math.log(2)));
    }
    
    /**
     * Gibt die aktuelle Anzahl der Elemente zurück
     * 
     * @return Anzahl der Elemente
     */
    public int size() {
        return elementsCount;
    }
    
    /**
     * Gibt die erwartete Anzahl von Elementen zurück
     * 
     * @return Erwartete Anzahl von Elementen
     */
    public int getExpectedNumberOfElements() {
        return expectedNumberOfElements;
    }
    
    /**
     * Gibt die Größe des BitSets zurück
     * 
     * @return Größe des BitSets
     */
    public int getBitSetSize() {
        return bitSetSize;
    }
    
    /**
     * Gibt die Anzahl der Hash-Funktionen zurück
     * 
     * @return Anzahl der Hash-Funktionen
     */
    public int getNumberOfHashFunctions() {
        return numberOfHashFunctions;
    }
    
    /**
     * Berechnet die aktuelle falsch-positiv-Wahrscheinlichkeit basierend auf der tatsächlichen Anzahl der Elemente
     * 
     * @return Aktuelle falsch-positiv-Wahrscheinlichkeit
     */
    public double getCurrentFalsePositiveProbability() {
        // (1 - e^(-k * n / m)) ^ k
        return Math.pow(1 - Math.exp(-numberOfHashFunctions * elementsCount / (double) bitSetSize), numberOfHashFunctions);
    }
} 
