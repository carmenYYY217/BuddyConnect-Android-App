package edu.cuhk.csci3310.buddyconnect.useraccount;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
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

public class RegisterFragment extends Fragment {
    private EditText emailInput;
    private EditText passwordInput;
    private EditText confirmPasswordInput;
    private EditText displayNameInput;
    private FirebaseAuthManager authManager;
    private Button registerButton;
    private ProgressDialog progressDialog;
    private SwitchCompat languageSwitch;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Apply saved language
        SharedPreferences prefs = requireActivity().getSharedPreferences(BuddyConnectApplication.PREFS_NAME, Context.MODE_PRIVATE);
        String currentLanguage = prefs.getString(BuddyConnectApplication.LANGUAGE_KEY, "en");
        
        View view = inflater.inflate(R.layout.fragment_register, container, false);

        authManager = new FirebaseAuthManager(requireContext());
        
        emailInput = view.findViewById(R.id.emailInput);
        passwordInput = view.findViewById(R.id.passwordInput);
        confirmPasswordInput = view.findViewById(R.id.confirmPasswordInput);
        displayNameInput = view.findViewById(R.id.displayNameInput);
        registerButton = view.findViewById(R.id.registerButton);
        Button backButton = view.findViewById(R.id.backButton);
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
        progressDialog.setMessage(getString(R.string.registering));
        progressDialog.setCancelable(false);

        // Add timeout handling
        View rootView = view;
        registerButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString();
            String password = passwordInput.getText().toString();
            String confirmPassword = confirmPasswordInput.getText().toString();
            String displayName = displayNameInput.getText().toString();

            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || displayName.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(getContext(), getString(R.string.password_not_match), Toast.LENGTH_SHORT).show();
                return;
            }

            if (password.length() < 6) {
                Toast.makeText(getContext(), getString(R.string.password_too_short), Toast.LENGTH_SHORT).show();
                return;
            }

            // Show progress dialog and disable register button
            progressDialog.show();
            registerButton.setEnabled(false);

            // Add timeout handling
            rootView.postDelayed(() -> {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    registerButton.setEnabled(true);
                    Toast.makeText(getContext(), getString(R.string.registration_timeout), Toast.LENGTH_LONG).show();
                }
            }, 30000); // 30 seconds timeout

            // Firebase Registration Logic
            authManager.registerUser(email, password, displayName, new FirebaseAuthManager.AuthCallback() {
                @Override
                public void onSuccess() {
                    // Signup and then login immediately
                    authManager.loginUser(email, password, new FirebaseAuthManager.AuthCallback() {
                        @Override
                        public void onSuccess() {
                            try {
                                if (progressDialog != null && progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                                Toast.makeText(getContext(), getString(R.string.registration_successful), Toast.LENGTH_SHORT).show();
                                if (getActivity() != null) {
                                    Intent intent = new Intent(getActivity(), MainActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                }
                            } catch (Exception e) {
                                Log.e("RegisterFragment", "Error successful registration: " + e.getMessage());
                            }
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            try {
                                if (progressDialog != null && progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                                registerButton.setEnabled(true);
                                Toast.makeText(getContext(), getString(R.string.registration_login_failed, errorMessage), Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                Log.e("RegisterFragment", "Error login failure: " + e.getMessage());
                            }
                        }
                    });
                }

                @Override
                public void onFailure(String errorMessage) {
                    try {
                        if (progressDialog != null && progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        registerButton.setEnabled(true);
                        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Log.e("RegisterFragment", "Error registration failure: " + e.getMessage());
                    }
                }
            });
        });

        backButton.setOnClickListener(v -> {
            FragmentTransaction transaction = getParentFragmentManager().beginTransaction();
            transaction.replace(R.id.auth_fragment_container, new LoginFragment());
            transaction.addToBackStack(null);
            transaction.commit();
        });

        return view;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
} 