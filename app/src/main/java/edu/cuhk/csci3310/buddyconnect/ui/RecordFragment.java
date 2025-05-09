package edu.cuhk.csci3310.buddyconnect.ui;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import android.widget.ImageButton;
import android.widget.PopupWindow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.cuhk.csci3310.buddyconnect.MainActivity;
import edu.cuhk.csci3310.buddyconnect.BaseActivity;
import edu.cuhk.csci3310.buddyconnect.R;
import edu.cuhk.csci3310.buddyconnect.Record_scheduler.SchedulerDialogFragment;

public class RecordFragment extends Fragment {
    private View statusBarSpaceView;
    private LinearLayout schedulerContainer;
    private List<String> schedulerNames;
    private FirebaseFirestore db;
    private String userId;
    private ImageButton notificationButton;
    private ListenerRegistration schedulerListener;
    private ListenerRegistration invitationsListener;
    private ListenerRegistration notificationsListener;
    private Set<String> addedSchedulerIds = new HashSet<>();

    public RecordFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_record, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        statusBarSpaceView = view.findViewById(R.id.statusBarSpaceView);
        schedulerContainer = view.findViewById(R.id.schedulerContainer);
        FloatingActionButton addButton = view.findViewById(R.id.addSchedulerButton);
        notificationButton = view.findViewById(R.id.notification_button);

