package com.android.insecurebankv2;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.KeyStore;

/**
 * Manages cryptographic keys using Android KeyStore
 * Keys are stored securely on the device and cannot be extracted
 * @author Security Team
 */
public class KeyStoreManager {

    private static final String KEYSTORE_ALIAS = "InsecureBank_Key_v2";
    private static final String KEYSTORE_PROVIDER = "AndroidKeyStore";
    private static final int KEY_SIZE = 256; // 256-bit AES key

    private static KeyStoreManager instance;
    private KeyStore keyStore;

    private KeyStoreManager() {
        initializeKeyStore();
    }

    /**
     * Singleton pattern - ensures only one KeyStoreManager instance
     */
    public static synchronized KeyStoreManager getInstance() {
        if (instance == null) {
            instance = new KeyStoreManager();
        }
        return instance;
    }

    /**
     * Initialize and load the Android KeyStore
     */
    private void initializeKeyStore() {
        try {
            keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER);
            keyStore.load(null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize KeyStore", e);
        }
    }

    /**
     * Get or create an encryption key
     * If key exists, retrieves it; otherwise, generates a new one
     * 
     * @return SecretKey for encryption/decryption
     */
    public SecretKey getOrCreateEncryptionKey() {
        try {
            // Check if key already exists
            if (keyStore.containsAlias(KEYSTORE_ALIAS)) {
                return (SecretKey) keyStore.getKey(KEYSTORE_ALIAS, null);
            }

            // Generate new key if it doesn't exist
            return generateNewEncryptionKey();

        } catch (Exception e) {
            throw new RuntimeException("Failed to get encryption key", e);
        }
    }

    /**
     * Generate a new 256-bit AES key for encryption
     * 
     * Key properties:
     * - Size: 256 bits (strong encryption)
     * - Block mode: GCM (provides authentication)
     * - Padding: None (GCM doesn't use padding)
     * - Randomized encryption: Each encryption produces different output
     * 
     * @return newly generated SecretKey
     */
    private SecretKey generateNewEncryptionKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    KEYSTORE_PROVIDER
            );

            // Define key generation parameters
            KeyGenParameterSpec keySpec = new KeyGenParameterSpec.Builder(
                    KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
            )
                    .setKeySize(KEY_SIZE)  // 256-bit key
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)  // GCM mode for authentication
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)  // No padding in GCM
                    .setRandomizedEncryptionRequired(true)  // Different IV for each encryption
                    .build();

            keyGenerator.init(keySpec);

            SecretKey key = keyGenerator.generateKey();

            if (key == null) {
                throw new RuntimeException("Failed to generate encryption key");
            }

            return key;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate new encryption key", e);
        }
    }

    /**
     * Delete the encryption key (use with caution)
     * This will make encrypted data unrecoverable
     */
    public void deleteEncryptionKey() {
        try {
            keyStore.deleteEntry(KEYSTORE_ALIAS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete encryption key", e);
        }
    }
}
