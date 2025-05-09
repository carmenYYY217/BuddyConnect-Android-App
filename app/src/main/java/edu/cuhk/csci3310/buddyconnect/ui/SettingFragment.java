package edu.cuhk.csci3310.buddyconnect.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import edu.cuhk.csci3310.buddyconnect.BaseActivity;
import edu.cuhk.csci3310.buddyconnect.BuddyConnectApplication;
import edu.cuhk.csci3310.buddyconnect.R;
import edu.cuhk.csci3310.buddyconnect.chatbot.ChatbotApiService;
import edu.cuhk.csci3310.buddyconnect.useraccount.AuthActivity;
import edu.cuhk.csci3310.buddyconnect.useraccount.FirebaseAuthManager;
import edu.cuhk.csci3310.buddyconnect.utils.ApiKeyManager;

public class SettingFragment extends Fragment {
    private FirebaseAuthManager authManager;
    private Button logoutButton;
    private SwitchCompat languageSwitch;
    private TextView languageText;
    private ApiKeyManager apiKeyManager;
    private TextInputEditText apiKeyInput;
    private Button saveApiKeyButton, clearApiKeyButton, fetchRemoteApiKeyButton;
    private Button uploadApiKeyButton, clearRemoteApiKeyButton;
    private TextView apiKeyStatusText;
    private Button removeFriendButton;

    public SettingFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setting, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authManager = new FirebaseAuthManager(requireContext());
        apiKeyManager = new ApiKeyManager(requireContext());

        // Set status bar space height
        View statusBarSpaceView = view.findViewById(R.id.statusBarSpaceView);
        BaseActivity.adjustStatusBarSpace(requireContext(), statusBarSpaceView);

        initializeUIComponents(view);
        setupListeners();
        updateApiKeyStatus();

        // Language switch setup
        languageSwitch = view.findViewById(R.id.languageSwitch);

        SharedPreferences prefs = requireActivity().getSharedPreferences("AppPrefs", requireActivity().MODE_PRIVATE);
        String savedLanguage = prefs.getString(BuddyConnectApplication.LANGUAGE_KEY, "en");
        boolean isChineseSelected = savedLanguage.equals("zh-TW");
        languageSwitch.setChecked(isChineseSelected);

        languageSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String languageCode = isChecked ? "zh-TW" : "en";

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(BuddyConnectApplication.LANGUAGE_KEY, languageCode);
            editor.apply();