        schedulerNames = new ArrayList<>();
        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        BaseActivity.adjustStatusBarSpace(requireContext(), statusBarSpaceView);

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (requireActivity() instanceof MainActivity) {
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }
                    }
                });

        addButton.setOnClickListener(v -> showAddSchedulerDialog());
        notificationButton.setOnClickListener(v -> showNotificationMenu());

        loadSchedulersFromFirestore();
        setupNotificationListener();
    }

    private void loadSchedulersFromFirestore() {
        if (schedulerListener != null) {
            schedulerListener.remove();
        }
        schedulerListener = db.collection("users").document(userId).collection("schedulers")
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        if (isAdded() && getContext() != null) {
                            Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }
                    schedulerContainer.removeAllViews();
                    addedSchedulerIds.clear();
                    for (var doc : queryDocumentSnapshots) {
                        String schedulerId = doc.getString("schedulerId");
                        if (schedulerId != null && !addedSchedulerIds.contains(schedulerId)) {
                            db.collection("schedulers").document(schedulerId)
                                    .get()
                                    .addOnSuccessListener(schedulerDoc -> {
                                        if (schedulerDoc.exists()) {
                                            String name = schedulerDoc.getString("name");
                                            Map<String, Object> members = (Map<String, Object>) schedulerDoc.get("members");
                                            String ownerId = schedulerDoc.getString("ownerId");
                                            String friendsStr = buildFriendsString(members, ownerId, userId, ownerId != null && ownerId.equals(userId));
                                            if (name != null) {
                                                boolean isOwner = ownerId != null && ownerId.equals(userId);
                                                addSchedulerRow(name, friendsStr, schedulerId, isOwner);
                                                addedSchedulerIds.add(schedulerId);
                                            }
                                        }
                                    });
                        }
                    }
                });
    }

    private String buildFriendsString(Map<String, Object> members, String ownerId, String userId, boolean isOwner) {
        if (members == null || members.isEmpty()) return "No friends added";
        List<String> friendNames = new ArrayList<>();
        for (Map.Entry<String, Object> entry : members.entrySet()) {
            String memberId = entry.getKey();
            Map<String, Object> memberData = (Map<String, Object>) entry.getValue();
            String displayName = (String) memberData.get("displayName");
            if (isOwner) {
                if (!memberId.equals(userId)) {
                    friendNames.add(displayName);
                }
            } else {
                if (!memberId.equals(userId)) {
                    friendNames.add(memberId.equals(ownerId) ? displayName + " (Owner)" : displayName);
                }
            }
        }
        return friendNames.isEmpty() ? "No friends added" : String.join(", ", friendNames);
    }

    private void showAddSchedulerDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_scheduler, null);
        EditText nameInput = dialogView.findViewById(R.id.schedulerNameInput);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Add Scheduler")
                .setView(dialogView)
                .setPositiveButton("Confirm", (d, which) -> {
                    String name = nameInput.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.collection("schedulers")
                            .whereEqualTo("name", name)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                if (!queryDocumentSnapshots.isEmpty()) {
                                    Toast.makeText(getContext(), "Scheduler name exists", Toast.LENGTH_SHORT).show();
                                } else {
                                    String schedulerId = db.collection("schedulers").document().getId();
                                    Map<String, Object> schedulerData = new HashMap<>();
                                    schedulerData.put("name", name);
                                    schedulerData.put("ownerId", userId);
                                    Map<String, Object> members = new HashMap<>();
                                    Map<String, Object> ownerData = new HashMap<>();
                                    ownerData.put("displayName", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                                    ownerData.put("joinedAt", System.currentTimeMillis());
                                    members.put(userId, ownerData);
                                    schedulerData.put("members", members);

                                    db.collection("schedulers").document(schedulerId)
                                            .set(schedulerData)
                                            .addOnSuccessListener(aVoid -> {
                                                Map<String, Object> userSchedulerRef = new HashMap<>();
                                                userSchedulerRef.put("schedulerId", schedulerId);
                                                db.collection("users").document(userId)
                                                        .collection("schedulers").document(schedulerId)
                                                        .set(userSchedulerRef);
                                            });
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }

    private void addSchedulerRow(String name, String friends, String schedulerId, boolean isOwner) {
        View row = LayoutInflater.from(getContext()).inflate(R.layout.scheduler_row, schedulerContainer, false);

        TextView nameText = row.findViewById(R.id.schedulerName);
        TextView friendsText = row.findViewById(R.id.friendsData);
        ImageButton editIcon = row.findViewById(R.id.editIcon);
        nameText.setText(name);
        friendsText.setText(friends);

        row.setOnClickListener(v -> openSchedulerUI(name, schedulerId));

        if (isOwner) {
            editIcon.setVisibility(View.VISIBLE);
            editIcon.setOnClickListener(v -> editScheduler(row, name, friends, schedulerId));
        } else {
            editIcon.setVisibility(View.GONE);
        }

        row.findViewById(R.id.deleteIcon).setOnClickListener(v -> {
            String message = isOwner ? "Are you sure you want to delete " + name + "? This will remove it for all members."
                    : "Are you sure you want to disconnect from " + name + "? Your activities will be removed.";
            new AlertDialog.Builder(requireContext())
                    .setTitle(isOwner ? "Delete Scheduler" : "Disconnect Scheduler")
                    .setMessage(message)
                    .setPositiveButton("Yes", (d, which) -> {
                        if (isOwner) {
                            deleteSchedulerForAll(schedulerId, name);
                        } else {
                            disconnectScheduler(schedulerId, name);
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        });

        schedulerContainer.addView(row);
    }

    private void deleteSchedulerForAll(String schedulerId, String schedulerName) {
        db.collection("schedulers").document(schedulerId).get()
                .addOnSuccessListener(doc -> {
                    Map<String, Object> members = (Map<String, Object>) doc.get("members");
                    if (members != null) {
                        for (String memberId : members.keySet()) {
                            db.collection("users").document(memberId).collection("schedulers")
                                    .document(schedulerId).delete();

                            if (!memberId.equals(userId)) {
                                Map<String, Object> notification = new HashMap<>();
                                notification.put("message", schedulerName + " has been deleted by its owner.");
                                notification.put("schedulerId", schedulerId);
                                notification.put("timestamp", System.currentTimeMillis());
                                db.collection("users").document(memberId).collection("notifications")
                                        .add(notification);
                            }

                            db.collection("users").document(memberId).collection("invitations")
                                    .whereEqualTo("schedulerId", schedulerId)
                                    .whereEqualTo("status", "pending")
                                    .get()
                                    .addOnSuccessListener(inviteQuery -> {
                                        for (var inviteDoc : inviteQuery) {
                                            inviteDoc.getReference().delete();
                                        }
                                    });
                        }
                    }
                    db.collection("schedulers").document(schedulerId).collection("tasks").get()
                            .addOnSuccessListener(taskQuery -> {
                                for (var taskDoc : taskQuery) {
                                    taskDoc.getReference().delete();
                                }
                                db.collection("schedulers").document(schedulerId).delete();
                            });
                });
    }

    private void disconnectScheduler(String schedulerId, String schedulerName) {
        db.collection("schedulers").document(schedulerId).get()
                .addOnSuccessListener(doc -> {
                    String ownerId = doc.getString("ownerId");
                    if (ownerId != null && !ownerId.equals(userId)) {
                        Map<String, Object> notification = new HashMap<>();
                        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
                        notification.put("message", userName + " has left " + schedulerName);
                        notification.put("schedulerId", schedulerId);
                        notification.put("timestamp", System.currentTimeMillis());
                        db.collection("users").document(ownerId).collection("notifications").add(notification);
                    }
                    db.collection("schedulers").document(schedulerId).update("members." + userId, FieldValue.delete());
                });
        db.collection("users").document(userId).collection("schedulers").document(schedulerId).delete();
        db.collection("schedulers").document(schedulerId).collection("tasks")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(query -> {
                    for (var doc : query) {
                        doc.getReference().delete();
                    }
                });
    }

    private void editScheduler(View row, String oldName, String oldFriends, String schedulerId) {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_scheduler, null);
        EditText nameInput = dialogView.findViewById(R.id.schedulerNameInput);
        nameInput.setText(oldName);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Edit Scheduler")
                .setView(dialogView)
                .setPositiveButton("Save", (d, which) -> {
                    String newName = nameInput.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(getContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    db.collection("schedulers")
                            .whereEqualTo("name", newName)
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots -> {
                                boolean hasConflict = false;
                                for (var doc : queryDocumentSnapshots) {
                                    if (!doc.getId().equals(schedulerId)) {
                                        hasConflict = true;
                                        break;
                                    }
                                }
                                if (hasConflict) {
                                    Toast.makeText(getContext(), "Scheduler name exists", Toast.LENGTH_SHORT).show();
                                } else {
                                    db.collection("schedulers").document(schedulerId)
                                            .update("name", newName)
                                            .addOnSuccessListener(aVoid -> {
                                                TextView nameText = row.findViewById(R.id.schedulerName);
                                                nameText.setText(newName);
                                                schedulerNames.remove(oldName);
                                                schedulerNames.add(newName);
                                            });
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .create();
        dialog.show();
    }

    private void showNotificationMenu() {
        View popupView = LayoutInflater.from(getContext()).inflate(R.layout.popup_notifications, null);
        LinearLayout notificationContainer = popupView.findViewById(R.id.notification_container);
        PopupWindow popupWindow = new PopupWindow(popupView, LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT, true);
        popupWindow.showAsDropDown(notificationButton);

        db.collection("users").document(userId).collection("invitations")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(query -> {
                    for (var doc : query) {
                        View inviteView = LayoutInflater.from(getContext()).inflate(R.layout.invitation_item, notificationContainer, false);
                        TextView inviteText = inviteView.findViewById(R.id.invite_text);
                        Button acceptButton = inviteView.findViewById(R.id.accept_invite_button);
                        Button declineButton = inviteView.findViewById(R.id.decline_invite_button);

                        String senderName = doc.getString("senderDisplayName");
                        String schedulerId = doc.getString("schedulerId");
                        String inviteId = doc.getId();

                        db.collection("schedulers").document(schedulerId).get()
                                .addOnSuccessListener(schedulerDoc -> {
                                    String schedulerName = schedulerDoc.getString("name");
                                    inviteText.setText(senderName + " invited you to " + schedulerName);

                                    acceptButton.setOnClickListener(v -> acceptInvite(inviteId, schedulerId, notificationContainer));
                                    declineButton.setOnClickListener(v -> declineInvite(inviteId, senderName, schedulerId, notificationContainer));
                                });

                        notificationContainer.addView(inviteView);
                    }
                });

        db.collection("users").document(userId).collection("notifications").get()
                .addOnSuccessListener(query -> {
                    for (var doc : query) {
                        View notifyView = LayoutInflater.from(getContext()).inflate(R.layout.notification_item, notificationContainer, false);
                        TextView notifyText = notifyView.findViewById(R.id.notification_text);
                        Button confirmButton = notifyView.findViewById(R.id.confirm_button);

                        notifyText.setText(doc.getString("message"));
                        confirmButton.setOnClickListener(v -> {
                            db.collection("users").document(userId).collection("notifications").document(doc.getId()).delete()
                                    .addOnSuccessListener(aVoid -> {
                                        notificationContainer.removeView(notifyView);
                                        updateNotificationIcon();
                                    });
                        });

                        notificationContainer.addView(notifyView);
                    }
                });
    }

    private void acceptInvite(String inviteId, String schedulerId, LinearLayout container) {
        db.collection("users").document(userId).collection("invitations").document(inviteId)
                .update("status", "accepted")
                .addOnSuccessListener(aVoid -> {
                    Map<String, Object> memberData = new HashMap<>();
                    memberData.put("displayName", FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                    memberData.put("joinedAt", System.currentTimeMillis());
                    db.collection("schedulers").document(schedulerId)
                            .update("members." + userId, memberData)
                            .addOnSuccessListener(aVoid2 -> {
                                Map<String, Object> schedulerRef = new HashMap<>();
                                schedulerRef.put("schedulerId", schedulerId);
                                db.collection("users").document(userId).collection("schedulers").document(schedulerId)
                                        .set(schedulerRef);
                                container.removeAllViews();
                                updateNotificationIcon();
                            });
                });
    }

    private void declineInvite(String inviteId, String senderName, String schedulerId, LinearLayout container) {
        db.collection("users").document(userId).collection("invitations").document(inviteId).delete()
                .addOnSuccessListener(aVoid -> {
                    db.collection("users").whereEqualTo("displayName", senderName).get()
                            .addOnSuccessListener(query -> {
                                if (!query.isEmpty()) {
                                    String senderId = query.getDocuments().get(0).getId();
                                    db.collection("users").document(senderId).collection("invitations")
                                            .whereEqualTo("schedulerId", schedulerId)
                                            .whereEqualTo("senderId", senderId)
                                            .whereEqualTo("status", "pending")
                                            .get()
                                            .addOnSuccessListener(inviteQuery -> {
                                                if (!inviteQuery.isEmpty()) {
                                                    inviteQuery.getDocuments().get(0).getReference().delete();
                                                }
                                            });
                                }
                                container.removeAllViews();
                                updateNotificationIcon();
                            });
                });
    }

    private void setupNotificationListener() {
        invitationsListener = db.collection("users").document(userId).collection("invitations")
                .whereEqualTo("status", "pending")
                .addSnapshotListener((query, e) -> updateNotificationIcon());
        notificationsListener = db.collection("users").document(userId).collection("notifications")
                .addSnapshotListener((query, e) -> updateNotificationIcon());
    }

    private void updateNotificationIcon() {
        db.collection("users").document(userId).collection("invitations")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(inviteQuery -> {
                    db.collection("users").document(userId).collection("notifications").get()
                            .addOnSuccessListener(notifyQuery -> {
                                boolean hasPending = !inviteQuery.isEmpty() || !notifyQuery.isEmpty();
                                notificationButton.setImageResource(hasPending ?
                                        R.drawable.ic_notification_active : R.drawable.ic_notification);
                            });
                });
    }

    private void openSchedulerUI(String schedulerName, String schedulerId) {
        SchedulerDialogFragment dialog = SchedulerDialogFragment.newInstance(schedulerId, schedulerName);
        dialog.show(getParentFragmentManager(), "SchedulerDialog");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (schedulerListener != null) schedulerListener.remove();
        if (invitationsListener != null) invitationsListener.remove();
        if (notificationsListener != null) notificationsListener.remove();
    }
}