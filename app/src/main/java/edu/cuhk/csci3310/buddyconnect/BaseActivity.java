package edu.cuhk.csci3310.buddyconnect;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.Locale;

/**
 * Handles common functionality for all activities:
 * 1. Language settings and localization
 * 2. Theme application
 * 3. Status bar adjustment
 * All activities should extend this class!!! 
 */

public class BaseActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "AppPrefs";
    public static final String THEME_KEY = "theme";
    
    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences(BuddyConnectApplication.PREFS_NAME, MODE_PRIVATE);
        String language = prefs.getString(BuddyConnectApplication.LANGUAGE_KEY, "en");
        
        Locale locale;
        if (language.equals("zh-TW")) {
            locale = Locale.TRADITIONAL_CHINESE;
        } else {
            locale = new Locale(language);
        }
        
        Locale.setDefault(locale);
        
        Configuration config = newBase.getResources().getConfiguration();
        config.setLocale(locale);
        Context context = newBase.createConfigurationContext(config);
        
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applySavedTheme();
        super.onCreate(savedInstanceState);
        applyLanguageSettings();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyLanguageSettings();
    }

    // Apply theme settings from SharedPreferences
    protected void applySavedTheme() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int themeMode = prefs.getInt(THEME_KEY, 2);
        switch (themeMode) {
            case 0:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    // Apply language settings from SharedPreferences
    public void applyLanguageSettings() {
        SharedPreferences prefs = getSharedPreferences(BuddyConnectApplication.PREFS_NAME, MODE_PRIVATE);
        String currentLanguage = prefs.getString(BuddyConnectApplication.LANGUAGE_KEY, "en");
        
        Locale locale;
        if (currentLanguage.equals("zh-TW")) {
            locale = Locale.TRADITIONAL_CHINESE;
        } else {
            locale = new Locale(currentLanguage);
        }
        
        Locale.setDefault(locale);
        
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }
    

    public static void adjustStatusBarSpace(Context context, View statusBarSpaceView) {
        if (statusBarSpaceView != null) {
            int statusBarHeight = 0;
            int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                statusBarHeight = context.getResources().getDimensionPixelSize(resourceId);
            }
            
            // Add space for content to be lower
            int extraPadding = (int) (12 * Resources.getSystem().getDisplayMetrics().density);
            
            ViewGroup.LayoutParams params = statusBarSpaceView.getLayoutParams();
            params.height = statusBarHeight + extraPadding;
            statusBarSpaceView.setLayoutParams(params);
        }
    }
} 