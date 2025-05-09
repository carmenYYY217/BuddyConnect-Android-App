package edu.cuhk.csci3310.buddyconnect.useraccount;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Locale;

import edu.cuhk.csci3310.buddyconnect.BuddyConnectApplication;
import edu.cuhk.csci3310.buddyconnect.MainActivity;
import edu.cuhk.csci3310.buddyconnect.R;

public class LoginFragment extends Fragment {
    private EditText emailInput;
    private EditText passwordInput;
    private FirebaseAuthManager authManager;
    private Button loginButton;
    private ProgressDialog progressDialog;
    private SwitchCompat languageSwitch;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        // Local saved language from SP
        SharedPreferences prefs = requireActivity().getSharedPreferences(BuddyConnectApplication.PREFS_NAME, Context.MODE_PRIVATE);
        String currentLanguage = prefs.getString(BuddyConnectApplication.LANGUAGE_KEY, "en");
        
        View view = inflater.inflate(R.layout.fragment_login, container, false);

        authManager = new FirebaseAuthManager(requireContext());
        
        emailInput = view.findViewById(R.id.emailInput);
        passwordInput = view.findViewById(R.id.passwordInput);
        loginButton = view.findViewById(R.id.loginButton);
        Button registerButton = view.findViewById(R.id.registerButton);
        languageSwitch = view.findViewById(R.id.languageSwitch);

        // Set the language switch state based on current language
        languageSwitch.setChecked(currentLanguage.equals("zh-TW"));
        
        // Setup language switch listener
        languageSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // Save language preference
                SharedPreferences.Editor editor = prefs.edit();
                if (isChecked) {
                    // Switch to Traditional Chinese
                    editor.putString(BuddyConnectApplication.LANGUAGE_KEY, "zh-TW");
                } else {
                    // Switch to English
                    editor.putString(BuddyConnectApplication.LANGUAGE_KEY, "en");
                }
                editor.apply();
                
                // Refresh activity to apply language change
                Intent refresh = new Intent(getActivity(), AuthActivity.class);
                getActivity().finish();
                startActivity(refresh);
            }
        });

        // Initialize progress dialog
        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage(getString(R.string.logging_in));
        progressDialog.setCancelable(false);

        loginButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString();
            String password = passwordInput.getText().toString();
            
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Show progress dialog and disable login button
            progressDialog.show();
            loginButton.setEnabled(false);
            
            // Firebase login
            authManager.loginUser(email, password, new FirebaseAuthManager.AuthCallback() {
                @Override
                public void onSuccess() {
                    // Hide progress dialog
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    Toast.makeText(getContext(), getString(R.string.login_successful), Toast.LENGTH_SHORT).show();
                    
                    // Go to MainActivity and clear the back stack
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    
                    // Finish auth activity
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                }

                @Override
                public void onFailure(String errorMessage) {
                    // Hide progress dialog and re-enable button
                    if (progressDialog.isShowing()) {
                        progressDialog.dismiss();
                    }
                    loginButton.setEnabled(true);
                    Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                }
            });
        });

        registerButton.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.auth_fragment_container, new RegisterFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        return view;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // Ensure dialog is closed
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
} 