package com.essentialscore.api.security;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides Two-Factor Authentication for securing administrative functions.
 */
public class TwoFactorAuthentication {
    private static final Logger LOGGER = Logger.getLogger(TwoFactorAuthentication.class.getName());
    
    // TOTP parameters
    private static final String HMAC_ALGORITHM = "HmacSHA1";
    private static final int CODE_DIGITS = 6;
    private static final int TIME_STEP_SECONDS = 30;
    
    // Recovery code settings
    private static final int RECOVERY_CODE_COUNT = 10;
    private static final int RECOVERY_CODE_LENGTH = 8;
    
    // Verification settings
    private static final int MAX_VERIFICATION_ATTEMPTS = 3;
    private static final long LOCKOUT_DURATION_MILLIS = TimeUnit.MINUTES.toMillis(5);
    
    private final Plugin plugin;
    private final File dataFile;
    private final Map<UUID, TwoFactorData> twoFactorData;
    private final Map<UUID, VerificationState> verificationStates;
    private final SecureRandom secureRandom;
    private final DataEncryption encryption;
    
    /**
     * Creates a new Two-Factor Authentication system.
     *
     * @param plugin The plugin
     * @param encryption The data encryption system
     */
    public TwoFactorAuthentication(Plugin plugin, DataEncryption encryption) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "security/2fa.yml");
        this.twoFactorData = new ConcurrentHashMap<>();
        this.verificationStates = new ConcurrentHashMap<>();
        this.secureRandom = new SecureRandom();
        this.encryption = encryption;
    }
    
    /**
     * Initializes the Two-Factor Authentication system.
     */
    public void initialize() {
        // Create data directory if it doesn't exist
        File securityDir = new File(plugin.getDataFolder(), "security");
        if (!securityDir.exists()) {
            securityDir.mkdirs();
        }
        
        // Load data
        loadData();
        
        // Schedule cleanup of expired verification states
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(
                plugin,
                this::cleanupExpiredStates,
                20 * 60, // 1 minute delay
                20 * 60 * 10 // Run every 10 minutes
        );
        
        LOGGER.info("Two-Factor Authentication system initialized");
    }
    
    /**
     * Enables Two-Factor Authentication for a player.
     *
     * @param player The player
     * @return A map containing the secret key and recovery codes
     */
    public Map<String, Object> enableTwoFactor(Player player) {
        UUID playerId = player.getUniqueId();
        
        // Check if already enabled
        if (twoFactorData.containsKey(playerId)) {
            return null;
        }
        
        // Generate secret key
        byte[] secretKey = generateSecretKey();
        String secretKeyBase32 = base32Encode(secretKey);
        
        // Generate recovery codes
        String[] recoveryCodes = generateRecoveryCodes();
        
        // Create 2FA data
        TwoFactorData data = new TwoFactorData(secretKey, recoveryCodes);
        twoFactorData.put(playerId, data);
        
        // Save data
        saveData();
        
        // Create result with secret key and recovery codes
        Map<String, Object> result = new HashMap<>();
        result.put("secretKey", secretKeyBase32);
        result.put("recoveryCodes", recoveryCodes);
        
        // Create TOTP URI for QR code
        String totpUri = String.format("otpauth://totp/%s:%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                plugin.getName(),
                player.getName(),
                secretKeyBase32,
                plugin.getName(),
                CODE_DIGITS,
                TIME_STEP_SECONDS);
        result.put("totpUri", totpUri);
        
        return result;
    }
    
    /**
     * Disables Two-Factor Authentication for a player.
     *
     * @param playerId The player ID
     * @return True if disabled successfully
     */
    public boolean disableTwoFactor(UUID playerId) {
        // Check if enabled
        if (!twoFactorData.containsKey(playerId)) {
            return false;
        }
        
        // Remove 2FA data
        twoFactorData.remove(playerId);
        
        // Save data
        saveData();
        
        return true;
    }
    
    /**
     * Verifies a TOTP code for a player.
     *
     * @param playerId The player ID
     * @param code The TOTP code
     * @return True if the code is valid
     */
    public boolean verifyCode(UUID playerId, String code) {
        // Check if 2FA is enabled
        TwoFactorData data = twoFactorData.get(playerId);
        if (data == null) {
            return false;
        }
        
        // Check verification state
        VerificationState state = getVerificationState(playerId);
        if (state.isLockedOut()) {
            return false;
        }
        
        // Clean input code
        code = code.replaceAll("\\s+", "");
        
        // Check for recovery code
        if (isRecoveryCode(data, code)) {
            // Mark recovery code as used
            markRecoveryCodeUsed(data, code);
            
            // Reset verification state
            state.resetAttempts();
            
            // Save data
            saveData();
            
            return true;
        }
        
        // Verify TOTP code
        boolean valid = validateTOTP(data.secretKey, code);
        
        if (valid) {
            // Reset verification state
            state.resetAttempts();
        } else {
            // Increment failed attempts
            state.incrementAttempts();
            
            // Save state if locked out
            if (state.isLockedOut()) {
                LOGGER.warning("Player locked out from 2FA: " + playerId);
            }
        }
        
        return valid;
    }
    
    /**
     * Checks if a player has Two-Factor Authentication enabled.
     *
     * @param playerId The player ID
     * @return True if enabled
     */
    public boolean isTwoFactorEnabled(UUID playerId) {
        return twoFactorData.containsKey(playerId);
    }
    
    /**
     * Gets the verification state for a player.
     *
     * @param playerId The player ID
     * @return The verification state
     */
    private VerificationState getVerificationState(UUID playerId) {
        VerificationState state = verificationStates.get(playerId);
        
        if (state == null) {
            state = new VerificationState();
            verificationStates.put(playerId, state);
        }
        
        return state;
    }
    
    /**
     * Checks if a code is a valid recovery code.
     *
     * @param data The 2FA data
     * @param code The code to check
     * @return True if it's a valid recovery code
     */
    private boolean isRecoveryCode(TwoFactorData data, String code) {
        for (String recoveryCode : data.recoveryCodes) {
            if (recoveryCode != null && !recoveryCode.startsWith("USED:") && recoveryCode.equals(code)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Marks a recovery code as used.
     *
     * @param data The 2FA data
     * @param code The code to mark as used
     */
    private void markRecoveryCodeUsed(TwoFactorData data, String code) {
        for (int i = 0; i < data.recoveryCodes.length; i++) {
            if (data.recoveryCodes[i] != null && data.recoveryCodes[i].equals(code)) {
                data.recoveryCodes[i] = "USED:" + code;
                break;
            }
        }
    }
    
    /**
     * Generates new recovery codes for a player.
     *
     * @param playerId The player ID
     * @return The new recovery codes, or null if 2FA is not enabled
     */
    public String[] regenerateRecoveryCodes(UUID playerId) {
        // Check if 2FA is enabled
        TwoFactorData data = twoFactorData.get(playerId);
        if (data == null) {
            return null;
        }
        
        // Generate new recovery codes
        data.recoveryCodes = generateRecoveryCodes();
        
        // Save data
        saveData();
        
        return data.recoveryCodes.clone();
    }
    
    /**
     * Loads 2FA data from disk.
     */
    private void loadData() {
        if (!dataFile.exists()) {
            return;
        }
        
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
            
            for (String uuidString : config.getKeys(false)) {
                try {
                    UUID playerId = UUID.fromString(uuidString);
                    
                    // Get encrypted data
                    String encryptedData = config.getString(uuidString);
                    
                    // Decrypt data
                    String decryptedData = encryption.decryptFromStorage(encryptedData);
                    if (decryptedData == null) {
                        LOGGER.warning("Failed to decrypt 2FA data for player: " + uuidString);
                        continue;
                    }
                    
                    // Parse data
                    String[] parts = decryptedData.split("\\|");
                    if (parts.length != 2) {
                        LOGGER.warning("Invalid 2FA data format for player: " + uuidString);
                        continue;
                    }
                    
                    // Decode secret key
                    byte[] secretKey = Base64.getDecoder().decode(parts[0]);
                    
                    // Parse recovery codes
                    String[] recoveryCodes = parts[1].split(",");
                    
                    // Create 2FA data
                    TwoFactorData data = new TwoFactorData(secretKey, recoveryCodes);
                    twoFactorData.put(playerId, data);
                } catch (IllegalArgumentException e) {
                    LOGGER.warning("Invalid UUID in 2FA data file: " + uuidString);
                }
            }
            
            LOGGER.info("Loaded 2FA data for " + twoFactorData.size() + " players");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load 2FA data", e);
        }
    }
    
    /**
     * Saves 2FA data to disk.
     */
    private void saveData() {
        try {
            YamlConfiguration config = new YamlConfiguration();
            
            for (Map.Entry<UUID, TwoFactorData> entry : twoFactorData.entrySet()) {
                TwoFactorData data = entry.getValue();
                
                // Encode secret key
                String secretKeyBase64 = Base64.getEncoder().encodeToString(data.secretKey);
                
                // Encode recovery codes
                StringBuilder recoveryCodesStr = new StringBuilder();
                for (int i = 0; i < data.recoveryCodes.length; i++) {
                    if (i > 0) {
                        recoveryCodesStr.append(",");
                    }
                    recoveryCodesStr.append(data.recoveryCodes[i]);
                }
                
                // Combine data
                String dataStr = secretKeyBase64 + "|" + recoveryCodesStr.toString();
                
                // Encrypt data
                String encryptedData = encryption.encryptForStorage(dataStr);
                
                // Store encrypted data
                config.set(entry.getKey().toString(), encryptedData);
            }
            
            // Save to file
            config.save(dataFile);
            
            LOGGER.fine("Saved 2FA data for " + twoFactorData.size() + " players");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to save 2FA data", e);
        }
    }
    
    /**
     * Generates a secure random secret key.
     *
     * @return The secret key
     */
    private byte[] generateSecretKey() {
        byte[] key = new byte[20]; // 160 bits for SHA1 HMAC
        secureRandom.nextBytes(key);
        return key;
    }
    
    /**
     * Generates random recovery codes.
     *
     * @return The recovery codes
     */
    private String[] generateRecoveryCodes() {
        String[] codes = new String[RECOVERY_CODE_COUNT];
        
        for (int i = 0; i < RECOVERY_CODE_COUNT; i++) {
            codes[i] = generateRecoveryCode();
        }
        
        return codes;
    }
    
    /**
     * Generates a single recovery code.
     *
     * @return The recovery code
     */
    private String generateRecoveryCode() {
        // Use only digits and uppercase letters (excluding confusing chars like O, 0, I, 1)
        char[] allowedChars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
        
        StringBuilder code = new StringBuilder();
        
        for (int i = 0; i < RECOVERY_CODE_LENGTH; i++) {
            code.append(allowedChars[secureRandom.nextInt(allowedChars.length)]);
            
            // Add hyphen after 4 characters
            if (i == 3) {
                code.append('-');
            }
        }
        
        return code.toString();
    }
    
    /**
     * Validates a TOTP code.
     *
     * @param secretKey The secret key
     * @param code The code to validate
     * @return True if the code is valid
     */
    private boolean validateTOTP(byte[] secretKey, String code) {
        if (code == null || code.length() != CODE_DIGITS) {
            return false;
        }
        
        // Parse code
        int codeInt;
        try {
            codeInt = Integer.parseInt(code);
        } catch (NumberFormatException e) {
            return false;
        }
        
        // Get current timestamp
        long currentTimeMillis = System.currentTimeMillis();
        
        // Check current and adjacent time steps (to account for clock skew)
        for (int window = -1; window <= 1; window++) {
            long timeWindow = currentTimeMillis + (window * TIME_STEP_SECONDS * 1000L);
            if (generateTOTP(secretKey, timeWindow) == codeInt) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Generates a TOTP code for a given time.
     *
     * @param secretKey The secret key
     * @param timeMillis The time in milliseconds
     * @return The TOTP code
     */
    private int generateTOTP(byte[] secretKey, long timeMillis) {
        try {
            // Calculate time step
            long timeStep = timeMillis / (TIME_STEP_SECONDS * 1000);
            
            // Convert time step to byte array
            byte[] timeBytes = ByteBuffer.allocate(8).putLong(timeStep).array();
            
            // Create HMAC
            Mac hmac = Mac.getInstance(HMAC_ALGORITHM);
            hmac.init(new SecretKeySpec(secretKey, HMAC_ALGORITHM));
            byte[] hash = hmac.doFinal(timeBytes);
            
            // Get offset
            int offset = hash[hash.length - 1] & 0xF;
            
            // Extract 4 bytes from hash starting at offset
            int binary = ((hash[offset] & 0x7F) << 24) |
                         ((hash[offset + 1] & 0xFF) << 16) |
                         ((hash[offset + 2] & 0xFF) << 8) |
                         (hash[offset + 3] & 0xFF);
            
            // Get last CODE_DIGITS digits
            int code = binary % (int) Math.pow(10, CODE_DIGITS);
            
            return code;
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            LOGGER.log(Level.SEVERE, "Error generating TOTP", e);
            return -1;
        }
    }
    
    /**
     * Cleans up expired verification states.
     */
    private void cleanupExpiredStates() {
        long now = System.currentTimeMillis();
        
        verificationStates.entrySet().removeIf(entry -> {
            VerificationState state = entry.getValue();
            return state.getLastAttemptTime() + LOCKOUT_DURATION_MILLIS < now;
        });
    }
    
    /**
     * Encodes data in Base32 (RFC 4648).
     *
     * @param data The data to encode
     * @return The Base32-encoded string
     */
    private String base32Encode(byte[] data) {
        final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        StringBuilder result = new StringBuilder();
        
        int buffer = 0;
        int bitsLeft = 0;
        
        for (byte b : data) {
            buffer = (buffer << 8) | (b & 0xFF);
            bitsLeft += 8;
            
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                result.append(ALPHABET.charAt(index));
                bitsLeft -= 5;
            }
        }
        
        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            result.append(ALPHABET.charAt(index));
        }
        
        return result.toString();
    }
    
    /**
     * Class to hold Two-Factor Authentication data.
     */
    private static class TwoFactorData {
        private final byte[] secretKey;
        private String[] recoveryCodes;
        
        /**
         * Creates new 2FA data.
         *
         * @param secretKey The secret key
         * @param recoveryCodes The recovery codes
         */
        public TwoFactorData(byte[] secretKey, String[] recoveryCodes) {
            this.secretKey = secretKey;
            this.recoveryCodes = recoveryCodes;
        }
    }
    
    /**
     * Class to track verification state.
     */
    private static class VerificationState {
        private int attempts;
        private long lastAttemptTime;
        
        /**
         * Creates a new verification state.
         */
        public VerificationState() {
            this.attempts = 0;
            this.lastAttemptTime = 0;
        }
        
        /**
         * Increments the attempt counter.
         */
        public void incrementAttempts() {
            attempts++;
            lastAttemptTime = System.currentTimeMillis();
        }
        
        /**
         * Resets the attempt counter.
         */
        public void resetAttempts() {
            attempts = 0;
        }
        
        /**
         * Checks if the account is locked out.
         *
         * @return True if locked out
         */
        public boolean isLockedOut() {
            if (attempts >= MAX_VERIFICATION_ATTEMPTS) {
                // Check if lockout period has expired
                if (System.currentTimeMillis() > lastAttemptTime + LOCKOUT_DURATION_MILLIS) {
                    // Reset attempts if lockout has expired
                    resetAttempts();
                    return false;
                }
                return true;
            }
            return false;
        }
        
        /**
         * Gets the time of the last attempt.
         *
         * @return The last attempt time
         */
        public long getLastAttemptTime() {
            return lastAttemptTime;
        }
    }
} 
