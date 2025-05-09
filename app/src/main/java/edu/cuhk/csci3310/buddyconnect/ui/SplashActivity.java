package edu.cuhk.csci3310.buddyconnect.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import edu.cuhk.csci3310.buddyconnect.BaseActivity;
import edu.cuhk.csci3310.buddyconnect.MainActivity;
import edu.cuhk.csci3310.buddyconnect.R;
import edu.cuhk.csci3310.buddyconnect.useraccount.FirebaseAuthManager;
import edu.cuhk.csci3310.buddyconnect.useraccount.AuthActivity;

/**
 * Checks user login status and
 * 參考 https://developer.android.com/develop/ui/views/launch/splash-screen?hl=zh-tw, 可以再新增動畫等優化
 */

public class SplashActivity extends BaseActivity {
    
    private static final int SPLASH_TIMEOUT = 1000;
    private FirebaseAuthManager authManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Hide the action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);
        
        // Initialize Firebase Auth Manager
        authManager = new FirebaseAuthManager(this);
        
        // Handle system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.splash_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        // Check login status
        new Handler(Looper.getMainLooper()).postDelayed(this::checkLoginStatus, SPLASH_TIMEOUT);
    }
    
    private void checkLoginStatus() {
        if (authManager.isUserSignedIn()) {
            // 登入左就去主頁
            startActivity(new Intent(this, MainActivity.class));
        } else {
            // 未登入去登入頁面(AuthActivity
            startActivity(new Intent(this, AuthActivity.class));
        }
        finish();
    }
} 