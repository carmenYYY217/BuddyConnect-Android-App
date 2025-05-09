package edu.cuhk.csci3310.buddyconnect.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.CompositePageTransformer;
import androidx.viewpager2.widget.MarginPageTransformer;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

import edu.cuhk.csci3310.buddyconnect.BaseActivity;
import edu.cuhk.csci3310.buddyconnect.MainActivity;
import edu.cuhk.csci3310.buddyconnect.Map.MapFragment;
import edu.cuhk.csci3310.buddyconnect.Plan.PlanFragment;
import edu.cuhk.csci3310.buddyconnect.R;
import edu.cuhk.csci3310.buddyconnect.friendRequest.User;
import edu.cuhk.csci3310.buddyconnect.useraccount.FirebaseAuthManager;
import edu.cuhk.csci3310.buddyconnect.ui.SliderAdapter.OnItemClickListener;
import edu.cuhk.csci3310.buddyconnect.utils.FriendAdapter;
import edu.cuhk.csci3310.buddyconnect.utils.ThemeHelper;

public class HomeFragment extends Fragment implements OnItemClickListener {
    private FirebaseAuthManager authManager;
    private View statusBarSpaceView;
    private ViewPager2 viewPager2;
    private List<Slider_item> sliderItems;
    private TextView sliderTitleText;
    private TextView greetingText;
    private ImageView happyButton, sadButton, chillButton, angryButton, sleepyButton;
    private ImageView selectedEmotionButton = null;
    private ImageButton userIconButton, addFriendButton;
    private FirebaseFirestore db;
    private View sliderBackgroundView;
    private RecyclerView friendsRecyclerView;
    private static final String PREFS_NAME = "AppPrefs";
    private static final String SELECTED_THEME_KEY = "selectedTheme";

    public HomeFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        updateExistingFriendsPhotoUrl();
        updateExistingFriendsActivityStatus();
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authManager = new FirebaseAuthManager(requireContext());
        db = FirebaseFirestore.getInstance();

        sliderBackgroundView = view.findViewById(R.id.sliderBackgroundView);
        statusBarSpaceView = view.findViewById(R.id.statusBarSpaceView);
        BaseActivity.adjustStatusBarSpace(requireContext(), statusBarSpaceView);

        greetingText = view.findViewById(R.id.greetingText);
        userIconButton = view.findViewById(R.id.userIconButton);
        happyButton = view.findViewById(R.id.happyButton);
        sadButton = view.findViewById(R.id.sadButton);
        chillButton = view.findViewById(R.id.chillButton);
        angryButton = view.findViewById(R.id.angryButton);
        sleepyButton = view.findViewById(R.id.sleepyButton);
        sliderTitleText = view.findViewById(R.id.sliderTitleText);
        addFriendButton = view.findViewById(R.id.addFriendButton);
        friendsRecyclerView = view.findViewById(R.id.friendsRecyclerView);

        addFriendButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new SearchFragment(), true);
            }
        });

        friendsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        friendsRecyclerView.setNestedScrollingEnabled(true); // Enable smooth sliding

        updateLoginStatus();
        loadUserProfile();
        loadFriends();
        applySavedTheme();

        happyButton.setOnClickListener(v -> toggleThemeButton(happyButton, "happy"));
        sadButton.setOnClickListener(v -> toggleThemeButton(sadButton, "sad"));
        chillButton.setOnClickListener(v -> toggleThemeButton(chillButton, "chill"));
        angryButton.setOnClickListener(v -> toggleThemeButton(angryButton, "angry"));
        sleepyButton.setOnClickListener(v -> toggleThemeButton(sleepyButton, "sleepy"));

        viewPager2 = view.findViewById(R.id.viewPagerImageSlider);
        sliderItems = new ArrayList<>();
        sliderItems.add(new Slider_item(R.drawable.map_image, "Map"));
        sliderItems.add(new Slider_item(R.drawable.plan_image, "Plan"));
        sliderItems.add(new Slider_item(R.drawable.record_image, "Record"));

        viewPager2.setAdapter(new SliderAdapter(sliderItems, viewPager2, this));

        if (!sliderItems.isEmpty()) {
            sliderTitleText.setText(sliderItems.get(0).getTitle());
        }

        viewPager2.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                Slider_item currentItem = sliderItems.get(position);
                sliderTitleText.setText(currentItem.getTitle());
            }
        });

        float density = getResources().getDisplayMetrics().density;
        int paddingDp = 80;
        int paddingPx = (int) (paddingDp * density);
        int screenWidthPx = getResources().getDisplayMetrics().widthPixels;
        int pageWidthPx = screenWidthPx - 2 * paddingPx;
        viewPager2.getLayoutParams().height = pageWidthPx;
        viewPager2.requestLayout();

        viewPager2.setClipToPadding(false);
        viewPager2.setClipChildren(false);
        viewPager2.setOffscreenPageLimit(3);
        viewPager2.getChildAt(0).setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);

        CompositePageTransformer compositePageTransformer = new CompositePageTransformer();
        compositePageTransformer.addTransformer(new MarginPageTransformer(40));
        compositePageTransformer.addTransformer((page, position) -> {
            float r = 1 - Math.abs(position);
            page.setScaleY(0.85f + r * 0.15f);
        });

        viewPager2.setPageTransformer(compositePageTransformer);
        userIconButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).loadFragment(new UserProfileFragment(), true);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLoginStatus();
        applyTheme();
    }

    private void updateLoginStatus() {
        if (authManager.isUserSignedIn()) {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String uid = currentUser.getUid();
                db.collection("users").document(uid).get() // Changed from "users" to "Users"
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String displayName = documentSnapshot.getString("displayName");
                                String photoUrl = documentSnapshot.getString("photoUrl");
                                String nameToDisplay = (displayName != null && !displayName.isEmpty()) ? displayName : "User";
                                updateProfileUI(nameToDisplay, photoUrl);
                            } else {
                                updateProfileUI("user", null); // Default to "User" if document doesn’t exist
                            }
                        })
                        .addOnFailureListener(e -> {
                            updateProfileUI("user", null); // Default to "User" on failure
                        });
            } else {
                greetingText.setText("Welcome!");
            }
        } else {
            greetingText.setText("Welcome!");
        }
    }

    private void loadFriends() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Log.d("HomeFragment", "Loading friends for user: " + userId);
        FirebaseFirestore.getInstance().collection("users").document(userId).collection("friends")
                .whereEqualTo("friendshipStatus", "accepted")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<User> friendsList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        User friend = doc.toObject(User.class);
                        Log.d("HomeFragment", "Friend loaded: " + friend.getDisplayName() + ", photoUrl: " + friend.getPhotoUrl() + ", activityStatus: " + friend.getActivityStatus());
                        friendsList.add(friend);
                    }
                    Log.d("HomeFragment", "Total friends loaded: " + friendsList.size());
                    FriendAdapter adapter = new FriendAdapter(getContext(), friendsList);
                    friendsRecyclerView.setAdapter(adapter);
                })
                .addOnFailureListener(e -> {
                    Log.e("HomeFragment", "Failed to load friends", e);
                    Toast.makeText(getContext(), "Failed to load friends", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onItemClick(int position) {
        Fragment targetFragment = null;
        switch (position) {
            case 0: targetFragment = new MapFragment(); break;
            case 1: targetFragment = new PlanFragment(); break;
            case 2: targetFragment = new RecordFragment(); break;
        }
        if (targetFragment != null && getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).loadFragment(targetFragment, true);
        }
    }

    private void toggleThemeButton(ImageView button, String theme) {
        if (selectedEmotionButton == button) {
            selectedEmotionButton.setSelected(false);
            selectedEmotionButton = null;
            ThemeHelper.saveSelectedTheme(requireContext(), "default");
        } else {
            if (selectedEmotionButton != null) {
                selectedEmotionButton.setSelected(false);
            }
            selectedEmotionButton = button;
            selectedEmotionButton.setSelected(true);
            ThemeHelper.saveSelectedTheme(requireContext(), theme);
        }
        requireActivity().recreate();
    }

    private void applyTheme() {
        sliderBackgroundView.setBackgroundColor(ThemeHelper.getSliderContainerColor(requireContext()));
        greetingText.setTextColor(ThemeHelper.getText1Color(requireContext()));
        TextView feelTodayText = getView().findViewById(R.id.feelTodayText);
        feelTodayText.setTextColor(ThemeHelper.getText2Color(requireContext()));
    }

    private void applySavedTheme() {
        SharedPreferences prefs = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String selectedTheme = prefs.getString(SELECTED_THEME_KEY, "default");

        switch (selectedTheme) {
            case "happy":
                happyButton.setSelected(true);
                selectedEmotionButton = happyButton;
                break;
            case "sad":
                sadButton.setSelected(true);
                selectedEmotionButton = sadButton;
                break;
            case "chill":
                chillButton.setSelected(true);
                selectedEmotionButton = chillButton;
                break;
            case "angry":
                angryButton.setSelected(true);
                selectedEmotionButton = angryButton;
                break;
            case "sleepy":
                sleepyButton.setSelected(true);
                selectedEmotionButton = sleepyButton;
                break;
            default:
                selectedEmotionButton = null;
                break;
        }
    }

    private void loadUserProfile() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String photoUrl = documentSnapshot.getString("photoUrl");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(this).load(photoUrl).into(userIconButton);
                            userIconButton.setTag(photoUrl);
                        } else {
                            userIconButton.setImageResource(R.drawable.ic_account_icon);
                        }
                    }
                });
    }

    public void updateProfileUI(String displayName, String photoUrl) {
        greetingText.setText("Hello " + displayName + " 👋");
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(this)
                    .load(photoUrl)
                    .circleCrop() // Transform the uploaded photo into a circle
                    .into(userIconButton);
            userIconButton.setTag(photoUrl);
        } else {
            Glide.with(this)
                    .load(R.drawable.ic_account_icon)
                    .circleCrop() // Transform the default icon into a circle
                    .into(userIconButton);
        }
    }

    private void updateExistingFriendsPhotoUrl() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(currentUserId).collection("friends")
                .whereEqualTo("friendshipStatus", "accepted")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String friendEmail = doc.getString("email");

                        db.collection("users")
                                .whereEqualTo("email", friendEmail)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(friendSnapshot -> {
                                    if (!friendSnapshot.isEmpty()) {
                                        String photoUrl = friendSnapshot.getDocuments().get(0).getString("photoUrl");
                                        db.collection("users")
                                                .document(currentUserId)
                                                .collection("friends")
                                                .document(friendEmail)
                                                .update("photoUrl", photoUrl)
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.d("HomeFragment", "Updated photoUrl for " + friendEmail);
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e("HomeFragment", "Failed to update photoUrl for " + friendEmail, e);
                                                });
                                    }
                                });
                    }
                });
    }

    private void updateExistingFriendsActivityStatus() {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(currentUserId).collection("friends")
                .whereEqualTo("friendshipStatus", "accepted")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String friendEmail = doc.getString("email");

                        db.collection("users")
                                .whereEqualTo("email", friendEmail)
                                .limit(1)
                                .get()
                                .addOnSuccessListener(friendSnapshot -> {
                                    if (!friendSnapshot.isEmpty()) {
                                        String activityStatus = friendSnapshot.getDocuments().get(0).getString("activityStatus");
                                        Log.d("HomeFragment", "Updating activityStatus for " + friendEmail + ": " + activityStatus);

                                        db.collection("users")
                                                .document(currentUserId)
                                                .collection("friends")
                                                .document(friendEmail)
                                                .update("activityStatus", activityStatus)
                                                .addOnSuccessListener(aVoid -> {
                                                    Log.d("HomeFragment", "Updated activityStatus for " + friendEmail);
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e("HomeFragment", "Failed to update activityStatus for " + friendEmail, e);
                                                });
                                    }
                                });
                    }
                });
    }
}