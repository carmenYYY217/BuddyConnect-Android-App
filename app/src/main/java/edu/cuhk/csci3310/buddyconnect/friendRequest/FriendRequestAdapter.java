package edu.cuhk.csci3310.buddyconnect.friendRequest;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.List;
import edu.cuhk.csci3310.buddyconnect.R;
import edu.cuhk.csci3310.buddyconnect.friendRequest.User;

public class FriendRequestAdapter extends RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {
    public interface FriendChangeListener {
        void onFriendListChanged();
    }
    private FriendChangeListener listener;
    private List<User> requestList;
    private String currentUserId;

    public FriendRequestAdapter(List<User> requestList, String currentUserId, FriendChangeListener listener) {
        this.requestList = requestList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friend_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = requestList.get(position);
        holder.displayName.setText(user.getDisplayName());
        holder.email.setText(user.getEmail());

        String status = user.getStatus();
        if ("accepted".equals(status)) {
            holder.acceptButton.setVisibility(View.GONE);
            holder.declineButton.setVisibility(View.GONE);
            holder.friendStatusButton.setVisibility(View.VISIBLE);
        } else if ("pending".equals(status)) {
            holder.acceptButton.setVisibility(View.VISIBLE);
            holder.declineButton.setVisibility(View.VISIBLE);
            holder.friendStatusButton.setVisibility(View.GONE);

            holder.acceptButton.setOnClickListener(v -> acceptRequest(user, holder));
            holder.declineButton.setOnClickListener(v -> declineRequest(user, holder, position));
        }
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    private void acceptRequest(User requester, ViewHolder holder) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String requesterEmail = requester.getEmail();
        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Fetch requester's activityStatus from received_requests
        db.collection("users")
                .document(currentUserId)
                .collection("received_requests")
                .document(requesterEmail)
                .get()
                .addOnSuccessListener(requestDoc -> {
                    final String initialActivityStatus = requestDoc.exists() ? requestDoc.getString("activityStatus") : null;
                    Log.d("FriendRequestAdapter", "Fetched initialActivityStatus from received_requests: " + initialActivityStatus);

                    // Fetch requester's data from users collection
                    db.collection("users")
                            .whereEqualTo("email", requesterEmail)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (!querySnapshot.isEmpty()) {
                                    final String requesterId = querySnapshot.getDocuments().get(0).getId();
                                    final String requesterPhotoUrl = querySnapshot.getDocuments().get(0).getString("photoUrl");
                                    final String requesterActivityStatus = (initialActivityStatus != null) ? initialActivityStatus : querySnapshot.getDocuments().get(0).getString("activityStatus");
                                    Log.d("FriendRequestAdapter", "Final requesterActivityStatus: " + requesterActivityStatus);

                                    // Fetch current user's data
                                    db.collection("users")
                                            .document(currentUserId)
                                            .get()
                                            .addOnSuccessListener(currentUserDoc -> {
                                                final String currentUserDisplayName = currentUserDoc.getString("displayName");
                                                final String currentUserPhotoUrl = currentUserDoc.getString("photoUrl");
                                                final String currentUserActivityStatus = currentUserDoc.getString("activityStatus");
                                                Log.d("FriendRequestAdapter", "Fetched currentUserActivityStatus: " + currentUserActivityStatus);

                                                // Save to current user's friends subcollection
                                                User friendForCurrent = new User(requester.getDisplayName(), requesterEmail, "accepted", requesterPhotoUrl, requesterActivityStatus);
                                                db.collection("users")
                                                        .document(currentUserId)
                                                        .collection("friends")
                                                        .document(requesterEmail)
                                                        .set(friendForCurrent)
                                                        .addOnSuccessListener(aVoid -> {
                                                            Log.d("FriendRequestAdapter", "Saved friend with photoUrl: " + requesterPhotoUrl + ", activityStatus: " + requesterActivityStatus);
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.e("FriendRequestAdapter", "Error saving friend: " + e.getMessage());
                                                        });

                                                // Save to requester's friends subcollection
                                                User friendForRequester = new User(currentUserDisplayName, currentUserEmail, "accepted", currentUserPhotoUrl, currentUserActivityStatus);
                                                db.collection("users")
                                                        .document(requesterId)
                                                        .collection("friends")
                                                        .document(currentUserEmail)
                                                        .set(friendForRequester)
                                                        .addOnSuccessListener(aVoid -> {
                                                            Log.d("FriendRequestAdapter", "Saved friend for requester with photoUrl: " + currentUserPhotoUrl + ", activityStatus: " + currentUserActivityStatus);
                                                        })
                                                        .addOnFailureListener(e -> {
                                                            Log.e("FriendRequestAdapter", "Error saving friend for requester: " + e.getMessage());
                                                        });

                                                // Remove the request from received_requests
                                                db.collection("users")
                                                        .document(currentUserId)
                                                        .collection("received_requests")
                                                        .document(requesterEmail)
                                                        .delete()
                                                        .addOnSuccessListener(aVoid -> {
                                                            Toast.makeText(holder.itemView.getContext(), "Friend request accepted", Toast.LENGTH_SHORT).show();
                                                            holder.acceptButton.setVisibility(View.GONE);
                                                            holder.declineButton.setVisibility(View.GONE);
                                                            holder.friendStatusButton.setVisibility(View.VISIBLE);
                                                            if (listener != null) {
                                                                listener.onFriendListChanged();
                                                            }
                                                        });
                                            });
                                } else {
                                    Log.e("FriendRequestAdapter", "No user found for email: " + requesterEmail);
                                }
                            });
                });
    }

    private void declineRequest(User requester, ViewHolder holder, int position) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String requesterEmail = requester.getEmail();
        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        db.collection("users")
                .document(currentUserId)
                .collection("received_requests")
                .document(requesterEmail)
                .delete()
                .addOnSuccessListener(unused -> {
                    db.collection("users")
                            .whereEqualTo("email", requesterEmail)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (!querySnapshot.isEmpty()) {
                                    String requesterId = querySnapshot.getDocuments().get(0).getId();
                                    db.collection("users")
                                            .document(requesterId)
                                            .collection("friends")
                                            .document(currentUserEmail)
                                            .update("friendshipStatus", "declined")
                                            .addOnSuccessListener(success -> {
                                                Toast.makeText(holder.itemView.getContext(), "Friend request declined", Toast.LENGTH_SHORT).show();
                                                requestList.remove(position);
                                                notifyItemRemoved(position);
                                                notifyItemRangeChanged(position, requestList.size());
                                            });
                                }
                            });
                });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView displayName, email;
        Button acceptButton, declineButton, friendStatusButton;

        public ViewHolder(View itemView) {
            super(itemView);
            displayName = itemView.findViewById(R.id.displayNameTextView);
            email = itemView.findViewById(R.id.emailTextView);
            acceptButton = itemView.findViewById(R.id.acceptButton);
            declineButton = itemView.findViewById(R.id.declineButton);
            friendStatusButton = itemView.findViewById(R.id.friendStatusButton);
        }
    }
}