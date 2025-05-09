package edu.cuhk.csci3310.buddyconnect.Plan;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.cuhk.csci3310.buddyconnect.BaseActivity;
import edu.cuhk.csci3310.buddyconnect.MainActivity;
import edu.cuhk.csci3310.buddyconnect.R;
import edu.cuhk.csci3310.buddyconnect.useraccount.AuthActivity;
import edu.cuhk.csci3310.buddyconnect.useraccount.FirebaseAuthManager;

public class PlanFragment extends Fragment {
    private View statusBarSpaceView;
    private RecyclerView recyclerView;
    private SearchView searchView;
    private BookmarkAdapter adapter;
    private List<Bookmark> bookmarkList = new ArrayList<>(); // Original list
    private List<Bookmark> filteredBookmarkList = new ArrayList<>(); // Filtered list for display
    private Map<Bookmark, String> bookmarkKeys = new HashMap<>(); // Map to store Firestore document IDs for bookmarks
    private CollectionReference bookmarksRef;
    private FirebaseAuthManager authManager;
    private ListenerRegistration bookmarkListener;

    public PlanFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_plan, container, false);

        // Initialize Firebase Auth Manager
        authManager = new FirebaseAuthManager(requireContext());

        // Get the UID directly from FirebaseAuth
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // User not logged in, redirect to AuthActivity
            startActivity(new Intent(requireActivity(), AuthActivity.class));
            requireActivity().finish();
            return view;
        }
        String userId = currentUser.getUid();

        // Initialize Firestore reference
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        bookmarksRef = db.collection("users").document(userId).collection("bookmarks");

        // Initialize UI components
        statusBarSpaceView = view.findViewById(R.id.statusBarSpaceView);
        recyclerView = view.findViewById(R.id.recyclerView);
        searchView = view.findViewById(R.id.searchView);

        // Set up RecyclerView with a 2-column grid layout
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        filteredBookmarkList.addAll(bookmarkList); // Initially, show all bookmarks
        adapter = new BookmarkAdapter(filteredBookmarkList, this::showEditBookmarkDialog, this::confirmDeleteBookmark);
        recyclerView.setAdapter(adapter);

        // Set up FAB for creating new bookmarks
        FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(v -> showBookmarkDialog());

        // Set up SearchView listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterBookmarks(newText);
                return true;
            }
        });

        // Load bookmarks from Firestore
        loadBookmarksFromFirestore();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Adjust the status bar space to prevent overlap with system UI
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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove Firestore listener to prevent memory leaks
        if (bookmarkListener != null) {
            bookmarkListener.remove();
        }
    }

    /**
     * Loads bookmarks from Firestore for the current user.
     */
    private void loadBookmarksFromFirestore() {
        bookmarkListener = bookmarksRef.addSnapshotListener((snapshots, e) -> {
            if (e != null) {
                Toast.makeText(requireContext(), "Failed to load bookmarks: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (snapshots != null) {
                bookmarkList.clear();
                bookmarkKeys.clear();
                for (QueryDocumentSnapshot doc : snapshots) {
                    Bookmark bookmark = doc.toObject(Bookmark.class);
                    bookmarkList.add(bookmark);
                    bookmarkKeys.put(bookmark, doc.getId());
                }
                filterBookmarks(searchView.getQuery().toString());
            }
        });
    }

    /**
     * Filters the bookmark list based on the search query and updates the RecyclerView.
     *
     * @param query The search query entered by the user
     */
    private void filterBookmarks(String query) {
        filteredBookmarkList.clear();
        if (query.isEmpty()) {
            filteredBookmarkList.addAll(bookmarkList); // Show all bookmarks if query is empty
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Bookmark bookmark : bookmarkList) {
                if (bookmark.getName().toLowerCase().contains(lowerCaseQuery)) {
                    filteredBookmarkList.add(bookmark);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showBookmarkDialog() {
        BookmarkDialogFragment dialog = new BookmarkDialogFragment();
        dialog.show(getChildFragmentManager(), "BookmarkDialog");
    }

    private void showEditBookmarkDialog(int position) {
        Bookmark bookmark = filteredBookmarkList.get(position);
        // Find the original position in bookmarkList
        int originalPosition = bookmarkList.indexOf(bookmark);
        BookmarkDialogFragment dialog = BookmarkDialogFragment.newInstance(bookmark, originalPosition);
        dialog.show(getChildFragmentManager(), "BookmarkDialog");
    }

    private void confirmDeleteBookmark(int position) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete Bookmark")
                .setMessage("Are you sure you want to delete this bookmark?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    // Find the original bookmark in bookmarkList
                    Bookmark bookmarkToDelete = filteredBookmarkList.get(position);
                    int originalPosition = bookmarkList.indexOf(bookmarkToDelete);
                    String docId = bookmarkKeys.get(bookmarkToDelete);
                    if (docId != null) {
                        // Delete from Firestore
                        bookmarksRef.document(docId)
                                .delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(requireContext(), "Bookmark deleted", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(requireContext(), "Failed to delete bookmark: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                    // Local list will be updated via Firestore listener
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    public void onBookmarkCreated(Bookmark bookmark) {
        // Ensure the fragment is attached before showing Toast
        if (!isAdded() || getActivity() == null) {
            System.out.println("Fragment not attached, cannot show Toast");
            return;
        }

        // Debug: Log the userId to confirm where the bookmark is being saved
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        System.out.println("Saving bookmark for User ID: " + userId);

        // Save to Firestore
        bookmarksRef.add(bookmark)
                .addOnSuccessListener(documentReference -> {
                    // Double-check we're on the main thread and fragment is attached
                    if (isAdded() && getActivity() != null) {
                        Toast.makeText(requireContext(), "Bookmark saved", Toast.LENGTH_SHORT).show();
                    } else {
                        System.out.println("Fragment not attached, cannot show success Toast");
                    }
                    System.out.println("Bookmark saved successfully with ID: " + documentReference.getId());
                })
                .addOnFailureListener(e -> {
                    // Double-check we're on the main thread and fragment is attached
                    if (isAdded() && getActivity() != null) {
                        Toast.makeText(requireContext(), "Failed to save bookmark: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    } else {
                        System.out.println("Fragment not attached, cannot show failure Toast");
                    }
                    System.out.println("Failed to save bookmark: " + e.getMessage());
                });
        // Local list will be updated via Firestore listener
    }
    public void onBookmarkUpdated(Bookmark updatedBookmark, int position) {
        if (position != -1) {
            Bookmark oldBookmark = bookmarkList.get(position);
            String docId = bookmarkKeys.get(oldBookmark);
            if (docId != null) {
                // Update in Firestore
                bookmarksRef.document(docId)
                        .set(updatedBookmark)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(requireContext(), "Bookmark updated", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(requireContext(), "Failed to update bookmark: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
            // Local list will be updated via Firestore listener
        }
    }
}