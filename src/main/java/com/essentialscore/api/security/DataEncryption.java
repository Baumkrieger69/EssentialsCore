package com.essentialscore.api.security;

import org.bukkit.plugin.Plugin;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides encryption capabilities for securing sensitive data 
 * both at-rest and in-transit.
 */
public class DataEncryption {
    private static final Logger LOGGER = Logger.getLogger(DataEncryption.class.getName());
    
    // AES-GCM parameters
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int AES_KEY_SIZE = 256;
    
    // Salt for key derivation
    private static final int SALT_LENGTH = 16;
    
    // Key derivation parameters
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH = 256;
    
    private final Plugin plugin;
    private final File keyStoreFile;
    private final String keyStorePassword;
    private KeyStore keyStore;
    private final Map<String, SecretKey> cachedKeys;
    private final SecureRandom secureRandom;
    
    /**
     * Creates a new data encryption system.
     *
     * @param plugin The plugin
     * @param keyStorePassword The keystore password
     */
    public DataEncryption(Plugin plugin, String keyStorePassword) {
        this.plugin = plugin;
        this.keyStoreFile = new File(plugin.getDataFolder(), "security/keystore.jks");
        this.keyStorePassword = keyStorePassword;
        this.cachedKeys = new HashMap<>();
        this.secureRandom = new SecureRandom();
    }
    
    /**
     * Initializes the encryption system.
     *
     * @return True if initialization succeeded
     */
    public boolean initialize() {
        try {
            // Create security directory if it doesn't exist
            File securityDir = new File(plugin.getDataFolder(), "security");
            if (!securityDir.exists()) {
                securityDir.mkdirs();
            }
            
            // Initialize or load keystore
            keyStore = KeyStore.getInstance("JCEKS");
            
            if (keyStoreFile.exists()) {
                // Load existing keystore
                try (FileInputStream fis = new FileInputStream(keyStoreFile)) {
                    keyStore.load(fis, keyStorePassword.toCharArray());
                    LOGGER.info("Loaded encryption keystore");
                }
            } else {
                // Create new keystore
                keyStore.load(null, keyStorePassword.toCharArray());
                
                // Generate default encryption key
                generateKey("default", keyStorePassword);
                
                // Save keystore
                saveKeyStore();
                LOGGER.info("Created new encryption keystore with default key");
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize encryption system", e);
            return false;
        }
    }
    
    /**
     * Saves the keystore to disk.
     */
    private void saveKeyStore() {
        try (FileOutputStream fos = new FileOutputStream(keyStoreFile)) {
            keyStore.store(fos, keyStorePassword.toCharArray());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to save keystore", e);
        }
    }
    
    /**
     * Generates a new encryption key.
     *
     * @param alias The key alias
     * @param password The key password
     * @return True if key generation succeeded
     */
    public boolean generateKey(String alias, String password) {
        try {
            // Generate a new AES key
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(AES_KEY_SIZE, secureRandom);
            SecretKey key = keyGen.generateKey();
            
            // Store in keystore
            KeyStore.SecretKeyEntry entry = new KeyStore.SecretKeyEntry(key);
            KeyStore.PasswordProtection protection = new KeyStore.PasswordProtection(password.toCharArray());
            keyStore.setEntry(alias, entry, protection);
            
            // Save keystore
            saveKeyStore();
            
            // Cache the key
            cachedKeys.put(alias, key);
            
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to generate encryption key: " + alias, e);
            return false;
        }
    }
    
    /**
     * Gets a key from the keystore.
     *
     * @param alias The key alias
     * @param password The key password
     * @return The secret key, or null if not found
     */
    private SecretKey getKey(String alias, String password) {
        // Check cache first
        if (cachedKeys.containsKey(alias)) {
            return cachedKeys.get(alias);
        }
        
        try {
            // Get from keystore
            KeyStore.PasswordProtection protection = new KeyStore.PasswordProtection(password.toCharArray());
            KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) keyStore.getEntry(alias, protection);
            
            if (entry != null) {
                // Cache the key
                cachedKeys.put(alias, entry.getSecretKey());
                return entry.getSecretKey();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get encryption key: " + alias, e);
        }
        
        return null;
    }
    
    /**
     * Encrypts a string using AES-GCM.
     *
     * @param plaintext The string to encrypt
     * @param keyAlias The key alias
     * @param keyPassword The key password
     * @return The Base64-encoded encrypted string, or null if encryption failed
     */
    public String encryptString(String plaintext, String keyAlias, String keyPassword) {
        try {
            // Get encryption key
            SecretKey key = getKey(keyAlias, keyPassword);
            if (key == null) {
                return null;
            }
            
            // Generate a nonce (IV)
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // Create GCM parameter spec
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);
            
            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV and ciphertext
            byte[] encrypted = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(ciphertext, 0, encrypted, iv.length, ciphertext.length);
            
            // Encode as Base64
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Encryption failed", e);
            return null;
        }
    }
    
