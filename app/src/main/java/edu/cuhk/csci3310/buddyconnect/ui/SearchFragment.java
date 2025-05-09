package edu.cuhk.csci3310.buddyconnect.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

import edu.cuhk.csci3310.buddyconnect.BaseActivity;
import edu.cuhk.csci3310.buddyconnect.R;
import edu.cuhk.csci3310.buddyconnect.friendRequest.User;
import edu.cuhk.csci3310.buddyconnect.friendRequest.UserAdapter;

public class SearchFragment extends Fragment {

    private RecyclerView recyclerView;
    private UserAdapter adapter;
    private List<User> userList = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        View statusBarSpaceView = view.findViewById(R.id.statusBarSpaceView);
        BaseActivity.adjustStatusBarSpace(requireContext(), statusBarSpaceView);

        recyclerView = view.findViewById(R.id.userRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        adapter = new UserAdapter(userList, currentUserId);
        recyclerView.setAdapter(adapter);

        loadUsers(currentUserId);
    }

    private void loadUsers(String currentUserId) {
        FirebaseFirestore.getInstance().collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    userList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        if (!doc.getId().equals(currentUserId)) {
                            User user = doc.toObject(User.class);
                            userList.add(user);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}