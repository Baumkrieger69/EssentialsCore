package com.essentialscore.api.backup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles deduplication of backup data using content-based chunking.
 */
public class DedupStore {
    private static final Logger LOGGER = Logger.getLogger(DedupStore.class.getName());
    private static final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
    
    private final File storeDirectory;
    private final Map<String, Integer> referenceCount;
    
    /**
     * Creates a new deduplication store.
     *
     * @param storeDirectory The store directory
     */
    public DedupStore(File storeDirectory) {
        this.storeDirectory = storeDirectory;
        this.referenceCount = new ConcurrentHashMap<>();
        
        if (!storeDirectory.exists()) {
            storeDirectory.mkdirs();
        }
        
        // Load reference counts
        loadReferenceCount();
    }
    
    /**
     * Loads the reference count for all chunks.
     */
    private void loadReferenceCount() {
        File[] chunkFiles = storeDirectory.listFiles((dir, name) -> name.endsWith(".chunk"));
        if (chunkFiles != null) {
            for (File chunk : chunkFiles) {
                String hash = chunk.getName().replace(".chunk", "");
                referenceCount.put(hash, 1); // Start with 1 reference
            }
        }
        
        // Try to load the reference count file
        File refCountFile = new File(storeDirectory, "refcount.dat");
        if (refCountFile.exists()) {
            try (InputStream in = new BufferedInputStream(new FileInputStream(refCountFile))) {
                byte[] data = new byte[(int) refCountFile.length()];
                in.read(data);
                String content = new String(data);
                
                String[] lines = content.split("\n");
                for (String line : lines) {
                    String[] parts = line.split(":");
                    if (parts.length == 2) {
                        String hash = parts[0];
                        int count = Integer.parseInt(parts[1]);
                        referenceCount.put(hash, count);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load reference count file", e);
            }
        }
    }
    
    /**
     * Saves the reference count for all chunks.
     */
    private void saveReferenceCount() {
        File refCountFile = new File(storeDirectory, "refcount.dat");
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(refCountFile))) {
            for (Map.Entry<String, Integer> entry : referenceCount.entrySet()) {
                String line = entry.getKey() + ":" + entry.getValue() + "\n";
                out.write(line.getBytes());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to save reference count file", e);
        }
    }
    
    /**
     * Stores a file in the deduplication store.
     *
     * @param file The file to store
     * @return A map of chunk hashes to positions
     * @throws IOException If an error occurs
     */
    public Map<String, Long> storeFile(File file) throws IOException {
        Map<String, Long> chunks = new HashMap<>();
        
        try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[CHUNK_SIZE];
            int bytesRead;
            long position = 0;
            
            while ((bytesRead = in.read(buffer)) > 0) {
                byte[] chunk = new byte[bytesRead];
                System.arraycopy(buffer, 0, chunk, 0, bytesRead);
                
                String hash = calculateHash(chunk);
                chunks.put(hash, position);
                
                // Store the chunk if it doesn't exist
                File chunkFile = new File(storeDirectory, hash + ".chunk");
                if (!chunkFile.exists()) {
                    try (OutputStream out = new FileOutputStream(chunkFile)) {
                        out.write(chunk);
                    }
                }
                
                // Increment reference count
                referenceCount.compute(hash, (k, v) -> v == null ? 1 : v + 1);
                
                position += bytesRead;
            }
        }
        
        // Save updated reference counts
        saveReferenceCount();
        
        return chunks;
    }
    
    /**
     * Retrieves a file from the deduplication store.
     *
     * @param chunks A map of chunk hashes to positions
     * @param outputFile The output file
     * @throws IOException If an error occurs
     */
    public void retrieveFile(Map<String, Long> chunks, File outputFile) throws IOException {
        // Create parent directories
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }
        
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(outputFile))) {
            // Sort chunks by position
            chunks.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry -> {
                    try {
                        String hash = entry.getKey();
                        File chunkFile = new File(storeDirectory, hash + ".chunk");
                        
                        if (chunkFile.exists()) {
                            byte[] data = Files.readAllBytes(chunkFile.toPath());
                            out.write(data);
                        } else {
                            LOGGER.warning("Chunk not found: " + hash);
                        }
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Failed to retrieve chunk", e);
                    }
                });
        }
    }
    
    /**
     * Removes a file from the deduplication store.
     *
     * @param chunks A map of chunk hashes to positions
     */
    public void removeFile(Map<String, Long> chunks) {
        for (String hash : chunks.keySet()) {
            // Decrement reference count
            int count = referenceCount.compute(hash, (k, v) -> v == null ? 0 : v - 1);
            
            // Remove chunk if reference count reaches 0
            if (count <= 0) {
                File chunkFile = new File(storeDirectory, hash + ".chunk");
                if (chunkFile.exists()) {
                    chunkFile.delete();
                }
                referenceCount.remove(hash);
            }
        }
        
        // Save updated reference counts
        saveReferenceCount();
    }
    
    /**
     * Calculates the SHA-256 hash of a byte array.
     *
     * @param data The data to hash
     * @return The hash as a hexadecimal string
     */
    private String calculateHash(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            
            // Convert to hexadecimal
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.SEVERE, "SHA-256 algorithm not found", e);
            return null;
        }
    }
} 