    /**
     * Encrypts a string using the default key.
     *
     * @param plaintext The string to encrypt
     * @return The Base64-encoded encrypted string, or null if encryption failed
     */
    public String encryptString(String plaintext) {
        return encryptString(plaintext, "default", keyStorePassword);
    }
    
    /**
     * Decrypts a string using AES-GCM.
     *
     * @param encrypted The Base64-encoded encrypted string
     * @param keyAlias The key alias
     * @param keyPassword The key password
     * @return The decrypted string, or null if decryption failed
     */
    public String decryptString(String encrypted, String keyAlias, String keyPassword) {
        try {
            // Get encryption key
            SecretKey key = getKey(keyAlias, keyPassword);
            if (key == null) {
                return null;
            }
            
            // Decode from Base64
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            
            // Extract IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, iv.length);
            
            // Extract ciphertext
            byte[] ciphertext = new byte[decoded.length - iv.length];
            System.arraycopy(decoded, iv.length, ciphertext, 0, ciphertext.length);
            
            // Create GCM parameter spec
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);
            
            // Decrypt
            byte[] plaintext = cipher.doFinal(ciphertext);
            
            // Convert to string
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Decryption failed", e);
            return null;
        }
    }
    
    /**
     * Decrypts a string using the default key.
     *
     * @param encrypted The Base64-encoded encrypted string
     * @return The decrypted string, or null if decryption failed
     */
    public String decryptString(String encrypted) {
        return decryptString(encrypted, "default", keyStorePassword);
    }
    
    /**
     * Encrypts a file using AES-GCM.
     *
     * @param inputFile The input file
     * @param outputFile The output file
     * @param keyAlias The key alias
     * @param keyPassword The key password
     * @return True if encryption succeeded
     */
    public boolean encryptFile(File inputFile, File outputFile, String keyAlias, String keyPassword) {
        try {
            // Get encryption key
            SecretKey key = getKey(keyAlias, keyPassword);
            if (key == null) {
                return false;
            }
            
            // Read input file
            byte[] plaintext = Files.readAllBytes(inputFile.toPath());
            
            // Generate a nonce (IV)
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // Create GCM parameter spec
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);
            
            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext);
            
            // Write IV and ciphertext to output file
            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(iv);
                fos.write(ciphertext);
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "File encryption failed: " + inputFile.getPath(), e);
            return false;
        }
    }
    
    /**
     * Encrypts a file using the default key.
     *
     * @param inputFile The input file
     * @param outputFile The output file
     * @return True if encryption succeeded
     */
    public boolean encryptFile(File inputFile, File outputFile) {
        return encryptFile(inputFile, outputFile, "default", keyStorePassword);
    }
    
    /**
     * Decrypts a file using AES-GCM.
     *
     * @param inputFile The encrypted input file
     * @param outputFile The output file
     * @param keyAlias The key alias
     * @param keyPassword The key password
     * @return True if decryption succeeded
     */
    public boolean decryptFile(File inputFile, File outputFile, String keyAlias, String keyPassword) {
        try {
            // Get encryption key
            SecretKey key = getKey(keyAlias, keyPassword);
            if (key == null) {
                return false;
            }
            
            // Read encrypted file
            byte[] encrypted = Files.readAllBytes(inputFile.toPath());
            
            // Extract IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encrypted, 0, iv, 0, iv.length);
            
            // Extract ciphertext
            byte[] ciphertext = new byte[encrypted.length - iv.length];
            System.arraycopy(encrypted, iv.length, ciphertext, 0, ciphertext.length);
            
            // Create GCM parameter spec
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);
            
            // Decrypt
            byte[] plaintext = cipher.doFinal(ciphertext);
            
            // Write to output file
            Files.write(outputFile.toPath(), plaintext);
            
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "File decryption failed: " + inputFile.getPath(), e);
            return false;
        }
    }
    
    /**
     * Decrypts a file using the default key.
     *
     * @param inputFile The encrypted input file
     * @param outputFile The output file
     * @return True if decryption succeeded
     */
    public boolean decryptFile(File inputFile, File outputFile) {
        return decryptFile(inputFile, outputFile, "default", keyStorePassword);
    }
    
    /**
     * Derives a key from a password.
     *
     * @param password The password
     * @param salt The salt
     * @return The derived key
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public SecretKey deriveKeyFromPassword(String password, byte[] salt)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }
    
    /**
     * Generates a random salt.
     *
     * @return The salt
     */
    public byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        secureRandom.nextBytes(salt);
        return salt;
    }
    
    /**
     * Encrypts a string with a password.
     *
     * @param plaintext The string to encrypt
     * @param password The password
     * @return A map containing the Base64-encoded encrypted string, salt, and algorithm
     */
    public Map<String, String> encryptWithPassword(String plaintext, String password) {
        try {
            // Generate salt
            byte[] salt = generateSalt();
            
            // Derive key from password
            SecretKey key = deriveKeyFromPassword(password, salt);
            
            // Generate a nonce (IV)
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);
            
            // Create GCM parameter spec
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            // Initialize cipher for encryption
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);
            
            // Encrypt
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            
            // Combine IV and ciphertext
            byte[] encrypted = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, encrypted, 0, iv.length);
            System.arraycopy(ciphertext, 0, encrypted, iv.length, ciphertext.length);
            
            // Create result map
            Map<String, String> result = new HashMap<>();
            result.put("ciphertext", Base64.getEncoder().encodeToString(encrypted));
            result.put("salt", Base64.getEncoder().encodeToString(salt));
            result.put("algorithm", "AES/GCM/NoPadding");
            
            return result;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Password-based encryption failed", e);
            return null;
        }
    }
    
    /**
     * Decrypts a string with a password.
     *
     * @param encryptedData A map containing the Base64-encoded encrypted string, salt, and algorithm
     * @param password The password
     * @return The decrypted string, or null if decryption failed
     */
    public String decryptWithPassword(Map<String, String> encryptedData, String password) {
        try {
            // Extract encrypted data
            String ciphertextBase64 = encryptedData.get("ciphertext");
            String saltBase64 = encryptedData.get("salt");
            
            if (ciphertextBase64 == null || saltBase64 == null) {
                throw new IllegalArgumentException("Missing required encryption data");
            }
            
            byte[] encrypted = Base64.getDecoder().decode(ciphertextBase64);
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            
            // Derive key from password
            SecretKey key = deriveKeyFromPassword(password, salt);
            
            // Extract IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encrypted, 0, iv, 0, iv.length);
            
            // Extract ciphertext
            byte[] ciphertext = new byte[encrypted.length - iv.length];
            System.arraycopy(encrypted, iv.length, ciphertext, 0, ciphertext.length);
            
            // Create GCM parameter spec
            GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            
            // Initialize cipher for decryption
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);
            
            // Decrypt
            byte[] plaintext = cipher.doFinal(ciphertext);
            
            // Convert to string
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Password-based decryption failed", e);
            return null;
        }
    }
    
    /**
     * Encrypts sensitive data for storage.
     *
     * @param data The data to encrypt
     * @return The encrypted data, or null if encryption failed
     */
    public String encryptForStorage(String data) {
        return encryptString(data);
    }
    
    /**
     * Decrypts sensitive data from storage.
     *
     * @param encryptedData The encrypted data
     * @return The decrypted data, or null if decryption failed
     */
    public String decryptFromStorage(String encryptedData) {
        return decryptString(encryptedData);
    }
    
    /**
     * Generates a secure random token.
     *
     * @param length The token length in bytes
     * @return The Base64-encoded token
     */
    public String generateSecureToken(int length) {
        byte[] token = new byte[length];
        secureRandom.nextBytes(token);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
    }
} 
