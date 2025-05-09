package edu.cuhk.csci3310.buddyconnect.chatbot;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/*
 * API service for chat AI communication
 */
public class ChatbotApiService {
    private static final String TAG = "ChatbotApiService";
    private static final String API_URL = "https://api.x.ai/v1/chat/completions";
    private static final int SOCKET_TIMEOUT_MS = 10000;

    private final Context context;
    private final String apiKey;
    private final RequestQueue requestQueue;

    public interface ChatbotApiCallback {
        void onSuccess(String response);
        void onError(String errorMessage);
    }

    public ChatbotApiService(Context context, String apiKey) {
        this.context = context;
        this.apiKey = apiKey;
        this.requestQueue = Volley.newRequestQueue(context);
    }

    public void sendMessage(String message, ChatbotApiCallback callback) {
        if (TextUtils.isEmpty(message)) {
            callback.onError("Message cannot be empty");
            return;
        }
        
        if (TextUtils.isEmpty(apiKey)) {
            callback.onError("API key not set");
            return;
        }
        
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "grok-2-latest");
            
            JSONArray messagesArray = new JSONArray();
            
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are a helpful assistant.");
            messagesArray.put(systemMessage);
            
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", sanitizeMessage(message));
            messagesArray.put(userMessage);
            
            jsonBody.put("messages", messagesArray);
            jsonBody.put("temperature", 0.7);
            jsonBody.put("stream", false);
            jsonBody.put("max_tokens", 1000);

            Log.d(TAG, "Sending request: " + jsonBody.toString());

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    API_URL,
                    jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                Log.d(TAG, "Received response: " + response.toString());
                                
                                String responseText;
                                
                                if (response.has("choices")) {
                                    JSONArray choices = response.getJSONArray("choices");
                                    if (choices.length() > 0) {
                                        JSONObject choice = choices.getJSONObject(0);
                                        if (choice.has("message")) {
                                            JSONObject messageObj = choice.getJSONObject("message");
                                            responseText = messageObj.getString("content");
                                        } else {
                                            responseText = choice.optString("text", "Unable to get response content");
                                        }
                                    } else {
                                        responseText = "API did not return valid choices";
                                    }
                                } else {
                                    responseText = "API response format error: " + response.toString().substring(0, Math.min(100, response.toString().length())) + "...";
                                }
                                
                                callback.onSuccess(responseText);
                            } catch (JSONException e) {
                                Log.e(TAG, "JSON parsing error: " + e.getMessage() + "\nResponse: " + response.toString());
                                callback.onError("Error parsing response: " + e.getMessage());
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            String errorBody = "";
                            String errorMessage = "Network error, please check connection";
                            int statusCode = 0;
                            
                            NetworkResponse networkResponse = error.networkResponse;
                            if (networkResponse != null) {
                                statusCode = networkResponse.statusCode;
                                if (networkResponse.data != null) {
                                    try {
                                        errorBody = new String(networkResponse.data, "UTF-8");
                                        try {
                                            JSONObject errorJson = new JSONObject(errorBody);
                                            if (errorJson.has("error")) {
                                                JSONObject errorObj = errorJson.getJSONObject("error");
                                                if (errorObj.has("message")) {
                                                    errorMessage = errorObj.getString("message");
                                                } 
                                            }
                                        } catch (JSONException e) {
                                            Log.e(TAG, "Failed to parse error JSON: " + e.getMessage());
                                        }
                                    } catch (UnsupportedEncodingException e) {
                                        Log.e(TAG, "Error response encoding issue: " + e.getMessage());
                                    }
                                }
                            }
                            
                            if (error instanceof AuthFailureError) {
                                errorMessage = "API key authentication failed, please check your key";
                            } else if (error.toString().contains("TimeoutError")) {
                                errorMessage = "Request timeout, please check network or try again later";
                            } else if (statusCode == 400) {
                                errorMessage = "Request format error: " + errorBody;
                            } else if (statusCode == 401) {
                                errorMessage = "Invalid or expired API key";
                            } else if (statusCode >= 500) {
                                errorMessage = "Server error, please try again later";
                            }
                            
                            Log.e(TAG, "Network error: " + error.toString() + " Status code: " + statusCode + "\nError content: " + errorBody);
                            callback.onError(errorMessage);
                        }
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Authorization", "Bearer " + apiKey);
                    return headers;
                }
            };

            RetryPolicy policy = new DefaultRetryPolicy(
                    SOCKET_TIMEOUT_MS,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
            request.setRetryPolicy(policy);

            requestQueue.add(request);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating JSON request: " + e.getMessage());
            callback.onError("Error creating request: " + e.getMessage());
        }
    }
    
    /*
     * Validate API key
     */

    public void validateApiKey(ChatbotApiCallback callback) {
        if (TextUtils.isEmpty(apiKey)) {
            callback.onError("API key not set");
            return;
        }
        
        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", "grok-2-latest");
            
            JSONArray messagesArray = new JSONArray();
            
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "You are a helpful assistant.");
            messagesArray.put(systemMessage);
            
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", "test");
            messagesArray.put(userMessage);
            
            jsonBody.put("messages", messagesArray);
            jsonBody.put("temperature", 0.7);
            jsonBody.put("stream", false);
            jsonBody.put("max_tokens", 10); // Use 10 token to verify

            Log.d(TAG, "Validating API key with test request");

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST,
                    API_URL,
                    jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // check API key! If hv response, its valid
                            callback.onSuccess("API key is valid");
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            String errorMessage = "API key validation failed";
                            
                            NetworkResponse networkResponse = error.networkResponse;
                            if (networkResponse != null) {
                                int statusCode = networkResponse.statusCode;
                                if (networkResponse.data != null) {
                                    try {
                                        String errorBody = new String(networkResponse.data, "UTF-8");
                                        try {
                                            JSONObject errorJson = new JSONObject(errorBody);
                                            if (errorJson.has("error")) {
                                                JSONObject errorObj = errorJson.getJSONObject("error");
                                                if (errorObj.has("message")) {
                                                    errorMessage = errorObj.getString("message");
                                                } 
                                            }
                                        } catch (JSONException e) {
                                            Log.e(TAG, "Failed to parse error JSON: " + e.getMessage());
                                        }
                                    } catch (UnsupportedEncodingException e) {
                                        Log.e(TAG, "Error response encoding issue: " + e.getMessage());
                                    }
                                }
                                
                                if (statusCode == 401) {
                                    errorMessage = "Invalid or expired API key";
                                }
                            }
                            
                            if (error instanceof AuthFailureError) {
                                errorMessage = "API key authentication failed";
                            }
                            
                            Log.e(TAG, "API key validation error: " + error.toString());
                            callback.onError(errorMessage);
                        }
                    }
            ) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Authorization", "Bearer " + apiKey);
                    return headers;
                }
            };

            // 縮短超時時間，加快驗證速度
            RetryPolicy policy = new DefaultRetryPolicy(
                    5000,
                    1,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT);
            request.setRetryPolicy(policy);

            requestQueue.add(request);
        } catch (JSONException e) {
            Log.e(TAG, "Error creating validation request: " + e.getMessage());
            callback.onError("Error creating validation request: " + e.getMessage());
        }
    }

    private String sanitizeMessage(String message) {
        if (message == null) {
            return "";
        }
        
        message = message.replaceAll("\\p{C}", "");
        message = message.replace("\"", "\\\"");
        
        return message.trim();
    }
} 