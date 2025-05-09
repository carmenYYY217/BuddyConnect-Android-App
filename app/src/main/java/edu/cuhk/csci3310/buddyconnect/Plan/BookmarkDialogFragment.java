package edu.cuhk.csci3310.buddyconnect.Plan;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import edu.cuhk.csci3310.buddyconnect.R;

public class BookmarkDialogFragment extends DialogFragment {
    private static final String ARG_BOOKMARK = "bookmark";
    private static final String ARG_POSITION = "position";

    private ImageView imageView;
    private Uri selectedImageUri;
    private ActivityResultLauncher<String> imagePickerLauncher;
    private Bookmark bookmark;
    private int position = -1; // -1 indicates a new bookmark
    private EditText dateEditText;
    private EditText timeEditText; // Add reference to timeEditText

    public static BookmarkDialogFragment newInstance(Bookmark bookmark, int position) {
        BookmarkDialogFragment fragment = new BookmarkDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_BOOKMARK, bookmark);
        args.putInt(ARG_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            bookmark = getArguments().getParcelable(ARG_BOOKMARK);
            position = getArguments().getInt(ARG_POSITION);
        }
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                selectedImageUri = uri;
                imageView.setImageURI(uri);
            }
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_bookmark, null);

        EditText nameEditText = view.findViewById(R.id.nameEditText);
        EditText addressEditText = view.findViewById(R.id.addressEditText);
        EditText commentsEditText = view.findViewById(R.id.commentsEditText);
        dateEditText = view.findViewById(R.id.dateEditText);
        timeEditText = view.findViewById(R.id.timeEditText); // Initialize timeEditText
        EditText peopleEditText = view.findViewById(R.id.peopleEditText);
        RatingBar ratingBar = view.findViewById(R.id.ratingBar);
        imageView = view.findViewById(R.id.imageView);
        Button selectImageButton = view.findViewById(R.id.selectImageButton);

        // Set up the date picker
        setupDatePicker();

        // Set up the time picker
        setupTimePicker();

        // Pre-fill fields if editing an existing bookmark
        if (bookmark != null) {
            nameEditText.setText(bookmark.getName());
            addressEditText.setText(bookmark.getAddress());
            commentsEditText.setText(bookmark.getComments());
            dateEditText.setText(bookmark.getDate());
            timeEditText.setText(bookmark.getTime());
            peopleEditText.setText(String.valueOf(bookmark.getNumberOfPeople()));
            ratingBar.setRating(bookmark.getRating());
            if (bookmark.getImageUri() != null && !bookmark.getImageUri().isEmpty()) {
                Glide.with(this).load(bookmark.getImageUriAsUri()).into(imageView);
                selectedImageUri = bookmark.getImageUriAsUri();
            }
        }

        selectImageButton.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(getActivity())
                .setView(view)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = nameEditText.getText().toString().trim();
                    String address = addressEditText.getText().toString().trim();
                    String comments = commentsEditText.getText().toString().trim();
                    String date = dateEditText.getText().toString().trim();
                    String time = timeEditText.getText().toString().trim();
                    String peopleStr = peopleEditText.getText().toString().trim();
                    float rating = ratingBar.getRating();
                    Uri imageUri = selectedImageUri;

                    // Require name field to be non-empty
                    if (name.isEmpty()) {
                        Toast.makeText(getActivity(), "Name is required to save a bookmark", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Parse number of people (default to 0 if empty or invalid)
                    int numberOfPeople = 0;
                    try {
                        numberOfPeople = peopleStr.isEmpty() ? 0 : Integer.parseInt(peopleStr);
                    } catch (NumberFormatException e) {
                        Toast.makeText(getActivity(), "Invalid number of people", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Create the bookmark
                    Bookmark updatedBookmark = new Bookmark(name, address, comments, date, time, numberOfPeople, rating, imageUri);

                    // Pass it to PlanFragment
                    if (getParentFragment() instanceof PlanFragment) {
                        PlanFragment planFragment = (PlanFragment) getParentFragment();
                        if (position == -1) {
                            // New bookmark
                            planFragment.onBookmarkCreated(updatedBookmark);
                        } else {
                            // Edit existing bookmark
                            planFragment.onBookmarkUpdated(updatedBookmark, position);
                        }
                    } else {
                        Log.e("BookmarkDialog", "Parent fragment is not PlanFragment");
                    }
                })
                .setNegativeButton("Cancel", null);

        return builder.create();
    }

    // Method to set up the date picker
    private void setupDatePicker() {
        dateEditText.setOnClickListener(v -> {
            // Get today's date
            final Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            // Create a DatePickerDialog
            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    requireContext(),
                    (view, selectedYear, selectedMonth, selectedDay) -> {
                        // Format the selected date as dd/MM/yyyy
                        Calendar selectedDate = Calendar.getInstance();
                        selectedDate.set(selectedYear, selectedMonth, selectedDay);
                        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        String formattedDate = dateFormat.format(selectedDate.getTime());
                        dateEditText.setText(formattedDate);
                    },
                    year, month, day
            );

            // Set the minimum date to today (disable past dates)
            datePickerDialog.getDatePicker().setMinDate(calendar.getTimeInMillis());

            // Show the date picker
            datePickerDialog.show();
        });
    }

    // Method to set up the time picker
    private void setupTimePicker() {
        timeEditText.setOnClickListener(v -> {
            // Get current time
            final Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            // Create a TimePickerDialog
            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    requireContext(),
                    (view, selectedHour, selectedMinute) -> {
                        // Format the selected time as HH:mm
                        String formattedTime = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute);
                        timeEditText.setText(formattedTime);
                    },
                    hour, minute, true // true for 24-hour format
            );

            // Show the time picker
            timePickerDialog.show();
        });
    }
}