package edu.cuhk.csci3310.buddyconnect.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Secure API key storage utility
 */
public class ApiKeyManager {
    private static final String TAG = "ApiKeyManager";
    private static final String COLLECTION_NAME = "api_keys";
    private static final String CHATBOT_API_KEY = "chatbot_api_key";
    private static final String ENCRYPTION_KEY = "BuddyConnectSecure";
    
    // Shared preferences constants
    private static final String PREFS_NAME = "ApiKeyPrefs";
    private static final String CACHED_API_KEY = "cached_api_key";
    private static final String CACHE_USER_ID = "cache_user_id";
    
    private final Context context;
    private SecretKeySpec secretKey;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;
    
    public interface ApiKeyCallback {
        void onSuccess(String apiKey);
        void onFailure(String errorMessage);
    }
    
    public ApiKeyManager(Context context) {
        this.context = context;
        this.db = FirebaseFirestore.getInstance();
        this.mAuth = FirebaseAuth.getInstance();
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        try {
            byte[] key = ENCRYPTION_KEY.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            key = java.util.Arrays.copyOf(key, 16);
            secretKey = new SecretKeySpec(key, "AES");
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "Encryption error: " + e.getMessage());
        }
    }
    
    // Save API key to local cache
    private boolean saveApiKeyToLocal(String userId, String apiKey) {
        try {
            String encryptedKey = encrypt(apiKey);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(CACHED_API_KEY, encryptedKey);
            editor.putString(CACHE_USER_ID, userId);
            return editor.commit();
        } catch (Exception e) {
            Log.e(TAG, "Error saving API key to local: " + e.getMessage());
            return false;
        }
    }
    
    // Get API key from local cache
    private String getApiKeyFromLocal(String userId) {
        try {
            String cachedUserId = sharedPreferences.getString(CACHE_USER_ID, "");
            // Ensure API key can only be read by the associated user
            if (!cachedUserId.equals(userId)) {
                return null;
            }
            
            String encryptedKey = sharedPreferences.getString(CACHED_API_KEY, null);
            if (encryptedKey == null) {
                return null;
            }
            
            return decrypt(encryptedKey);
        } catch (Exception e) {
            Log.e(TAG, "Error getting API key from local: " + e.getMessage());
            return null;
        }
    }
    
    // Clear API key from local cache
    private boolean clearLocalApiKey() {
        try {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(CACHED_API_KEY);
            editor.remove(CACHE_USER_ID);
            return editor.commit();
        } catch (Exception e) {
            Log.e(TAG, "Error clearing local API key: " + e.getMessage());
            return false;
        }
    }

    // Save API key to both local and Firebase
    public boolean saveChatbotApiKey(String apiKey) {
        try {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Log.e(TAG, "Error saving API key: No user is signed in");
                return false;
            }
            
            String userId = currentUser.getUid();
            String encryptedKey = encrypt(apiKey);
            
            // Save to local cache
            saveApiKeyToLocal(userId, apiKey);
            
            // Save to Firebase
            Map<String, Object> data = new HashMap<>();
            data.put(CHATBOT_API_KEY, encryptedKey);
            
            DocumentReference docRef = db.collection(COLLECTION_NAME).document(userId);
            docRef.set(data)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "API key saved successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving API key: " + e.getMessage());
                });
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error saving API key: " + e.getMessage());
            return false;
        }
    }
    
    // Fetch API key from Firebase
    public void fetchRemoteChatbotApiKey(ApiKeyCallback callback) {
        try {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                callback.onFailure("No user is signed in");
                return;
            }
            
            String userId = currentUser.getUid();
            
            // Add timeout handling
            java.util.Timer timeoutTimer = new java.util.Timer();
            final boolean[] completed = {false};
            
            timeoutTimer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    if (!completed[0]) {
                        completed[0] = true;
                        Log.e(TAG, "Timeout when fetching remote API key");
                        callback.onFailure("Operation timed out. Please check your network connection.");
                    }
                    timeoutTimer.cancel();
                }
            }, 20000); // 20 second timeout
            
            // Get from Firebase
            DocumentReference docRef = db.collection(COLLECTION_NAME).document(userId);
            
            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (!completed[0]) {
                        completed[0] = true;
                        timeoutTimer.cancel();
                        
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document != null && document.exists()) {
                                try {
                                    String encryptedKey = document.getString(CHATBOT_API_KEY);
                                    if (encryptedKey != null) {
                                        String decryptedKey = decrypt(encryptedKey);
                                        
                                        // Save to local cache
                                        saveApiKeyToLocal(userId, decryptedKey);
                                        
                                        callback.onSuccess(decryptedKey);
                                    } else {
                                        callback.onFailure("API key not found on server");
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Error decrypting API key: " + e.getMessage());
                                    callback.onFailure("Error decrypting API key: " + e.getMessage());
                                }
                            } else {
                                Log.d(TAG, "API key document not found for user: " + userId);
                                callback.onFailure("API key document not found on server");
                            }
                        } else {
                            Exception exception = task.getException();
                            Log.e(TAG, "Error fetching API key: " + 
                                 (exception != null ? exception.getMessage() : "unknown error"));
                            callback.onFailure("Error fetching API key from server: " + 
                                              (exception != null ? exception.getMessage() : "unknown error"));
                        }
                    }
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error getting API key: " + e.getMessage());
            callback.onFailure("Error getting API key: " + e.getMessage());
        }
    }
    
    // Save API key to local only
    public boolean saveApiKeyToLocalOnly(String apiKey) {
        try {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Log.e(TAG, "Error saving API key: No user is signed in");
                return false;
            }
            
            String userId = currentUser.getUid();
            
            // Save only to local cache
            boolean success = saveApiKeyToLocal(userId, apiKey);
            if (success) {
                Log.d(TAG, "API key saved to local cache only");
            }
            
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error saving API key to local: " + e.getMessage());
            return false;
        }
    }
    
    // Upload API key from local to Firebase
    public void uploadApiKeyToRemote(ApiKeyCallback callback) {
        try {
            // Check if user is logged in
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                callback.onFailure("No user is signed in");
                return;
            }
            
            String userId = currentUser.getUid();
            Log.d(TAG, "Attempting to upload API key for user: " + userId);
            
            // Get API key from local
            String localApiKey = getApiKeyFromLocal(userId);
            if (localApiKey == null || localApiKey.isEmpty()) {
                Log.e(TAG, "No API key found in local storage for user: " + userId);
                callback.onFailure("No API key found in local storage. Please save an API key locally first.");
                return;
            }
            
            try {
                // Encrypt API key
                String encryptedKey = encrypt(localApiKey);
                
                // Add timeout handling
                java.util.Timer timeoutTimer = new java.util.Timer();
                final boolean[] completed = {false};
                
                timeoutTimer.schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        if (!completed[0]) {
                            completed[0] = true;
                            Log.e(TAG, "Timeout when uploading API key");
                            callback.onFailure("Operation timed out after 20 seconds. Please check your network connection and try again.");
                        }
                        timeoutTimer.cancel();
                    }
                }, 20000); // 20 second timeout
                
                // Save to Firebase
                Map<String, Object> data = new HashMap<>();
                data.put(CHATBOT_API_KEY, encryptedKey);
                
                // Check Firestore instance
                if (db == null) {
                    db = FirebaseFirestore.getInstance();
                    Log.d(TAG, "Firestore instance was null, recreated");
                }
                
                Log.d(TAG, "Starting Firebase upload for user: " + userId);
                DocumentReference docRef = db.collection(COLLECTION_NAME).document(userId);
                docRef.set(data)
                    .addOnSuccessListener(aVoid -> {
                        if (!completed[0]) {
                            completed[0] = true;
                            timeoutTimer.cancel();
                            Log.d(TAG, "API key uploaded to Firebase successfully for user: " + userId);
                            callback.onSuccess(localApiKey);
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!completed[0]) {
                            completed[0] = true;
                            timeoutTimer.cancel();
                            String errorMsg = e.getMessage();
                            Log.e(TAG, "Error uploading API key: " + errorMsg);
                            callback.onFailure("Error uploading API key: " + errorMsg);
                        }
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error encrypting API key: " + e.getMessage());
                callback.onFailure("Error encrypting API key: " + e.getMessage());
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error in uploadApiKeyToRemote: " + e.getMessage());
            callback.onFailure("Error uploading API key: " + e.getMessage());
        }
    }
    
    // Clear local API key only
    public boolean clearLocalApiKeyOnly() {
        try {
            boolean success = clearLocalApiKey();
            if (success) {
                Log.d(TAG, "Local API key cache cleared successfully");
            }
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Error clearing local API key: " + e.getMessage());
            return false;
        }
    }
    
    // Delete API key from Firebase
    public void clearRemoteApiKey(ApiKeyCallback callback) {
        try {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                callback.onFailure("No user is signed in");
                return;
            }
            
            String userId = currentUser.getUid();
            DocumentReference docRef = db.collection(COLLECTION_NAME).document(userId);
            
            // Add timeout handling
            java.util.Timer timeoutTimer = new java.util.Timer();
            final boolean[] completed = {false};
            
            timeoutTimer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    if (!completed[0]) {
                        completed[0] = true;
                        Log.e(TAG, "Timeout when deleting remote API key");
                        callback.onFailure("Operation timed out. Please check your network connection.");
                    }
                    timeoutTimer.cancel();
                }
            }, 20000); // 20 second timeout
            
            docRef.delete()
                .addOnSuccessListener(aVoid -> {
                    if (!completed[0]) {
                        completed[0] = true;
                        timeoutTimer.cancel();
                        Log.d(TAG, "Remote API key deleted successfully");
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!completed[0]) {
                        completed[0] = true;
                        timeoutTimer.cancel();
                        Log.e(TAG, "Error deleting remote API key: " + e.getMessage());
                        callback.onFailure("Error deleting remote API key: " + e.getMessage());
                    }
                });
        } catch (Exception e) {
            Log.e(TAG, "Error deleting remote API key: " + e.getMessage());
            callback.onFailure("Error deleting remote API key: " + e.getMessage());
        }
    }

    // Get API key from local cache only
    public void getChatbotApiKeyAsync(ApiKeyCallback callback) {
        try {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                callback.onFailure("No user is signed in");
                return;
            }
            
            String userId = currentUser.getUid();
            
            // Check local cache only
            String cachedKey = getApiKeyFromLocal(userId);
            if (cachedKey != null) {
                Log.d(TAG, "API key retrieved from local cache");
                callback.onSuccess(cachedKey);
                return;
            }
            
            // If no local cache, return error
            callback.onFailure("No API key found in local cache");
        } catch (Exception e) {
            Log.e(TAG, "Error getting API key: " + e.getMessage());
            callback.onFailure("Error getting API key: " + e.getMessage());
        }
    }
    
    // Sync method to get API key from local
    public String getChatbotApiKeySyncLocal() {
        try {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Log.e(TAG, "Error getting API key: No user is signed in");
                return null;
            }
            
            String userId = currentUser.getUid();
            return getApiKeyFromLocal(userId);
        } catch (Exception e) {
            Log.e(TAG, "Error getting local API key: " + e.getMessage());
            return null;
        }
    }
    
    // Remove API key from Firestore and local cache
    public boolean clearChatbotApiKey() {
        try {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser == null) {
                Log.e(TAG, "Error clearing API key: No user is signed in");
                return false;
            }
            
            // Clear local cache
            clearLocalApiKey();
            
            String userId = currentUser.getUid();
            DocumentReference docRef = db.collection(COLLECTION_NAME).document(userId);
            
            docRef.delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "API key cleared successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error clearing API key: " + e.getMessage());
                });
            
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error clearing API key: " + e.getMessage());
            return false;
        }
    }
    
    // Called when user logs out to clear local API key cache
    public void onUserLogout() {
        clearLocalApiKey();
    }
    
    private String encrypt(String strToEncrypt) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return Base64.encodeToString(cipher.doFinal(strToEncrypt.getBytes(StandardCharsets.UTF_8)), Base64.DEFAULT);
    }
    
    private String decrypt(String strToDecrypt) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return new String(cipher.doFinal(Base64.decode(strToDecrypt, Base64.DEFAULT)), StandardCharsets.UTF_8);
    }
} 