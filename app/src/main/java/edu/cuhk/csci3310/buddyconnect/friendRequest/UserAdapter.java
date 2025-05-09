package edu.cuhk.csci3310.buddyconnect.friendRequest;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import edu.cuhk.csci3310.buddyconnect.R;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> users;
    private String currentUserId;

    public UserAdapter(List<User> users, String currentUserId) {
        this.users = users;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.search_item, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.displayName.setText(user.getDisplayName());
        holder.email.setText(user.getEmail());

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String otherUserEmail = user.getEmail();
        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        // Check if current user has already sent a request
        db.collection("users")
                .document(currentUserId)
                .collection("friends")
                .document(otherUserEmail)
                .get()
                .addOnSuccessListener(friendDoc -> {
                    if (friendDoc.exists()) {
                        String friendshipStatus = friendDoc.getString("friendshipStatus");
                        if ("pending".equals(friendshipStatus)) {
                            holder.addButton.setText("Requested");
                            holder.addButton.setEnabled(false);
                        } else if ("accepted".equals(friendshipStatus)) {
                            holder.addButton.setText("Friend");
                            holder.addButton.setEnabled(false);
                        } else if ("declined".equals(friendshipStatus)) {
                            db.collection("users")
                                    .document(currentUserId)
                                    .collection("friends")
                                    .document(otherUserEmail)
                                    .delete();
                            checkIfUserRequestedCurrentUser(holder, db, otherUserEmail, currentUserEmail);
                        } else {
                            checkIfUserRequestedCurrentUser(holder, db, otherUserEmail, currentUserEmail);
                        }
                    } else {
                        checkIfUserRequestedCurrentUser(holder, db, otherUserEmail, currentUserEmail);
                    }
                });

        holder.addButton.setOnClickListener(v -> {
            // Fetch current user's data including photoUrl and activityStatus
            db.collection("users")
                    .document(currentUserId)
                    .get()
                    .addOnSuccessListener(currentUserDoc -> {
                        String currentUserDisplayName = currentUserDoc.getString("displayName");
                        String currentUserPhotoUrl = currentUserDoc.getString("photoUrl");
                        String currentUserActivityStatus = currentUserDoc.getString("activityStatus");

                        // Fetch target user's data including photoUrl and activityStatus
                        db.collection("users")
                                .whereEqualTo("email", otherUserEmail)
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    if (!querySnapshot.isEmpty()) {
                                        String targetUserId = querySnapshot.getDocuments().get(0).getId();
                                        String targetUserPhotoUrl = querySnapshot.getDocuments().get(0).getString("photoUrl");
                                        String targetUserActivityStatus = querySnapshot.getDocuments().get(0).getString("activityStatus");

                                        // Save to current user's 'friends' subcollection
                                        User friendForCurrent = new User(user.getDisplayName(), otherUserEmail, "pending", targetUserPhotoUrl, targetUserActivityStatus);
                                        db.collection("users")
                                                .document(currentUserId)
                                                .collection("friends")
                                                .document(otherUserEmail)
                                                .set(friendForCurrent);

                                        // Save to target user's 'received_requests' subcollection
                                        User requestForTarget = new User(currentUserDisplayName, currentUserEmail, "pending", currentUserPhotoUrl, currentUserActivityStatus);
                                        db.collection("users")
                                                .document(targetUserId)
                                                .collection("received_requests")
                                                .document(currentUserEmail)
                                                .set(requestForTarget);
                                    }
                                });

                        holder.addButton.setText("Requested");
                        holder.addButton.setEnabled(false);
                    });
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView displayName, email;
        Button addButton;

        public UserViewHolder(View itemView) {
            super(itemView);
            displayName = itemView.findViewById(R.id.displayNameTextView);
            email = itemView.findViewById(R.id.emailTextView);
            addButton = itemView.findViewById(R.id.addFriendButton);
        }
    }

    private void checkIfUserRequestedCurrentUser(UserViewHolder holder, FirebaseFirestore db, String otherUserEmail, String currentUserEmail) {
        db.collection("users")
                .whereEqualTo("email", otherUserEmail)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String otherUserId = querySnapshot.getDocuments().get(0).getId();
                        db.collection("users")
                                .document(otherUserId)
                                .collection("friends")
                                .document(currentUserEmail)
                                .get()
                                .addOnSuccessListener(requestedDoc -> {
                                    if (requestedDoc.exists() && "pending".equals(requestedDoc.getString("friendshipStatus"))) {
                                        holder.addButton.setText("Pending");
                                        holder.addButton.setEnabled(false);
                                    } else {
                                        holder.addButton.setText("Request");
                                        holder.addButton.setEnabled(true);
                                    }
                                });
                    }
                });
    }

}