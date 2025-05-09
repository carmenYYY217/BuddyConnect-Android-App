package edu.cuhk.csci3310.buddyconnect.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import edu.cuhk.csci3310.buddyconnect.MainActivity;
import edu.cuhk.csci3310.buddyconnect.R;
import edu.cuhk.csci3310.buddyconnect.utils.ThemeHelper;

public class UserProfileFragment extends Fragment {
    private ImageView userIcon;
    private TextView displayNameText;
    private TextView emailText;
    private RadioButton activeRadioButton, inactiveRadioButton;
    private RadioGroup statusRadioGroup;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile, container, false);
        view.setBackgroundColor(ThemeHelper.getBottomBarSelectedColor(requireContext()));

        userIcon = view.findViewById(R.id.userIcon);
        displayNameText = view.findViewById(R.id.displayNameText);
        emailText = view.findViewById(R.id.emailText);
        activeRadioButton = view.findViewById(R.id.activeRadioButton);
        inactiveRadioButton = view.findViewById(R.id.inactiveRadioButton);
        statusRadioGroup = view.findViewById(R.id.statusRadioGroup);
        ImageButton leaveButton = view.findViewById(R.id.leaveButton);

        leaveButton.setOnClickListener(v -> requireActivity().onBackPressed());

        userIcon.setOnClickListener(v -> {
            String currentDisplayName = displayNameText.getText().toString();
            String currentPhotoUrl = (String) userIcon.getTag();
            if (getActivity() instanceof MainActivity) {
                ActivityResultLauncher<String> launcher = ((MainActivity) getActivity()).getImagePickerLauncher();
                ProfileUpdateDialog dialog = new ProfileUpdateDialog(getContext(), currentDisplayName, currentPhotoUrl, launcher);
                dialog.show();
            }
        });

        // Set up status button listeners
        statusRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String newStatus = checkedId == R.id.activeRadioButton ? "active" : "inactive";
            updateStatus(newStatus);
        });

        loadUserProfile();

        return view;
    }

    private void loadUserProfile() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String displayName = documentSnapshot.getString("displayName");
                        String photoUrl = documentSnapshot.getString("photoUrl");
                        String email = documentSnapshot.getString("email");
                        String activityStatus = documentSnapshot.getString("activityStatus"); // Updated field name

                        displayNameText.setText(displayName != null && !displayName.isEmpty() ? displayName : "Display Name");
                        emailText.setText(email != null && !email.isEmpty() ? email : "user@example.com");

                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(this).load(photoUrl).circleCrop().into(userIcon);
                            userIcon.setTag(photoUrl);
                        } else {
                            Glide.with(this).load(R.drawable.ic_account_icon).circleCrop().into(userIcon);
                        }

                        // Set default status to "active" if not present
                        String currentStatus = activityStatus != null && !activityStatus.isEmpty() ? activityStatus : "active";
                        if (currentStatus.equals("active")) {
                            activeRadioButton.setChecked(true);
                        } else {
                            inactiveRadioButton.setChecked(true);
                        }
                    } else {
                        displayNameText.setText("Display Name");
                        emailText.setText("user@example.com");
                        Glide.with(this).load(R.drawable.ic_account_icon).circleCrop().into(userIcon);
                        activeRadioButton.setChecked(true); // Default to active
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("UserProfileFragment", "Failed to load profile", e);
                    Toast.makeText(getContext(), "Failed to load profile", Toast.LENGTH_SHORT).show();
                    displayNameText.setText("Display Name");
                    emailText.setText("user@example.com");
                    Glide.with(this).load(R.drawable.ic_account_icon).circleCrop().into(userIcon);
                    activeRadioButton.setChecked(true); // Default to active on failure
                });
    }

    private void updateStatus(String newStatus) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Update the user's activityStatus in the users collection
        db.collection("users").document(userId)
                .update("activityStatus", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Log.d("UserProfileFragment", "User activityStatus updated to " + newStatus);
                    // Update activityStatus in all friends subcollections where this user is listed
                    updateFriendsActivityStatus(userEmail, newStatus);
                })
                .addOnFailureListener(e -> {
                    Log.e("UserProfileFragment", "Failed to update status", e);
                    Toast.makeText(getContext(), "Failed to update status", Toast.LENGTH_SHORT).show();
                    // Revert UI on failure
                    if ("active".equals(newStatus)) {
                        inactiveRadioButton.setChecked(true);
                    } else {
                        activeRadioButton.setChecked(true);
                    }
                });
    }

    private void updateFriendsActivityStatus(String userEmail, String newStatus) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Find all users who have this user as a friend
        db.collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot userDoc : querySnapshot) {
                        String friendUserId = userDoc.getId();
                        db.collection("users")
                                .document(friendUserId)
                                .collection("friends")
                                .whereEqualTo("email", userEmail)
                                .whereEqualTo("friendshipStatus", "accepted")
                                .get()
                                .addOnSuccessListener(friendsSnapshot -> {
                                    for (QueryDocumentSnapshot friendDoc : friendsSnapshot) {
                                        db.collection("users")
                                                .document(friendUserId)
                                                .collection("friends")
                                                .document(userEmail)
                                                .update("activityStatus", newStatus)
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.d("UserProfileFragment", "Updated activityStatus to " + newStatus + " in friends list of user " + friendUserId);
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e("UserProfileFragment", "Failed to update activityStatus in friends list", e);
                                                });
                                    }
                                });
                    }
                });
    }

    public void updateProfileUI(String displayName, String photoUrl) {
        displayNameText.setText(displayName != null && !displayName.isEmpty() ? displayName : "Display Name");
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this).load(photoUrl).circleCrop().into(userIcon);
            userIcon.setTag(photoUrl);
        } else {
            Glide.with(this).load(R.drawable.ic_account_icon).circleCrop().into(userIcon);
        }
    }
}