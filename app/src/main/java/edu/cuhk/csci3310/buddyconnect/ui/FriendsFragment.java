package edu.cuhk.csci3310.buddyconnect.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import edu.cuhk.csci3310.buddyconnect.BaseActivity;
import edu.cuhk.csci3310.buddyconnect.R;
import edu.cuhk.csci3310.buddyconnect.friendRequest.FriendRequestAdapter;
import edu.cuhk.csci3310.buddyconnect.friendRequest.User;

public class FriendsFragment extends Fragment {
    private RecyclerView recyclerView;
    private List<User> requestList = new ArrayList<>();
    private String currentUserId;
    private TextView noFriendsTextView;
    private FriendRequestAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friends, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View statusBarSpaceView = view.findViewById(R.id.statusBarSpaceView);
        BaseActivity.adjustStatusBarSpace(requireContext(), statusBarSpaceView);

        recyclerView = view.findViewById(R.id.friendRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        noFriendsTextView = view.findViewById(R.id.noFriendsTextView);

        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        loadFriendData();
    }

    private void loadFriendData() {
        requestList.clear();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users")
                .document(currentUserId)
                .collection("received_requests")
                .get()
                .addOnSuccessListener(requestsSnapshot -> {
                    for (QueryDocumentSnapshot doc : requestsSnapshot) {
                        User user = doc.toObject(User.class);
                        user.setFriendshipStatus("pending");
                        requestList.add(user);
                    }

                    db.collection("users")
                            .document(currentUserId)
                            .collection("friends")
                            .get()
                            .addOnSuccessListener(friendsSnapshot -> {
                                for (QueryDocumentSnapshot doc : friendsSnapshot) {
                                    String status = doc.getString("status");
                                    if ("accepted".equals(status)) {
                                        User friend = doc.toObject(User.class);
                                        friend.setFriendshipStatus("accepted");
                                        requestList.add(friend);
                                    }
                                }

                                updateEmptyView();

                                adapter = new FriendRequestAdapter(requestList, currentUserId, this::updateEmptyView);
                                recyclerView.setAdapter(adapter);
                            });
                });
    }

    private void updateEmptyView() {
        if (requestList.isEmpty()) {
            noFriendsTextView.setVisibility(View.VISIBLE);
        } else {
            noFriendsTextView.setVisibility(View.GONE);
        }
    }
}