            ((BaseActivity) requireActivity()).applyLanguageSettings();
            requireActivity().recreate();
        });

        // Logout button listener
        logoutButton.setOnClickListener(v -> {
            // Clear local API key cache
            apiKeyManager.onUserLogout();

            // Logout user
            authManager.logoutUser();
            Toast.makeText(requireContext(), getString(R.string.logged_out), Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(requireContext(), AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            requireActivity().finish();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isAdded() && !authManager.isUserSignedIn()) {
            Intent intent = new Intent(requireContext(), AuthActivity.class);
            startActivity(intent);
            requireActivity().finish();
        }
    }

    private void initializeUIComponents(View view) {
        languageText = view.findViewById(R.id.languageText);
        logoutButton = view.findViewById(R.id.logoutButton);
        removeFriendButton = view.findViewById(R.id.removeFriendButton);

        // API key components
        apiKeyInput = view.findViewById(R.id.apiKeyInput);
        saveApiKeyButton = view.findViewById(R.id.saveApiKeyButton);
        clearApiKeyButton = view.findViewById(R.id.clearApiKeyButton);
        fetchRemoteApiKeyButton = view.findViewById(R.id.fetchRemoteApiKeyButton);
        uploadApiKeyButton = view.findViewById(R.id.uploadApiKeyButton);
        clearRemoteApiKeyButton = view.findViewById(R.id.clearRemoteApiKeyButton);
        apiKeyStatusText = view.findViewById(R.id.apiKeyStatusText);

        apiKeyInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
    }

    private void setupListeners() {
        saveApiKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveApiKeyLocally();
            }
        });

        clearApiKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearLocalApiKey();
            }
        });

        fetchRemoteApiKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchRemoteApiKey();
            }
        });

        uploadApiKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadApiKey();
            }
        });

        clearRemoteApiKeyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearRemoteApiKey();
            }
        });

        removeFriendButton.setOnClickListener(v -> showFriendRemovalDialog());
    }

    private void saveApiKeyLocally() {
        String apiKey = apiKeyInput.getText().toString().trim();
        if (apiKey.isEmpty()) {
            Toast.makeText(requireContext(), R.string.api_key_cannot_be_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage(getString(R.string.validating_api_key));
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Validate API key
        ChatbotApiService tempService = new ChatbotApiService(requireContext(), apiKey);
        tempService.validateApiKey(new ChatbotApiService.ChatbotApiCallback() {
            @Override
            public void onSuccess(String response) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();

                        // API key is valid, save to local
                        boolean success = apiKeyManager.saveApiKeyToLocalOnly(apiKey);
                        if (success) {
                            Toast.makeText(requireContext(), R.string.api_key_saved_locally, Toast.LENGTH_SHORT).show();
                            apiKeyInput.setText("");
                            updateApiKeyStatus();
                        } else {
                            Toast.makeText(requireContext(), getString(R.string.error_saving_api_key, ""), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        progressDialog.dismiss();

                        // API key is invalid
                        Toast.makeText(requireContext(),
                                getString(R.string.invalid_api_key) + ": " + errorMessage,
                                Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void uploadApiKey() {
        // Check if local API key exists
        apiKeyManager.getChatbotApiKeyAsync(new ApiKeyManager.ApiKeyCallback() {
            @Override
            public void onSuccess(String apiKey) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        proceedWithUpload();
                    });
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        // No local API key, prompt user to save first
                        Toast.makeText(
                                requireContext(),
                                getString(R.string.error_uploading_api_key) + ": " +
                                        getString(R.string.save_key_first),
                                Toast.LENGTH_LONG
                        ).show();
                    });
                }
            }
        });
    }

    private void proceedWithUpload() {
        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage(getString(R.string.uploading_api_key));
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Auto-close dialog after 20 seconds to prevent UI freeze
        new Handler().postDelayed(() -> {
            if (progressDialog.isShowing() && isAdded()) {
                progressDialog.dismiss();
                Toast.makeText(
                        requireContext(),
                        getString(R.string.operation_taking_too_long),
                        Toast.LENGTH_LONG
                ).show();
            }
        }, 20000); // 20 seconds

        apiKeyManager.uploadApiKeyToRemote(new ApiKeyManager.ApiKeyCallback() {
            @Override
            public void onSuccess(String apiKey) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(requireContext(), R.string.api_key_uploaded, Toast.LENGTH_SHORT).show();
                        updateApiKeyStatus(); // Update status display
                    });
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        // Show user-friendly error message
                        String displayMessage;
                        if (errorMessage.contains("timeout") || errorMessage.contains("network")) {
                            displayMessage = getString(R.string.error_uploading_api_key) + ": "
                                    + getString(R.string.network_error);
                        } else if (errorMessage.contains("No API key")) {
                            displayMessage = getString(R.string.error_uploading_api_key) + ": "
                                    + "Please save an API key locally first.";
                        } else {
                            displayMessage = getString(R.string.error_uploading_api_key) + ": " + errorMessage;
                        }

                        Toast.makeText(requireContext(), displayMessage, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void clearLocalApiKey() {
        boolean success = apiKeyManager.clearLocalApiKeyOnly();
        if (success) {
            Toast.makeText(requireContext(), R.string.api_key_cleared_local, Toast.LENGTH_SHORT).show();
            apiKeyInput.setText("");
            updateApiKeyStatus();
        } else {
            Toast.makeText(requireContext(), R.string.error_clearing_api_key, Toast.LENGTH_SHORT).show();
        }
    }

    private void clearRemoteApiKey() {
        // Confirmation dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.clear_remote_api_key);
        builder.setMessage(R.string.confirm_clear_remote_api_key);
        builder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                performRemoteApiKeyDeletion();
            }
        });
        builder.setNegativeButton(android.R.string.no, null);
        builder.show();
    }

    private void performRemoteApiKeyDeletion() {
        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage(getString(R.string.deleting_remote_api_key));
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Auto-close dialog after 20 seconds to prevent UI freeze
        new Handler().postDelayed(() -> {
            if (progressDialog.isShowing() && isAdded()) {
                progressDialog.dismiss();
                Toast.makeText(
                        requireContext(),
                        getString(R.string.operation_taking_too_long),
                        Toast.LENGTH_LONG
                ).show();
            }
        }, 20000); // 20 seconds

        apiKeyManager.clearRemoteApiKey(new ApiKeyManager.ApiKeyCallback() {
            @Override
            public void onSuccess(String apiKey) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(requireContext(), R.string.api_key_cleared_remote, Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        // Show user-friendly error message
                        String displayMessage;
                        if (errorMessage.contains("timeout") || errorMessage.contains("network")) {
                            displayMessage = getString(R.string.error_clearing_remote_api_key) + ": "
                                    + getString(R.string.network_error);
                        } else {
                            displayMessage = getString(R.string.error_clearing_remote_api_key) + ": " + errorMessage;
                        }

                        Toast.makeText(requireContext(), displayMessage, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void updateApiKeyStatus() {
        apiKeyManager.getChatbotApiKeyAsync(new ApiKeyManager.ApiKeyCallback() {
            @Override
            public void onSuccess(String apiKey) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (apiKey != null && !apiKey.isEmpty()) {
                            apiKeyStatusText.setText(R.string.api_key_status_set);
                            apiKeyStatusText.setTextColor(getResources().getColor(R.color.green));
                            clearApiKeyButton.setEnabled(true);
                        } else {
                            apiKeyStatusText.setText(R.string.api_key_status_not_set);
                            apiKeyStatusText.setTextColor(getResources().getColor(R.color.red));
                            clearApiKeyButton.setEnabled(false);
                        }
                    });
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        apiKeyStatusText.setText(getString(R.string.error_retrieving_api_key));
                        apiKeyStatusText.setTextColor(getResources().getColor(R.color.red));
                        clearApiKeyButton.setEnabled(false);
                    });
                }
            }
        });
    }

    private void fetchRemoteApiKey() {
        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(requireContext());
        progressDialog.setMessage(getString(R.string.fetching_api_key));
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Auto-close dialog after 20 seconds to prevent UI freeze
        new Handler().postDelayed(() -> {
            if (progressDialog.isShowing() && isAdded()) {
                progressDialog.dismiss();
                Toast.makeText(
                        requireContext(),
                        getString(R.string.operation_taking_too_long),
                        Toast.LENGTH_LONG
                ).show();
            }
        }, 20000); // 20 seconds

        apiKeyManager.fetchRemoteChatbotApiKey(new ApiKeyManager.ApiKeyCallback() {
            @Override
            public void onSuccess(String apiKey) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }
                        Toast.makeText(requireContext(), R.string.api_key_fetched, Toast.LENGTH_SHORT).show();
                        updateApiKeyStatus();
                    });
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        // Show user-friendly error message
                        String displayMessage;
                        if (errorMessage.contains("timeout") || errorMessage.contains("network")) {
                            displayMessage = getString(R.string.error_fetching_remote_api_key) + ": "
                                    + getString(R.string.network_error);
                        } else if (errorMessage.contains("not found")) {
                            displayMessage = getString(R.string.error_fetching_remote_api_key) + ": "
                                    + getString(R.string.no_remote_key_found);
                        } else {
                            displayMessage = getString(R.string.error_fetching_remote_api_key) + ": " + errorMessage;
                        }

                        Toast.makeText(requireContext(), displayMessage, Toast.LENGTH_LONG).show();
                    });
                }
            }
        });
    }

    private void showFriendRemovalDialog() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        db.collection("users")
                .document(currentUserId)
                .collection("friends")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<String> friendEmails = new ArrayList<>();
                    List<String> displayItems = new ArrayList<>();

                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String status = doc.getString("status");
                        if ("accepted".equals(status)) {
                            String email = doc.getString("email");
                            String name = doc.getString("displayName");
                            friendEmails.add(email);
                            displayItems.add(name + " (" + email + ")");
                        }
                    }

                    if (friendEmails.isEmpty()) {
                        Toast.makeText(getContext(), "You have no friends to remove.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] items = displayItems.toArray(new String[0]);
                    final int[] selectedIndex = {-1};

                    new AlertDialog.Builder(requireContext())
                            .setTitle("Select a friend to remove")
                            .setSingleChoiceItems(items, -1, (dialog, which) -> selectedIndex[0] = which)
                            .setPositiveButton("Remove", (dialog, which) -> {
                                if (selectedIndex[0] >= 0) {
                                    String selectedEmail = friendEmails.get(selectedIndex[0]);

                                    // Remove from current user's list
                                    db.collection("users")
                                            .document(currentUserId)
                                            .collection("friends")
                                            .document(selectedEmail)
                                            .delete();

                                    // Remove from friend's list
                                    db.collection("users")
                                            .whereEqualTo("email", selectedEmail)
                                            .get()
                                            .addOnSuccessListener(querySnapshot -> {
                                                if (!querySnapshot.isEmpty()) {
                                                    String friendId = querySnapshot.getDocuments().get(0).getId();
                                                    db.collection("users")
                                                            .document(friendId)
                                                            .collection("friends")
                                                            .document(currentUserEmail)
                                                            .delete();
                                                }
                                            });

                                    Toast.makeText(getContext(), "Friend removed successfully", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getContext(), "Please select a friend to remove", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                });
    }
}