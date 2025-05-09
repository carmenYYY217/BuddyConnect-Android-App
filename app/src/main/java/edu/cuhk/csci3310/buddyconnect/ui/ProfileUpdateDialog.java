package edu.cuhk.csci3310.buddyconnect.ui;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import edu.cuhk.csci3310.buddyconnect.MainActivity;
import edu.cuhk.csci3310.buddyconnect.R;

public class ProfileUpdateDialog extends Dialog {
    private Context context;
    private ImageView profileImageView;
    private EditText displayNameEditText;
    private Button saveButton;
    private ImageView closeIcon;
    private Uri selectedImageUri;
    private String currentDisplayName;
    private String currentPhotoUrl;
    private ActivityResultLauncher<String> imagePickerLauncher;

    public ProfileUpdateDialog(Context context, String currentDisplayName, String currentPhotoUrl,
                               ActivityResultLauncher<String> imagePickerLauncher) {
        super(context);
        this.context = context;
        this.currentDisplayName = currentDisplayName != null ? currentDisplayName : "";
        this.currentPhotoUrl = currentPhotoUrl;
        this.imagePickerLauncher = imagePickerLauncher;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_profile_update, null);
        setContentView(view);

        profileImageView = view.findViewById(R.id.profileImageView);
        displayNameEditText = view.findViewById(R.id.displayNameEditText);
        saveButton = view.findViewById(R.id.saveButton);
        closeIcon = view.findViewById(R.id.closeIcon);
        TextView titleText = view.findViewById(R.id.titleText);
        titleText.setText("Update Profile");

        displayNameEditText.setText(currentDisplayName);
        if (currentPhotoUrl != null && !currentPhotoUrl.isEmpty()) {
            Glide.with(context).load(currentPhotoUrl).into(profileImageView);
        } else {
            profileImageView.setImageResource(R.drawable.ic_account_icon);
        }

        if (context instanceof MainActivity) {
            ((MainActivity) context).setCurrentDialog(this);
        }

        profileImageView.setOnClickListener(v -> {
            if (imagePickerLauncher != null) {
                imagePickerLauncher.launch("image/*");
            } else {
                Toast.makeText(context, "Image picker not initialized", Toast.LENGTH_SHORT).show();
            }
        });

        saveButton.setOnClickListener(v -> {
            String newDisplayName = displayNameEditText.getText().toString().trim();
            if (newDisplayName.isEmpty()) {
                Toast.makeText(context, "Display name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            saveButton.setEnabled(false);
            if (selectedImageUri != null) {
                uploadImageAndGetUrl(selectedImageUri, photoUrl -> {
                    String finalPhotoUrl = (photoUrl != null && !photoUrl.isEmpty()) ? photoUrl : currentPhotoUrl;
                    updateUserProfile(newDisplayName, finalPhotoUrl);
                });
            } else {
                updateUserProfile(newDisplayName, currentPhotoUrl);
            }
        });

        closeIcon.setOnClickListener(v -> {
            selectedImageUri = null;
            dismiss();
        });
    }

    private void uploadImageAndGetUrl(Uri imageUri, OnImageUploadListener listener) {
        if (imageUri == null || imageUri.getScheme() == null) {
            Log.e("ProfileUpdate", "Invalid or null image URI");
            listener.onUploadComplete(null);
            return;
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {
            Log.e("ProfileUpdate", "User is not authenticated");
            listener.onUploadComplete(null);
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        StorageReference storageRef = FirebaseStorage.getInstance().getReference()
                .child("profile_pictures/" + userId + ".jpg");

        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                String photoUrl = uri.toString();
                                listener.onUploadComplete(photoUrl);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("ProfileUpdate", "Failed to get download URL", e);
                                listener.onUploadComplete(null);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("ProfileUpdate", "Upload failed", e);
                    listener.onUploadComplete(null);
                });
    }

    private void updateUserProfile(String displayName, String photoUrl) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Fetch the current activityStatus to preserve it
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    String activityStatus = documentSnapshot.exists() ? documentSnapshot.getString("activityStatus") : "active";

                    // Update the user document with all fields
                    db.collection("users").document(userId)
                            .update(
                                    "displayName", displayName,
                                    "photoUrl", photoUrl,
                                    "activityStatus", activityStatus // Preserve the activityStatus
                            )
                            .addOnSuccessListener(aVoid -> {
                                if (context instanceof MainActivity) {
                                    ((MainActivity) context).updateProfileUI(displayName, photoUrl);
                                }
                                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show();
                                dismiss();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("ProfileUpdate", "Failed to update profile", e);
                                Toast.makeText(context, "Failed to update profile", Toast.LENGTH_SHORT).show();
                                saveButton.setEnabled(true);
                            });
                });
    }

    interface OnImageUploadListener {
        void onUploadComplete(String photoUrl);
    }

    public void setSelectedImageUri(Uri uri) {
        this.selectedImageUri = uri;
        if (uri != null && profileImageView != null) {
            Glide.with(context).load(uri).into(profileImageView);
        }
    }
}