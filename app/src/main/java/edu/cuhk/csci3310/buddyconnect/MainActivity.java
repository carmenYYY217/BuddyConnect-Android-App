package edu.cuhk.csci3310.buddyconnect;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import com.nafis.bottomnavigation.NafisBottomNavigation;

import edu.cuhk.csci3310.buddyconnect.chatbot.ChatBotFragment;
import edu.cuhk.csci3310.buddyconnect.ui.FriendsFragment;
import edu.cuhk.csci3310.buddyconnect.ui.HomeFragment;
import edu.cuhk.csci3310.buddyconnect.ui.ProfileUpdateDialog;
import edu.cuhk.csci3310.buddyconnect.ui.SearchFragment;
import edu.cuhk.csci3310.buddyconnect.ui.SettingFragment;
import edu.cuhk.csci3310.buddyconnect.ui.UserProfileFragment;
import edu.cuhk.csci3310.buddyconnect.useraccount.AuthActivity;
import edu.cuhk.csci3310.buddyconnect.useraccount.FirebaseAuthManager;
import edu.cuhk.csci3310.buddyconnect.utils.ThemeHelper;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class MainActivity extends BaseActivity {
    private FirebaseAuthManager authManager;
    private NafisBottomNavigation bottomNavigation;
    private ProfileUpdateDialog currentDialog; // Track the active dialog
    private ActivityResultLauncher<String> imagePickerLauncher;
    private Function1<NafisBottomNavigation.Model, Unit> clickListener;
    private int selectedBottomNavId = 1; // Default to "Home" tab

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        authManager = new FirebaseAuthManager(this);
        if (!authManager.isUserSignedIn()) {
            Intent intent = new Intent(this, AuthActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        // Register image picker launcher in onCreate to avoid lifecycle issues
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null && currentDialog != null) {
                        currentDialog.setSelectedImageUri(uri);
                    }
                }
        );

        bottomNavigation = findViewById(R.id.bottomNavigationView);
        applyTheme();

        bottomNavigation.setClipToOutline(true);

        bottomNavigation.add(new NafisBottomNavigation.Model(1, R.drawable.ic_home));
        bottomNavigation.add(new NafisBottomNavigation.Model(2, R.drawable.ic_friend));
        bottomNavigation.add(new NafisBottomNavigation.Model(3, R.drawable.ic_search));
        bottomNavigation.add(new NafisBottomNavigation.Model(4, R.drawable.ic_chat));
        bottomNavigation.add(new NafisBottomNavigation.Model(5, R.drawable.ic_setting));

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment(), false);
            bottomNavigation.show(1, true);
            selectedBottomNavId = 1;
        }

        clickListener = model -> {
            Fragment fragment = null;
            switch (model.getId()) {
                case 1: fragment = new HomeFragment(); break;
                case 2: fragment = new FriendsFragment(); break;
                case 3: fragment = new SearchFragment(); break;
                case 4: fragment = new ChatBotFragment(); break;
                case 5: fragment = new SettingFragment(); break;
            }
            if (fragment != null) {
                loadFragment(fragment, true);
            }
            return null;
        };

        bottomNavigation.setOnClickMenuListener(clickListener);

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.frame_layout);
            if (currentFragment instanceof HomeFragment) {
                bottomNavigation.show(1, true);
                selectedBottomNavId = 1;
            } else if (currentFragment instanceof FriendsFragment) {
                bottomNavigation.show(2, true);
                selectedBottomNavId = 2;
            } else if (currentFragment instanceof SearchFragment) {
                bottomNavigation.show(3, true);
                selectedBottomNavId = 3;
            } else if (currentFragment instanceof ChatBotFragment) {
                bottomNavigation.show(4, true);
                selectedBottomNavId = 4;
            } else if (currentFragment instanceof SettingFragment) {
                bottomNavigation.show(5, true);
                selectedBottomNavId = 5;
            }
        });
    }

    public void applyTheme() {
        View mainBackground = findViewById(R.id.mainBackground);
        mainBackground.setBackgroundColor(ThemeHelper.getBackgroundColor(this));
        bottomNavigation.setBackgroundBottomColor(ThemeHelper.getBottomBarBackgroundColor(this));
        bottomNavigation.setBackgroundColor(ThemeHelper.getSliderContainerColor(this));
        bottomNavigation.setCircleColor(ThemeHelper.getBottomBarSelectedColor(this));
        bottomNavigation.setSelectedIconColor(ThemeHelper.getBottomBarIconSelectedColor(this));
        bottomNavigation.setDefaultIconColor(ThemeHelper.getBottomBarIconUnselectedColor(this));
    }

    public void loadFragment(Fragment fragment, boolean addToBackStack) {
        String tag = fragment.getClass().getSimpleName();
        if (addToBackStack) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, fragment, tag)
                    .addToBackStack(tag)
                    .commit();
        } else {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame_layout, fragment, tag)
                    .commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!authManager.isUserSignedIn()) {
            Intent intent = new Intent(this, AuthActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            if (!isFinishing() && !isChangingConfigurations()) {
                BuddyConnectApplication.clearChatHistory();
            }
            super.onDestroy();
        } catch (Exception e) {
            Log.e("MainActivity", "Error in onDestroy: " + e.getMessage());
            super.onDestroy();
        }
        currentDialog = null;
    }

    public void selectTabWithAction(int id) {
        bottomNavigation.show(id, true);
        selectedBottomNavId = id;
        if (clickListener != null) {
            clickListener.invoke(new NafisBottomNavigation.Model(id, 0));
        }
    }

    public ActivityResultLauncher<String> getImagePickerLauncher() {
        return imagePickerLauncher;
    }

    public void setCurrentDialog(ProfileUpdateDialog dialog) {
        this.currentDialog = dialog;
    }

    public void updateProfileUI(String displayName, String photoUrl) {
        Fragment homeFragment = getSupportFragmentManager().findFragmentByTag("HomeFragment");
        if (homeFragment instanceof HomeFragment) {
            ((HomeFragment) homeFragment).updateProfileUI(displayName, photoUrl);
        }
        Fragment profileFragment = getSupportFragmentManager().findFragmentByTag("UserProfileFragment");
        if (profileFragment instanceof UserProfileFragment) {
            ((UserProfileFragment) profileFragment).updateProfileUI(displayName, photoUrl);
        }
    }

    public void showProfileUpdateDialog(String currentDisplayName, String currentPhotoUrl) {
        currentDialog = new ProfileUpdateDialog(this, currentDisplayName, currentPhotoUrl, imagePickerLauncher);
        currentDialog.show();
    }


}