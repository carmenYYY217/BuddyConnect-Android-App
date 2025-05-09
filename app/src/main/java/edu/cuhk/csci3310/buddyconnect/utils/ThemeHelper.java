package edu.cuhk.csci3310.buddyconnect.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.core.content.ContextCompat;

import edu.cuhk.csci3310.buddyconnect.R;

public class ThemeHelper {
    private static final String PREFS_NAME = "AppPrefs";
    private static final String SELECTED_THEME_KEY = "selectedTheme";

    // Retrieve the currently selected theme
    public static String getSelectedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(SELECTED_THEME_KEY, "default");
    }

    // Save the selected theme
    public static void saveSelectedTheme(Context context, String theme) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(SELECTED_THEME_KEY, theme);
        editor.apply();
    }

    // Background color for the main layout
    public static int getBackgroundColor(Context context) {
        String theme = getSelectedTheme(context);
        switch (theme) {
            case "happy": return ContextCompat.getColor(context, R.color.happy_background);
            case "sad": return ContextCompat.getColor(context, R.color.sad_background);
            case "chill": return ContextCompat.getColor(context, R.color.chill_background);
            case "angry": return ContextCompat.getColor(context, R.color.angry_background);
            case "sleepy": return ContextCompat.getColor(context, R.color.sleepy_background);
            default: return ContextCompat.getColor(context, R.color.default_background); // Default fallback
        }
    }

    // Slider container color
    public static int getSliderContainerColor(Context context) {
        String theme = getSelectedTheme(context);
        switch (theme) {
            case "happy": return ContextCompat.getColor(context, R.color.happy_slider_container);
            case "sad": return ContextCompat.getColor(context, R.color.sad_slider_container);
            case "chill": return ContextCompat.getColor(context, R.color.chill_slider_container);
            case "angry": return ContextCompat.getColor(context, R.color.angry_slider_container);
            case "sleepy": return ContextCompat.getColor(context, R.color.sleepy_slider_container);
            default: return ContextCompat.getColor(context, R.color.default_slider_container); // Default fallback
        }
    }

    // Bottom navigation bar background color
    public static int getBottomBarBackgroundColor(Context context) {
        String theme = getSelectedTheme(context);
        switch (theme) {
            case "happy": return ContextCompat.getColor(context, R.color.happy_bottom_bar);
            case "sad": return ContextCompat.getColor(context, R.color.sad_bottom_bar);
            case "chill": return ContextCompat.getColor(context, R.color.chill_bottom_bar);
            case "angry": return ContextCompat.getColor(context, R.color.angry_bottom_bar);
            case "sleepy": return ContextCompat.getColor(context, R.color.sleepy_bottom_bar);
            default: return ContextCompat.getColor(context, R.color.default_bottom_bar); // Default fallback
        }
    }

    // Bottom navigation bar selected item color
    public static int getBottomBarSelectedColor(Context context) {
        String theme = getSelectedTheme(context);
        switch (theme) {
            case "happy": return ContextCompat.getColor(context, R.color.happy_bottom_bar_selected);
            case "sad": return ContextCompat.getColor(context, R.color.sad_bottom_bar_selected);
            case "chill": return ContextCompat.getColor(context, R.color.chill_bottom_bar_selected);
            case "angry": return ContextCompat.getColor(context, R.color.angry_bottom_bar_selected);
            case "sleepy": return ContextCompat.getColor(context, R.color.sleepy_bottom_bar_selected);
            default: return ContextCompat.getColor(context, R.color.default_bottom_bar_selected); // Default fallback
        }
    }

    // Bottom navigation bar icon selected color
    public static int getBottomBarIconSelectedColor(Context context) {
        String theme = getSelectedTheme(context);
        switch (theme) {
            case "happy": return ContextCompat.getColor(context, R.color.happy_bottom_bar_icon_selected);
            case "sad": return ContextCompat.getColor(context, R.color.sad_bottom_bar_icon_selected);
            case "chill": return ContextCompat.getColor(context, R.color.chill_bottom_bar_icon_selected);
            case "angry": return ContextCompat.getColor(context, R.color.angry_bottom_bar_icon_selected);
            case "sleepy": return ContextCompat.getColor(context, R.color.sleepy_bottom_bar_icon_selected);
            default: return ContextCompat.getColor(context, R.color.default_bottom_bar_icon_selected); // Default fallback
        }
    }

    // Bottom navigation bar icon unselected color
    public static int getBottomBarIconUnselectedColor(Context context) {
        String theme = getSelectedTheme(context);
        switch (theme) {
            case "happy": return ContextCompat.getColor(context, R.color.happy_bottom_bar_icon_unselected);
            case "sad": return ContextCompat.getColor(context, R.color.sad_bottom_bar_icon_unselected);
            case "chill": return ContextCompat.getColor(context, R.color.chill_bottom_bar_icon_unselected);
            case "angry": return ContextCompat.getColor(context, R.color.angry_bottom_bar_icon_unselected);
            case "sleepy": return ContextCompat.getColor(context, R.color.sleepy_bottom_bar_icon_unselected);
            default: return ContextCompat.getColor(context, R.color.default_bottom_bar_icon_unselected); // Default fallback
        }
    }

    public static int getText1Color(Context context) {
        String theme = getSelectedTheme(context);
        switch (theme) {
            case "happy": return ContextCompat.getColor(context, R.color.happy_text1);
            case "sad": return ContextCompat.getColor(context, R.color.sad_text1);
            case "chill": return ContextCompat.getColor(context, R.color.chill_text1);
            case "angry": return ContextCompat.getColor(context, R.color.angry_text1);
            case "sleepy": return ContextCompat.getColor(context, R.color.sleepy_text1);
            default: return ContextCompat.getColor(context, R.color.default_text1);
        }
    }

    public static int getText2Color(Context context) {
        String theme = getSelectedTheme(context);
        switch (theme) {
            case "happy": return ContextCompat.getColor(context, R.color.happy_text2);
            case "sad": return ContextCompat.getColor(context, R.color.sad_text2);
            case "chill": return ContextCompat.getColor(context, R.color.chill_text2);
            case "angry": return ContextCompat.getColor(context, R.color.angry_text2);
            case "sleepy": return ContextCompat.getColor(context, R.color.sleepy_text2);
            default: return ContextCompat.getColor(context, R.color.default_text2);
        }
    }

    public static int getText3Color(Context context) {
        String theme = getSelectedTheme(context);
        switch (theme) {
            case "happy": return ContextCompat.getColor(context, R.color.happy_text3);
            case "sad": return ContextCompat.getColor(context, R.color.sad_text3);
            case "chill": return ContextCompat.getColor(context, R.color.chill_text3);
            case "angry": return ContextCompat.getColor(context, R.color.angry_text3);
            case "sleepy": return ContextCompat.getColor(context, R.color.sleepy_text3);
            default: return ContextCompat.getColor(context, R.color.default_text3);
        }
    }

}