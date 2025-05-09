package edu.cuhk.csci3310.buddyconnect.Record_scheduler;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import edu.cuhk.csci3310.buddyconnect.R;
import edu.cuhk.csci3310.buddyconnect.friendRequest.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SchedulerFriendManager {
    private final Context context;
    private final String schedulerId;
    private final String userId;
    private final FirebaseFirestore db;
    private PopupWindow popupWindow;

    public SchedulerFriendManager(Context context, String schedulerId) {
        this.context = context;
        this.schedulerId = schedulerId;
        this.userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        this.db = FirebaseFirestore.getInstance();
    }

    public void showFriendInviteMenu(View anchor, List<User> friends) {
        View popupView = LayoutInflater.from(context).inflate(R.layout.popup_friend_invite, null);
        LinearLayout friendListContainer = popupView.findViewById(R.id.friend_list_container);

        // Load current members and invitations to determine button states
        Map<String, String> friendStatus = new HashMap<>();
        db.collection("schedulers").document(schedulerId).get()
                .addOnSuccessListener(doc -> {
                    Map<String, Object> members = (Map<String, Object>) doc.get("members");
                    if (members != null) {
                        for (String memberId : members.keySet()) {
                            friendStatus.put(memberId, "accepted");
                        }
                    }
                    // Check pending invitations
                    for (User friend : friends) {
                        db.collection("users").whereEqualTo("email", friend.getEmail()).get()
                                .addOnSuccessListener(query -> {
                                    if (!query.isEmpty()) {
                                        String friendId = query.getDocuments().get(0).getId();
                                        db.collection("users").document(friendId).collection("invitations")
                                                .whereEqualTo("schedulerId", schedulerId)
                                                .whereEqualTo("senderId", userId)
                                                .whereEqualTo("status", "pending")
                                                .get()
                                                .addOnSuccessListener(inviteQuery -> {
                                                    if (!inviteQuery.isEmpty()) {
                                                        friendStatus.put(friendId, "pending");
                                                    }
                                                    populateFriendList(friendListContainer, friends, friendStatus);
                                                });
                                    }
                                });
                    }
                });

        popupWindow = new PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popupWindow.showAsDropDown(anchor);
    }

    private void populateFriendList(LinearLayout container, List<User> friends, Map<String, String> friendStatus) {
        container.removeAllViews(); // Clear previous views
        for (User friend : friends) {
            View friendView = LayoutInflater.from(context).inflate(R.layout.friend_invite_item, container, false);
            TextView friendName = friendView.findViewById(R.id.friend_name);
            Button inviteButton = friendView.findViewById(R.id.invite_button);

            friendName.setText(friend.getDisplayName());

            db.collection("users").whereEqualTo("email", friend.getEmail()).get()
                    .addOnSuccessListener(query -> {
                        if (!query.isEmpty()) {
                            String friendId = query.getDocuments().get(0).getId();
                            String status = friendStatus.getOrDefault(friendId, "none");

                            if ("accepted".equals(status)) {
                                inviteButton.setVisibility(View.GONE);
                            } else if ("pending".equals(status)) {
                                inviteButton.setText("Wait");
                                inviteButton.setEnabled(false);
                            } else {
                                inviteButton.setText("Invite");
                                inviteButton.setOnClickListener(v -> inviteFriend(friend, friendId, inviteButton));
                            }
                        }
                    });

            container.addView(friendView);
        }
    }

    private void inviteFriend(User friend, String friendId, Button inviteButton) {
        String inviteId = db.collection("users").document(friendId).collection("invitations").document().getId();
        Map<String, Object> inviteData = new HashMap<>();
        inviteData.put("schedulerId", schedulerId);
        inviteData.put("senderId", userId);
        inviteData.put("senderDisplayName", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        inviteData.put("status", "pending");

        db.collection("users").document(friendId).collection("invitations").document(inviteId)
                .set(inviteData)
                .addOnSuccessListener(aVoid -> {
                    inviteButton.setText("Wait");
                    inviteButton.setEnabled(false);
                    Toast.makeText(context, "Invited " + friend.getDisplayName(), Toast.LENGTH_SHORT).show();
                });
    }

    public void dismissPopup() {
        if (popupWindow != null && popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }
}