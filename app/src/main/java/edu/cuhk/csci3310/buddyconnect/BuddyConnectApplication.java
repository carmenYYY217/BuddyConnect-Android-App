package edu.cuhk.csci3310.buddyconnect;

/*
 * For application initialization (e.g. Firebase, SharedPreferences, etc.. )
 * -> 處理全局設置和初始化
 */

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import com.google.firebase.FirebaseApp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import edu.cuhk.csci3310.buddyconnect.chatbot.ChatMessage;
import edu.cuhk.csci3310.buddyconnect.utils.ApiKeyManager;

public class BuddyConnectApplication extends Application implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "BuddyConnectApp";
    public static final String PREFS_NAME = "AppPrefs";
    public static final String LANGUAGE_KEY = "language";
    
    // ChatBOt history save
    private static List<ChatMessage> chatHistory = new ArrayList<>();
    private static boolean chatInitialized = false;
    
    private int activityReferences = 0;
    private boolean isActivityChangingConfigurations = false;
    
    private static boolean isAppInitialized = false;
    
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(updateBaseContextLocale(base));
    }
    
    private Context updateBaseContextLocale(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String language = prefs.getString(LANGUAGE_KEY, "en"); // Default to English
        
        Locale locale;
        if (language.equals("zh-TW")) {
            locale = Locale.TRADITIONAL_CHINESE;
        } else {
            locale = new Locale(language);
        }
        
        Locale.setDefault(locale);
        
        Configuration config = context.getResources().getConfiguration();
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Register activity lifecycle callbacks
        registerActivityLifecycleCallbacks(this);
        
        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this);
            Log.d(TAG, "Firebase initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Firebase initialization failed", e);
        }
        
        isAppInitialized = true;
        Log.d(TAG, "Application initialized successfully");
    }
    
    @Override
    public void onTerminate() {
        Log.d(TAG, "Application terminating");
        // Cancel registration
        unregisterActivityLifecycleCallbacks(this);
        // Ensure application terminates
        clearChatHistory();
        super.onTerminate();
    }
    
    // 聊天記錄管理
    public static List<ChatMessage> getChatHistory() {
        return chatHistory;
    }
    
    public static void addChatMessage(ChatMessage message) {
        if (message != null) {
            chatHistory.add(message);
            Log.d(TAG, "Message added to chat history, total: " + chatHistory.size());
        }
    }
    
    public static void clearChatHistory() {
        int size = chatHistory.size();
        chatHistory.clear();
        chatInitialized = false;
        Log.d(TAG, "Chat history cleared, removed " + size + " messages");
    }
    
    public static boolean hasChatHistory() {
        return !chatHistory.isEmpty();
    }
    
    public static void setChatInitialized(boolean initialized) {
        chatInitialized = initialized;
        Log.d(TAG, "Chat initialized set to: " + initialized);
    }
    
    public static boolean isChatInitialized() {
        return chatInitialized;
    }
    
    public static boolean isInitialized() {
        return isAppInitialized;
    }
    
    // 應用進入Foreground時會...
    private void onAppForeground() {
        Log.d(TAG, "App entered foreground");
        
        if (!chatInitialized && !chatHistory.isEmpty()) {
            Log.d(TAG, "Restoring chat state on app foreground");
            chatInitialized = true;
        }
    }
    
    // 應用進入background時會...
    private void onAppBackground() {
        Log.d(TAG, "App entered background");
        // 
        
    }
    
    // 監聽Activity生命週期
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        Log.d(TAG, "Activity created: " + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (++activityReferences == 1 && !isActivityChangingConfigurations) {
            // Application entered foreground
            onAppForeground();
        }
        Log.d(TAG, "Activity started: " + activity.getClass().getSimpleName() + 
              ", activityReferences: " + activityReferences);
    }

    @Override
    public void onActivityResumed(Activity activity) {
        Log.d(TAG, "Activity resumed: " + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityPaused(Activity activity) {
        Log.d(TAG, "Activity paused: " + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityStopped(Activity activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations();
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            // Application entered background
            onAppBackground();
        }
        Log.d(TAG, "Activity stopped: " + activity.getClass().getSimpleName() + 
              ", activityReferences: " + activityReferences + 
              ", isChangingConfigurations: " + isActivityChangingConfigurations);
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        Log.d(TAG, "Activity saved instance state: " + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Log.d(TAG, "Activity destroyed: " + activity.getClass().getSimpleName());
    }
} 