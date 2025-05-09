package edu.cuhk.csci3310.buddyconnect.useraccount;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import edu.cuhk.csci3310.buddyconnect.BaseActivity;
import edu.cuhk.csci3310.buddyconnect.MainActivity;
import edu.cuhk.csci3310.buddyconnect.R;

/*
 * 管理用戶身份驗證流程 shown when the user is not logged in
 */

public class AuthActivity extends BaseActivity {
    
    private FirebaseAuthManager authManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force to use light theme in login activity
        getDelegate().setLocalNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_auth);
        
        authManager = new FirebaseAuthManager(this);
        
        // Check already login
        if (authManager.isUserSignedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        
        // Set title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.auth_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.auth_fragment_container, new LoginFragment());
            transaction.commit();
        }
    }
    
    // Prevent accidental app exit
    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.exit_app)
                    .setMessage(R.string.exit_confirmation)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> finishAffinity())
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        }
    }
} 