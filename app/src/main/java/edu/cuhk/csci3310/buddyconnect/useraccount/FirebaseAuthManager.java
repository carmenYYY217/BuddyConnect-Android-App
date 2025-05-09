package edu.cuhk.csci3310.buddyconnect.useraccount;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class FirebaseAuthManager {
    private static final String TAG = "FirebaseAuthManager";
    private static final String PREFS_NAME = "AppPrefs";
    private static final String LOGIN_STATUS_KEY = "loginStatus";
    private static final String USER_EMAIL_KEY = "userEmail";
    private static final String USER_DISPLAY_NAME_KEY = "userDisplayName";

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Context context;

    public interface AuthCallback {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public FirebaseAuthManager(Context context) {
        this.context = context;
        
        try {
            this.mAuth = FirebaseAuth.getInstance();
            this.db = FirebaseFirestore.getInstance();
            Log.d(TAG, "Firebase authentication service initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization error", e);
        }
    }

    public boolean isUserSignedIn() {
        try {
            return mAuth != null && mAuth.getCurrentUser() != null;
        } catch (Exception e) {
            Log.e(TAG, "Error checking user login status", e);
            return false;
        }
    }

    public String getCurrentUserEmail() {
        try {
            FirebaseUser user = mAuth != null ? mAuth.getCurrentUser() : null;
            return user != null ? user.getEmail() : "";
        } catch (Exception e) {
            Log.e(TAG, "Error getting user email", e);
            return "";
        }
    }

    public void registerUser(String email, String password, String displayName, AuthCallback callback) {
        if (mAuth == null) {
            callback.onFailure("Firebase authentication service not initialized");
            return;
        }
        
        try {
            mAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Registration successful");
                            
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(displayName)
                                        .build();
                                
                                user.updateProfile(profileUpdates)
                                        .addOnCompleteListener(profileTask -> {
                                            if (profileTask.isSuccessful()) {
                                                Log.d(TAG, "Display name updated");
                                                
                                                // Save user data to Firestore
                                                Map<String, Object> userMap = new HashMap<>();
                                                userMap.put("email", email);
                                                userMap.put("displayName", displayName);
                                                userMap.put("photoUrl", null); // Explicitly set photoUrl to null
                                                userMap.put("activityStatus", "active"); // Default status set to "active"
                                                
                                                try {
                                                    saveUserLoginStatus(true, email, displayName);
                                                    callback.onSuccess();
                                                    
                                                    db.collection("users").document(user.getUid())
                                                            .set(userMap)
                                                            .addOnSuccessListener(aVoid -> {
                                                                Log.d(TAG, "User data successfully saved to Firestore");
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                Log.w(TAG, "Error: Firestore write failed", e);
                                                            });
                                                } catch (Exception e) {
                                                    Log.e(TAG, "Firestore operation exception", e);
                                                }
                                            } else {
                                                Log.w(TAG, "Error: failed to update display name", profileTask.getException());
                                                saveUserLoginStatus(true, email, displayName);
                                                callback.onSuccess();
                                            }
                                        });
                            } else {
                                callback.onFailure("Registration successful but unable to get user data");
                            }
                        } else {
                            Log.w(TAG, "Error: registration failed", task.getException());
                            String errorMessage = "Registration failed";
                            if (task.getException() != null) {
                                errorMessage += ": " + task.getException().getMessage();
                            }
                            callback.onFailure(errorMessage);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error during registration", e);
            callback.onFailure("Error during registration: " + e.getMessage());
        }
    }

    public void loginUser(String email, String password, AuthCallback callback) {
        if (mAuth == null) {
            callback.onFailure("Firebase authentication service not initialized");
            return;
        }
        
        try {
            mAuth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Login successful");
                            FirebaseUser user = mAuth.getCurrentUser();
                            String displayName = user != null ? user.getDisplayName() : "";
                            saveUserLoginStatus(true, email, displayName);
                            callback.onSuccess();
                        } else {
                            Log.w(TAG, "Error: login failed", task.getException());
                            String errorMessage = "Login failed";
                            if (task.getException() != null) {
                                errorMessage += ": " + task.getException().getMessage();
                            }
                            callback.onFailure(errorMessage);
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "Error during login", e);
            callback.onFailure("Error during login: " + e.getMessage());
        }
    }

    public void logoutUser() {
        try {
            if (mAuth != null) {
                mAuth.signOut();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during logout", e);
        }
        saveUserLoginStatus(false, "", "");
    }

    private void saveUserLoginStatus(boolean isLoggedIn, String email, String displayName) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(LOGIN_STATUS_KEY, isLoggedIn);
            editor.putString(USER_EMAIL_KEY, email);
            editor.putString(USER_DISPLAY_NAME_KEY, displayName);
            editor.apply();
        } catch (Exception e) {
            Log.e(TAG, "Error saving login status", e);
        }
    }
} 