package edu.cuhk.csci3310.buddyconnect.chatbot;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import edu.cuhk.csci3310.buddyconnect.BuddyConnectApplication;
import edu.cuhk.csci3310.buddyconnect.MainActivity;
import edu.cuhk.csci3310.buddyconnect.R;
import edu.cuhk.csci3310.buddyconnect.ui.SettingFragment;
import edu.cuhk.csci3310.buddyconnect.utils.ApiKeyManager;

public class ChatBotFragment extends Fragment {
    private static final String TAG = "ChatBotFragment";

    private RecyclerView chatRecyclerView;
    private EditText messageEditText;
    private ImageButton sendButton;
    private ProgressBar progressBar;
    private View statusBarSpaceView;
    private ChatAdapter chatAdapter;
    private ChatbotApiService chatbotApiService;
    private ApiKeyManager localApiKeyManager;
    private boolean isInitialized = false;

    private AlertDialog settingsDialog = null;

    public ChatBotFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chatbot, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView);
        messageEditText = view.findViewById(R.id.messageEditText);
        sendButton = view.findViewById(R.id.sendButton);
        progressBar = view.findViewById(R.id.progressBar);
        statusBarSpaceView = view.findViewById(R.id.statusBarSpaceView);

        // 設置狀態欄空間的高度
        adjustStatusBarSpace();
        
        // 初始化聊天適配器
        chatAdapter = new ChatAdapter();
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        chatRecyclerView.setAdapter(chatAdapter);
        
        // 從應用程序恢復聊天記錄
        if (BuddyConnectApplication.hasChatHistory()) {
            chatAdapter.setMessages(BuddyConnectApplication.getChatHistory());
            chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
        }

        localApiKeyManager = new ApiKeyManager(requireContext());
        
        // 先嘗試使用同步本地方法獲取API密鑰
        String localApiKey = localApiKeyManager.getChatbotApiKeySyncLocal();
        if (localApiKey != null && !localApiKey.isEmpty()) {
            initChatbotApiService(localApiKey);
            isInitialized = true;
            BuddyConnectApplication.setChatInitialized(true);
        } else {
            // 僅在沒有聊天記錄或未初始化時才進行初始化
            if (!BuddyConnectApplication.hasChatHistory() || !BuddyConnectApplication.isChatInitialized()) {
                // 使用延遲以確保UI已經完全加載
                new Handler().postDelayed(() -> {
                    if (isAdded()) {
                        checkApiKeyAndInitialize();
                    }
                }, 300);
            } else {
                isInitialized = true;
            }
        }

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (settingsDialog != null && settingsDialog.isShowing()) {
                            settingsDialog.dismiss(); // Treat back press as cancel
                        } else {
                            // Allow normal back press behavior
                            setEnabled(false);
                            requireActivity().onBackPressed();
                        }
                    }
                });
    }
    
    private void adjustStatusBarSpace() {
        if (statusBarSpaceView != null) {
            int statusBarHeight = 0;
            int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                statusBarHeight = getResources().getDimensionPixelSize(resourceId);
            }
            
            // 額外空間使內容下移
            int extraPadding = (int) (12 * Resources.getSystem().getDisplayMetrics().density);
            
            ViewGroup.LayoutParams params = statusBarSpaceView.getLayoutParams();
            params.height = statusBarHeight + extraPadding;
            statusBarSpaceView.setLayoutParams(params);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isInitialized) {
            new Handler().postDelayed(() -> {
                if (isAdded() && !isInitialized) {
                    checkApiKeyAndInitialize();
                }
            }, 500);
        }
    }

    private void checkApiKeyAndInitialize() {
        progressBar.setVisibility(View.VISIBLE);
        
        localApiKeyManager.getChatbotApiKeyAsync(new ApiKeyManager.ApiKeyCallback() {
            @Override
            public void onSuccess(String apiKey) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        // 確保Fragment仍然附加到Activity
                        if (!isAdded()) return;
                        
                        progressBar.setVisibility(View.GONE);
                        
                        if (apiKey != null && !apiKey.isEmpty()) {
                            if (chatbotApiService == null || !isInitialized) {
                                initChatbotApiService(apiKey);
                                isInitialized = true;
                                BuddyConnectApplication.setChatInitialized(true);
                            }
                        } else {
                            if (isAdded()) {
                                redirectToSettings();
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        // 確保Fragment仍然附加到Activity
                        if (!isAdded()) return;
                        
                        progressBar.setVisibility(View.GONE);
                        Log.d(TAG, "API key not found in local storage: " + errorMessage);
                        
                        if (isAdded()) {
                            redirectToSettings();
                        }
                    });
                }
            }
        });
    }

    private void redirectToSettings() {
        try {
            // Check if fragment is attached
            if (!isAdded()) return;
            if (settingsDialog != null && settingsDialog.isShowing()) {
                return;
            }

            Context context = getContext();
            if (context == null) return;

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.chatbot_unavailable);
            builder.setMessage(R.string.setup_key_in_settings);
            builder.setPositiveButton(R.string.go_to_settings, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (!isAdded()) return;

                    try {
                        // Cast the activity to MainActivity and load the SettingFragment
                        MainActivity mainActivity = (MainActivity) requireActivity();
                        mainActivity.selectTabWithAction(5);
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "Fragment not attached to activity: " + e.getMessage());
                    }
                }
            });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.setCancelable(true);

            // Save dialog reference
            settingsDialog = builder.create();

            // Set dialog dismiss listener to clear reference
            settingsDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    settingsDialog = null;
                }
            });

            settingsDialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Error showing settings dialog: " + e.getMessage());
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // 在onPause時關閉對話框
        if (settingsDialog != null && settingsDialog.isShowing()) {
            settingsDialog.dismiss();
            settingsDialog = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clear dialog reference
        settingsDialog = null;
    }

    private void initChatbotApiService(String apiKey) {
        chatbotApiService = new ChatbotApiService(requireContext(), apiKey);
        
        // Only add welcome message if there is no chat history
        if (!BuddyConnectApplication.hasChatHistory()) {
            ChatMessage welcomeMessage = new ChatMessage(getString(R.string.chatbot_welcome), ChatMessage.TYPE_BOT);
            chatAdapter.addMessage(welcomeMessage);
            BuddyConnectApplication.addChatMessage(welcomeMessage);
        }
    }

    private void sendMessage() {
        if (chatbotApiService == null) {
            Toast.makeText(requireContext(), R.string.chatbot_not_initialized, Toast.LENGTH_SHORT).show();
            checkApiKeyAndInitialize();
            return;
        }

        String message = messageEditText.getText().toString().trim();
        
        if (TextUtils.isEmpty(message)) {
            Toast.makeText(requireContext(), getString(R.string.enter_message), Toast.LENGTH_SHORT).show();
            return;
        }

        sendButton.setEnabled(false);
        messageEditText.setText("");
        
        // Create user message and add to UI and global storage
        ChatMessage userMessage = new ChatMessage(message, ChatMessage.TYPE_USER);
        chatAdapter.addMessage(userMessage);
        BuddyConnectApplication.addChatMessage(userMessage);
        
        chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
        progressBar.setVisibility(View.VISIBLE);
        
        final String finalMessage = message;
        
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isAdded() && progressBar.getVisibility() == View.VISIBLE) {
                    progressBar.setVisibility(View.GONE);
                    sendButton.setEnabled(true);
                    Toast.makeText(requireContext(), R.string.api_timeout, Toast.LENGTH_SHORT).show();
                }
            }
        }, 30000);
        
        chatbotApiService.sendMessage(message, new ChatbotApiService.ChatbotApiCallback() {
            @Override
            public void onSuccess(String response) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            sendButton.setEnabled(true);
                            
                            // Create bot reply message and add to UI and global storage
                            ChatMessage botMessage = new ChatMessage(response, ChatMessage.TYPE_BOT);
                            chatAdapter.addMessage(botMessage);
                            BuddyConnectApplication.addChatMessage(botMessage);
                            
                            chatRecyclerView.scrollToPosition(chatAdapter.getItemCount() - 1);
                        }
                    });
                }
            }

            @Override
            public void onError(String errorMessage) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressBar.setVisibility(View.GONE);
                            sendButton.setEnabled(true);
                            
                            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                            builder.setTitle(R.string.error_title);
                            builder.setMessage(getString(R.string.error_sending_message) + ": " + errorMessage);
                            builder.setPositiveButton(R.string.retry, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    messageEditText.setText(finalMessage);
                                    chatAdapter.removeLastUserMessage();
                                    
                                    // Remove last user message from global storage
                                    List<ChatMessage> history = BuddyConnectApplication.getChatHistory();
                                    if (!history.isEmpty()) {
                                        for (int i = history.size() - 1; i >= 0; i--) {
                                            if (history.get(i).getType() == ChatMessage.TYPE_USER) {
                                                history.remove(i);
                                                break;
                                            }
                                        }
                                    }
                                    
                                    sendMessage();
                                }
                            });
                            builder.setNegativeButton(android.R.string.cancel, null);
                            builder.show();
                            
                            Log.e(TAG, "Error sending message: " + errorMessage);
                        }
                    });
                }
            }
        });
    }
} 