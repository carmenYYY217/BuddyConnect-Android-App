package edu.cuhk.csci3310.buddyconnect.Record_scheduler;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.MaterialCalendarView;

import org.threeten.bp.Instant;
import org.threeten.bp.LocalDate;
import org.threeten.bp.ZoneId;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import edu.cuhk.csci3310.buddyconnect.R;
import edu.cuhk.csci3310.buddyconnect.friendRequest.User;

public class SchedulerDialogFragment extends DialogFragment implements TaskSelectionListener {
    private RecyclerView taskRecyclerView;
    private MaterialCalendarView calendarView;
    private TaskAdapter taskAdapter;
    private CalendarDay selectedDate;
    private boolean isEditMode = false;
    private boolean isDeleteMode = false;
    private ImageButton editButton;
    private ImageButton deleteButton;
    private ImageButton closeButton;
    private ImageButton friendInviteButton;
    private LinearLayout deleteOptions;
    private TextView deleteTextView;

    private CollectionReference tasksRef;
    private ListenerRegistration tasksListener;
    private List<Task> allTasks = new ArrayList<>();
    private String schedulerName;
    private String schedulerId;
    private String userId;
    private String ownerId;
    private FirebaseFirestore db;
    private SchedulerFriendManager friendManager;

    private LinearLayout floatingMenu;
    private boolean isMenuVisible = false;
    private ImageButton tasksButton, eventsButton, sessionsButton, holidaysButton;

    public static SchedulerDialogFragment newInstance(String schedulerId, String schedulerName) {
        SchedulerDialogFragment fragment = new SchedulerDialogFragment();
        Bundle args = new Bundle();
        args.putString("schedulerId", schedulerId);
        args.putString("schedulerName", schedulerName);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            schedulerId = getArguments().getString("schedulerId");
            schedulerName = getArguments().getString("schedulerName");
        }

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(getContext(), "User not authenticated", Toast.LENGTH_SHORT).show();
            dismiss();
            return;
        }
        if (schedulerId != null) {
            tasksRef = db.collection("schedulers").document(schedulerId).collection("tasks");
            db.collection("schedulers").document(schedulerId).get()
                    .addOnSuccessListener(doc -> ownerId = doc.getString("ownerId"));
        } else {
            Toast.makeText(getContext(), "Scheduler ID not provided", Toast.LENGTH_SHORT).show();
            dismiss();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            DisplayMetrics displayMetrics = new DisplayMetrics();
            getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;
            int dialogWidth = (int) (screenWidth * 0.9);
            getDialog().getWindow().setLayout(dialogWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_scheduler, container, false);

        taskRecyclerView = view.findViewById(R.id.taskRecyclerView);
        calendarView = view.findViewById(R.id.calendarView);
        editButton = view.findViewById(R.id.editButton);
        deleteButton = view.findViewById(R.id.deleteButton);
        closeButton = view.findViewById(R.id.closeButton);
        deleteOptions = view.findViewById(R.id.deleteOptions);
        deleteTextView = view.findViewById(R.id.deleteTextView);
        friendInviteButton = view.findViewById(R.id.friend_invite_button);

        floatingMenu = view.findViewById(R.id.floatingMenu);
        ImageButton addIconButton = view.findViewById(R.id.addIconButton);
        tasksButton = view.findViewById(R.id.tasksButton);
        eventsButton = view.findViewById(R.id.eventsButton);
        sessionsButton = view.findViewById(R.id.sessionsButton);
        holidaysButton = view.findViewById(R.id.holidaysButton);

        friendManager = new SchedulerFriendManager(getContext(), schedulerId);
        taskRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        taskAdapter = new TaskAdapter(new ArrayList<>(), this, this, userId);
        taskRecyclerView.setAdapter(taskAdapter);

        calendarView.setSelectionMode(MaterialCalendarView.SELECTION_MODE_SINGLE);
        calendarView.setShowOtherDates(MaterialCalendarView.SHOW_OUT_OF_RANGE);

        addIconButton.setOnClickListener(v -> {
            if (isMenuVisible) {
                hideMenu();
            } else {
                showMenu();
            }
        });

        tasksButton.setOnClickListener(v -> showAddDialog("Task"));
        eventsButton.setOnClickListener(v -> showAddDialog("Event"));
        sessionsButton.setOnClickListener(v -> showAddDialog("Session"));
        holidaysButton.setOnClickListener(v -> showAddDialog("Holiday"));

        friendInviteButton.setOnClickListener(v -> {
            List<User> friends = new ArrayList<>();
            db.collection("users").document(userId).collection("friends")
                    .whereEqualTo("status", "accepted")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (var doc : querySnapshot) {
                            friends.add(doc.toObject(User.class));
                        }
                        friendManager.showFriendInviteMenu(friendInviteButton, friends);
                    });
        });

        calendarView.setOnDateChangedListener((widget, date, selected) -> {
            selectedDate = date;
            long timestamp = localDateToTimestamp(date.getDate());
            Log.d("SchedulerDialog", "Selected date: " + date.toString() + ", Timestamp: " + timestamp);
            updateTaskList(timestamp);
            widget.setDateTextAppearance(R.style.CustomCalendarDateTextAppearance);
        });

        CalendarDay today = CalendarDay.today();
        calendarView.setCurrentDate(today);
        calendarView.setDateSelected(today, true);
        selectedDate = today;

        loadTasksFromFirestore();
        updateTaskList(localDateToTimestamp(today.getDate()));
        updateEventSpots();
        setupTaskActions();

        closeButton.setOnClickListener(v -> dismiss());

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        friendManager.dismissPopup();
        if (tasksListener != null) {
            tasksListener.remove();
        }
    }

    private void showMenu() {
        floatingMenu.setVisibility(View.VISIBLE);
        animateButton(holidaysButton, 300);
        animateButton(sessionsButton, 200);
        animateButton(eventsButton, 100);
        animateButton(tasksButton, 0);
        isMenuVisible = true;
    }

    private void hideMenu() {
        animateButton(tasksButton, 300, true);
        animateButton(eventsButton, 200, true);
        animateButton(sessionsButton, 100, true);
        animateButton(holidaysButton, 0, true, () -> floatingMenu.setVisibility(View.GONE));
        isMenuVisible = false;
    }

    private void animateButton(View button, int delay, boolean hide) {
        animateButton(button, delay, hide, null);
    }

    private void animateButton(View button, int delay, boolean hide, Runnable endAction) {
        button.animate()
                .alpha(hide ? 0f : 1f)
                .translationY(hide ? 20f : 0f)
                .setDuration(200)
                .setStartDelay(delay)
                .withEndAction(endAction)
                .start();
    }

    private void animateButton(View button, int delay) {
        animateButton(button, delay, false, null);
    }

    private void showAddDialog(String type) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(
                type.equals("Holiday") ? R.layout.dialog_add_holiday :
                        type.equals("Session") ? R.layout.dialog_add_session :
                                R.layout.dialog_add_task_event, null);
        builder.setView(dialogView);

        if (type.equals("Task") || type.equals("Event")) {
            EditText titleInput = dialogView.findViewById(R.id.titleInput);
            CheckBox allDayCheckBox = dialogView.findViewById(R.id.allDayCheckBox);
            TextView startTimeInput = dialogView.findViewById(R.id.startTimeInput);
            TextView endTimeInput = dialogView.findViewById(R.id.endTimeInput);

            allDayCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                startTimeInput.setEnabled(!isChecked);
                endTimeInput.setEnabled(!isChecked);
            });

            startTimeInput.setOnClickListener(v -> showTimePicker(startTimeInput));
            endTimeInput.setOnClickListener(v -> showTimePicker(endTimeInput));

            builder.setPositiveButton("Add", (dialog, which) -> {
                String title = titleInput.getText().toString().trim();
                boolean isAllDay = allDayCheckBox.isChecked();
                String startTime = startTimeInput.getText().toString();
                String endTime = endTimeInput.getText().toString();

                if (title.isEmpty() || (!isAllDay && (startTime.isEmpty() || endTime.isEmpty()))) {
                    Toast.makeText(getContext(), "Required fields missing", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isAllDay && !isValidTimeRange(startTime, endTime)) {
                    Toast.makeText(getContext(), "Invalid time range: Start must be before or equal to End and between 00:00 and 23:59", Toast.LENGTH_LONG).show();
                    return;
                }

                Task task = new Task();
                task.setType(type);
                task.setTitle(title);
                task.setAllDay(isAllDay);
                task.setStartTime(isAllDay ? "00:00" : startTime);
                task.setEndTime(isAllDay ? "23:59" : endTime);
                task.setStartDate(localDateToTimestamp(selectedDate.getDate()));
                task.setUserId(userId);
                task.setCreatorDisplayName(FirebaseAuth.getInstance().getCurrentUser().getDisplayName());

                checkActivityConflict(task, schedulerId, userId).thenAccept(hasConflict -> {
                    if (!hasConflict) {
                        addTaskToFirestore(task);
                    } else {
                        Toast.makeText(getContext(), "Conflict with your existing activities", Toast.LENGTH_LONG).show();
                    }
                });
            });
        } else if (type.equals("Session")) {
            EditText titleInput = dialogView.findViewById(R.id.titleInput);
            Spinner sessionTypeSpinner = dialogView.findViewById(R.id.sessionTypeSpinner);
            CheckBox[] weekdays = {
                    dialogView.findViewById(R.id.monCheckBox),
                    dialogView.findViewById(R.id.tueCheckBox),
                    dialogView.findViewById(R.id.wedCheckBox),
                    dialogView.findViewById(R.id.thuCheckBox),
                    dialogView.findViewById(R.id.friCheckBox),
                    dialogView.findViewById(R.id.satCheckBox),
                    dialogView.findViewById(R.id.sunCheckBox)
            };
            TextView startTimeInput = dialogView.findViewById(R.id.startTimeInput);
            TextView endTimeInput = dialogView.findViewById(R.id.endTimeInput);
            TextView startDateInput = dialogView.findViewById(R.id.startDateInput);
            TextView endDateInput = dialogView.findViewById(R.id.endDateInput);

            startTimeInput.setOnClickListener(v -> showTimePicker(startTimeInput));
            endTimeInput.setOnClickListener(v -> showTimePicker(endTimeInput));
            startDateInput.setOnClickListener(v -> showDatePicker(startDateInput));
            endDateInput.setOnClickListener(v -> showDatePicker(endDateInput));

            builder.setPositiveButton("Add", (dialog, which) -> {
                String title = titleInput.getText().toString().trim();
                String sessionType = sessionTypeSpinner.getSelectedItem().toString();
                List<String> selectedWeekdays = new ArrayList<>();
                String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
                for (int i = 0; i < weekdays.length; i++) {
                    if (weekdays[i].isChecked()) selectedWeekdays.add(days[i]);
                }
                String startTime = startTimeInput.getText().toString();
                String endTime = endTimeInput.getText().toString();
                String startDateStr = startDateInput.getText().toString();
                String endDateStr = endDateInput.getText().toString();

                if (title.isEmpty() || selectedWeekdays.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || startDateStr.isEmpty() || endDateStr.isEmpty()) {
                    Toast.makeText(getContext(), "All fields required", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isValidTimeRange(startTime, endTime)) {
                    Toast.makeText(getContext(), "Invalid time range: Start must be before or equal to End and between 00:00 and 23:59", Toast.LENGTH_LONG).show();
                    return;
                }

                if (!isValidDateRange(startDateStr, endDateStr)) {
                    Toast.makeText(getContext(), "Invalid date range: Start date must be before End date", Toast.LENGTH_LONG).show();
                    return;
                }

                Task task = new Task();
                task.setType(type);
                task.setTitle(title);
                task.setSessionType(sessionType);
                task.setWeekdays(selectedWeekdays);
                task.setStartTime(startTime);
                task.setEndTime(endTime);
                task.setStartDate(parseDate(startDateStr).getTime());
                task.setEndDate(parseDate(endDateStr).getTime());
                task.setUserId(userId);
                task.setCreatorDisplayName(FirebaseAuth.getInstance().getCurrentUser().getDisplayName());

                checkActivityConflict(task, schedulerId, userId).thenAccept(hasConflict -> {
                    if (!hasConflict) {
                        addTaskToFirestore(task);
                    } else {
                        Toast.makeText(getContext(), "Conflict with your existing sessions", Toast.LENGTH_LONG).show();
                    }
                });
            });
        } else if (type.equals("Holiday")) {
            TextView startDateInput = dialogView.findViewById(R.id.startDateInput);
            TextView endDateInput = dialogView.findViewById(R.id.endDateInput);

            startDateInput.setOnClickListener(v -> showDatePicker(startDateInput));
            endDateInput.setOnClickListener(v -> showDatePicker(endDateInput));

            builder.setPositiveButton("Add", (dialog, which) -> {
                String startDateStr = startDateInput.getText().toString();
                String endDateStr = endDateInput.getText().toString();

                if (startDateStr.isEmpty() || endDateStr.isEmpty()) {
                    Toast.makeText(getContext(), "Dates required", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isValidDateRange(startDateStr, endDateStr)) {
                    Toast.makeText(getContext(), "Invalid date range: Start date must be before End date", Toast.LENGTH_LONG).show();
                    return;
                }

                Task task = new Task();
                task.setType(type);
                task.setStartDate(parseDate(startDateStr).getTime());
                task.setEndDate(parseDate(endDateStr).getTime());
                task.setUserId(userId);
                task.setCreatorDisplayName(FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
                addTaskToFirestore(task);
            });
        }

        builder.setNegativeButton("Cancel", null);
        builder.show();
        hideMenu();
    }

    private void showTimePicker(TextView timeInput) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        TimePickerDialog timePickerDialog = new TimePickerDialog(getContext(),
                (view, hourOfDay, minuteOfHour) -> {
                    String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minuteOfHour);
                    timeInput.setText(time);
                }, hour, minute, true);
        timePickerDialog.show();
    }

    private void showDatePicker(TextView dateInput) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay);
                    dateInput.setText(date);
                }, year, month, day);
        datePickerDialog.show();
    }

    private boolean isValidTimeRange(String startTime, String endTime) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Date start = sdf.parse(startTime);
            Date end = sdf.parse(endTime);
            Date midnightStart = sdf.parse("00:00");
            Date midnightEnd = sdf.parse("23:59");

            return start.compareTo(midnightStart) >= 0 && start.compareTo(midnightEnd) <= 0 &&
                    end.compareTo(midnightStart) >= 0 && end.compareTo(midnightEnd) <= 0 &&
                    start.compareTo(end) <= 0;
        } catch (ParseException e) {
            return false;
        }
    }

    private boolean isValidDateRange(String startDateStr, String endDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date start = sdf.parse(startDateStr);
            Date end = sdf.parse(endDateStr);
            return start.compareTo(end) <= 0;
        } catch (ParseException e) {
            return false;
        }
    }

    private Date parseDate(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            return new Date();
        }
    }

    private CompletableFuture<Boolean> checkActivityConflict(Task newTask, String schedulerId, String userId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        long startDate = newTask.getStartDate();
        long endDate = newTask.getEndDate() != 0 ? newTask.getEndDate() : startDate;

        db.collection("schedulers").document(schedulerId).collection("tasks")
                .whereEqualTo("userId", userId) // Check only tasks by the same user
                .get()
                .addOnSuccessListener(query -> {
                    for (var doc : query) {
                        Task existingTask = doc.toObject(Task.class);
                        if (existingTask.getId() != null && newTask.getId() != null && existingTask.getId().equals(newTask.getId())) continue; // Skip same task
                        long existingStart = existingTask.getStartDate();
                        long existingEnd = existingTask.getEndDate() != 0 ? existingTask.getEndDate() : existingStart;

                        // Check date overlap
                        if (startDate <= existingEnd && endDate >= existingStart) {
                            if (!newTask.getType().equals("Holiday") && !existingTask.getType().equals("Holiday")) {
                                // Check time overlap for non-holidays
                                if (timesOverlap(existingTask.getStartTime(), existingTask.getEndTime(),
                                        newTask.getStartTime(), newTask.getEndTime())) {
                                    future.complete(true); // Conflict found
                                    return;
                                }
                            }
                        }
                    }
                    future.complete(false); // No conflict
                })
                .addOnFailureListener(e -> future.completeExceptionally(e));
        return future;
    }

    private boolean timesOverlap(String start1, String end1, String start2, String end2) {
        // Allow overlap only at exact boundaries (start2 == end1 or end2 == start1)
        return (start1.compareTo(end2) < 0 && start2.compareTo(end1) < 0) &&
                !(start2.equals(end1) || end2.equals(start1));
    }

    private void addTaskToFirestore(Task task) {
        tasksRef.add(task)
                .addOnSuccessListener(documentReference -> {
                    task.setId(documentReference.getId());
                    allTasks.add(task);
                    Toast.makeText(getContext(), task.getType() + " added", Toast.LENGTH_SHORT).show();
                    updateTaskList(localDateToTimestamp(selectedDate.getDate()));
                    updateEventSpots();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error adding " + task.getType() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadTasksFromFirestore() {
        allTasks.clear();
        tasksListener = tasksRef.addSnapshotListener((queryDocumentSnapshots, e) -> {
            if (e != null) {
                Toast.makeText(getContext(), "Error loading tasks: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (queryDocumentSnapshots != null) {
                allTasks.clear();
                for (var doc : queryDocumentSnapshots) {
                    Task task = doc.toObject(Task.class);
                    task.setId(doc.getId());
                    allTasks.add(task);
                }
                Log.d("SchedulerDialog", "Loaded " + allTasks.size() + " tasks from Firestore");

                if (selectedDate != null) {
                    updateTaskList(localDateToTimestamp(selectedDate.getDate()));
                    updateEventSpots();
                }
            }
        });
    }

    private void updateTaskList(long date) {
        Map<String, Task> taskMap = new HashMap<>();
        LocalDate selectedLocalDate = Instant.ofEpochMilli(date).atZone(ZoneId.systemDefault()).toLocalDate();

        for (Task task : allTasks) {
            LocalDate taskStartDate = Instant.ofEpochMilli(task.getStartDate()).atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate taskEndDate = task.getEndDate() != 0 ?
                    Instant.ofEpochMilli(task.getEndDate()).atZone(ZoneId.systemDefault()).toLocalDate() : taskStartDate;

            if (task.getType().equals("Holiday")) {
                if (!selectedLocalDate.isBefore(taskStartDate) && !selectedLocalDate.isAfter(taskEndDate)) {
                    taskMap.put(task.getId(), task);
                }
            } else if (task.getType().equals("Session")) {
                List<LocalDate> sessionDates = generateSessionDates(task);
                if (sessionDates.contains(selectedLocalDate)) {
                    taskMap.put(task.getId(), task);
                }
            } else if (taskStartDate.equals(selectedLocalDate)) {
                taskMap.put(task.getId(), task);
            }
        }

        List<Task> tasksForDate = new ArrayList<>(taskMap.values());
        tasksForDate.sort((t1, t2) -> {
            if (t1.getType().equals("Holiday") && !t2.getType().equals("Holiday")) return -1;
            if (!t1.getType().equals("Holiday") && t2.getType().equals("Holiday")) return 1;
            if (t1.isAllDay() && !t2.isAllDay()) return -1;
            if (!t1.isAllDay() && t2.isAllDay()) return 1;
            if (t1.getStartTime() == null || t2.getStartTime() == null) return 0;
            return t1.getStartTime().compareTo(t2.getStartTime());
        });
        taskAdapter.updateTasks(tasksForDate);
    }

    private void updateEventSpots() {
        calendarView.removeDecorators();

        Set<CalendarDay> holidayDays = new HashSet<>();
        Set<CalendarDay> sessionDays = new HashSet<>();
        Set<CalendarDay> taskDaysOwner = new HashSet<>();
        Set<CalendarDay> taskDaysOther = new HashSet<>();
        Set<CalendarDay> eventDaysOwner = new HashSet<>();
        Set<CalendarDay> eventDaysOther = new HashSet<>();

        for (Task task : allTasks) {
            LocalDate taskStartDate = Instant.ofEpochMilli(task.getStartDate()).atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate taskEndDate = task.getEndDate() != 0 ?
                    Instant.ofEpochMilli(task.getEndDate()).atZone(ZoneId.systemDefault()).toLocalDate() : taskStartDate;
            boolean isOwner = userId.equals(task.getUserId());

            if (task.getType().equals("Holiday")) {
                LocalDate current = taskStartDate;
                while (!current.isAfter(taskEndDate)) {
                    holidayDays.add(CalendarDay.from(current));
                    current = current.plusDays(1);
                }
            } else if (task.getType().equals("Session")) {
                List<LocalDate> sessionDates = generateSessionDates(task);
                for (LocalDate date : sessionDates) {
                    sessionDays.add(CalendarDay.from(date));
                }
            } else if (task.getType().equals("Task")) {
                (isOwner ? taskDaysOwner : taskDaysOther).add(CalendarDay.from(taskStartDate));
            } else if (task.getType().equals("Event")) {
                (isOwner ? eventDaysOwner : eventDaysOther).add(CalendarDay.from(taskStartDate));
            }
        }

        List<EventDecorator> decorators = new ArrayList<>();
        if (!holidayDays.isEmpty()) decorators.add(new EventDecorator(Color.GREEN, holidayDays, 0f, true));
        if (!sessionDays.isEmpty()) decorators.add(new EventDecorator(Color.BLUE, sessionDays, 0f, true));
        if (!taskDaysOwner.isEmpty()) decorators.add(new EventDecorator(Color.RED, taskDaysOwner, 0f, true));
        if (!taskDaysOther.isEmpty()) decorators.add(new EventDecorator(Color.RED, taskDaysOther, 0f, false));
        if (!eventDaysOwner.isEmpty()) decorators.add(new EventDecorator(Color.MAGENTA, eventDaysOwner, 0f, true));
        if (!eventDaysOther.isEmpty()) decorators.add(new EventDecorator(Color.MAGENTA, eventDaysOther, 0f, false));

        int totalDecorators = decorators.size();
        for (int i = 0; i < totalDecorators; i++) {
            float position = 0.2f + (0.6f * i) / (totalDecorators > 1 ? totalDecorators - 1 : 1);
            EventDecorator decorator = decorators.get(i);
            decorator.setPosition(position);
            calendarView.addDecorator(decorator);
        }
    }

    private List<LocalDate> generateSessionDates(Task task) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate start = Instant.ofEpochMilli(task.getStartDate()).atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate end = Instant.ofEpochMilli(task.getEndDate()).atZone(ZoneId.systemDefault()).toLocalDate();

        while (!start.isAfter(end)) {
            String dayOfWeek = start.getDayOfWeek().toString().substring(0, 1).toUpperCase() + start.getDayOfWeek().toString().substring(1).toLowerCase();
            if (task.getWeekdays() != null && task.getWeekdays().contains(dayOfWeek)) {
                dates.add(start);
            }
            start = start.plusDays(1);
        }
        return dates;
    }

    private long localDateToTimestamp(LocalDate date) {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private void setupTaskActions() {
        editButton.setOnClickListener(v -> {
            if (isDeleteMode) exitDeleteMode();
            toggleEditMode();
        });

        deleteButton.setOnClickListener(v -> {
            if (isEditMode) exitEditMode();
            toggleDeleteMode();
        });

        deleteTextView.setOnClickListener(v -> {
            int selectedCount = taskAdapter.getSelectedTasks().size();
            int totalCount = taskAdapter.getItemCount();
            if (selectedCount == 0 || selectedCount == totalCount) {
                deleteAllTasksForDay();
            } else {
                deleteSelectedTasks();
            }
        });
    }

    private void toggleEditMode() {
        isEditMode = !isEditMode;
        if (isEditMode) {
            editButton.setBackgroundColor(Color.GREEN);
            taskAdapter.setEditMode(true);
        } else {
            exitEditMode();
        }
    }

    private void exitEditMode() {
        isEditMode = false;
        editButton.setBackgroundColor(Color.TRANSPARENT);
        taskAdapter.setEditMode(false);
    }

    private void toggleDeleteMode() {
        if (selectedDate == null) {
            Toast.makeText(getContext(), "Please select a date first", Toast.LENGTH_SHORT).show();
            return;
        }

        isDeleteMode = !isDeleteMode;
        if (isDeleteMode) {
            deleteButton.setBackgroundColor(Color.RED);
            showDeleteOptions();
            taskAdapter.setDeleteMode(true);
        } else {
            exitDeleteMode();
        }
    }

    private void exitDeleteMode() {
        isDeleteMode = false;
        deleteButton.setBackgroundColor(Color.TRANSPARENT);
        hideDeleteOptions();
        taskAdapter.clearSelections();
        taskAdapter.setDeleteMode(false);
    }

    private void showDeleteOptions() {
        deleteOptions.setVisibility(View.VISIBLE);
        updateDeleteText();
    }

    private void hideDeleteOptions() {
        deleteOptions.setVisibility(View.GONE);
    }

    private void updateDeleteText() {
        int selectedCount = taskAdapter.getSelectedTasks().size();
        int totalCount = taskAdapter.getItemCount();

        if (selectedCount == 0 || selectedCount == totalCount) {
            deleteTextView.setText("Delete ALL");
        } else {
            String taskType = "item";
            Set<Task> selectedTasks = taskAdapter.getSelectedTasks();
            if (!selectedTasks.isEmpty()) {
                Task firstSelectedTask = selectedTasks.iterator().next();
                taskType = firstSelectedTask.getType().toLowerCase();
                if (taskType.equals("session") && firstSelectedTask.getSessionType() != null) {
                    taskType = firstSelectedTask.getSessionType().toLowerCase();
                }
            }
            String label = selectedCount == 1 ? taskType : taskType + "s";
            deleteTextView.setText("Delete " + selectedCount + " " + label);
        }
    }

    @Override
    public void onTaskSelectionChanged(int selectedCount) {
        updateDeleteText();
    }

    private void deleteSelectedTasks() {
        Set<Task> selectedTasks = taskAdapter.getSelectedTasks();
        if (selectedTasks.isEmpty()) return;

        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete " + selectedTasks.size() + " selected tasks?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    for (Task task : selectedTasks) {
                        tasksRef.document(task.getId()).delete()
                                .addOnSuccessListener(aVoid -> allTasks.remove(task));
                    }
                    if (selectedDate != null) {
                        updateTaskList(localDateToTimestamp(selectedDate.getDate()));
                        updateEventSpots();
                    }
                    exitDeleteMode();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteAllTasksForDay() {
        new AlertDialog.Builder(getContext())
                .setTitle("Confirm Deletion")
                .setMessage("Are you sure you want to delete all your tasks for this day?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    if (selectedDate != null) {
                        long timestamp = localDateToTimestamp(selectedDate.getDate());
                        LocalDate selectedLocalDate = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate();
                        List<Task> tasksToDelete = new ArrayList<>();

                        for (Task task : allTasks) {
                            if (!userId.equals(task.getUserId())) continue; // Only delete own tasks
                            LocalDate taskStartDate = Instant.ofEpochMilli(task.getStartDate()).atZone(ZoneId.systemDefault()).toLocalDate();
                            LocalDate taskEndDate = task.getEndDate() != 0 ?
                                    Instant.ofEpochMilli(task.getEndDate()).atZone(ZoneId.systemDefault()).toLocalDate() : taskStartDate;

                            if (task.getType().equals("Holiday")) {
                                if (!selectedLocalDate.isBefore(taskStartDate) && !selectedLocalDate.isAfter(taskEndDate)) {
                                    tasksToDelete.add(task);
                                }
                            } else if (task.getType().equals("Session")) {
                                List<LocalDate> sessionDates = generateSessionDates(task);
                                if (sessionDates.contains(selectedLocalDate)) {
                                    tasksToDelete.add(task);
                                }
                            } else if (taskStartDate.equals(selectedLocalDate)) {
                                tasksToDelete.add(task);
                            }
                        }

                        for (Task task : tasksToDelete) {
                            tasksRef.document(task.getId()).delete()
                                    .addOnSuccessListener(aVoid -> allTasks.remove(task));
                        }

                        updateTaskList(timestamp);
                        updateEventSpots();
                        exitDeleteMode();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onTaskSelected(Task task) {
        if (isEditMode) {
            showEditDialog(task);
            exitEditMode();
        }
    }

    protected void showEditDialog(Task task) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(
                task.getType().equals("Holiday") ? R.layout.dialog_add_holiday :
                        task.getType().equals("Session") ? R.layout.dialog_add_session :
                                R.layout.dialog_add_task_event, null);
        builder.setView(dialogView);

        if (task.getType().equals("Task") || task.getType().equals("Event")) {
            EditText titleInput = dialogView.findViewById(R.id.titleInput);
            CheckBox allDayCheckBox = dialogView.findViewById(R.id.allDayCheckBox);
            TextView startTimeInput = dialogView.findViewById(R.id.startTimeInput);
            TextView endTimeInput = dialogView.findViewById(R.id.endTimeInput);

            titleInput.setText(task.getTitle());
            allDayCheckBox.setChecked(task.isAllDay());
            startTimeInput.setText(task.getStartTime());
            endTimeInput.setText(task.getEndTime());

            allDayCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                startTimeInput.setEnabled(!isChecked);
                endTimeInput.setEnabled(!isChecked);
            });

            startTimeInput.setOnClickListener(v -> showTimePicker(startTimeInput));
            endTimeInput.setOnClickListener(v -> showTimePicker(endTimeInput));

            builder.setPositiveButton("Update", (dialog, which) -> {
                String title = titleInput.getText().toString().trim();
                boolean isAllDay = allDayCheckBox.isChecked();
                String startTime = startTimeInput.getText().toString();
                String endTime = endTimeInput.getText().toString();

                if (title.isEmpty() || (!isAllDay && (startTime.isEmpty() || endTime.isEmpty()))) {
                    Toast.makeText(getContext(), "Required fields missing", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isAllDay && !isValidTimeRange(startTime, endTime)) {
                    Toast.makeText(getContext(), "Invalid time range: Start must be before or equal to End and between 00:00 and 23:59", Toast.LENGTH_LONG).show();
                    return;
                }

                task.setTitle(title);
                task.setAllDay(isAllDay);
                task.setStartTime(isAllDay ? "00:00" : startTime);
                task.setEndTime(isAllDay ? "23:59" : endTime);

                checkActivityConflict(task, schedulerId, userId).thenAccept(hasConflict -> {
                    if (!hasConflict) {
                        updateTaskInFirestore(task);
                    } else {
                        Toast.makeText(getContext(), "Conflict with your existing activities", Toast.LENGTH_LONG).show();
                    }
                });
            });
        } else if (task.getType().equals("Session")) {
            EditText titleInput = dialogView.findViewById(R.id.titleInput);
            Spinner sessionTypeSpinner = dialogView.findViewById(R.id.sessionTypeSpinner);
            CheckBox[] weekdays = {
                    dialogView.findViewById(R.id.monCheckBox),
                    dialogView.findViewById(R.id.tueCheckBox),
                    dialogView.findViewById(R.id.wedCheckBox),
                    dialogView.findViewById(R.id.thuCheckBox),
                    dialogView.findViewById(R.id.friCheckBox),
                    dialogView.findViewById(R.id.satCheckBox),
                    dialogView.findViewById(R.id.sunCheckBox)
            };
            TextView startTimeInput = dialogView.findViewById(R.id.startTimeInput);
            TextView endTimeInput = dialogView.findViewById(R.id.endTimeInput);
            TextView startDateInput = dialogView.findViewById(R.id.startDateInput);
            TextView endDateInput = dialogView.findViewById(R.id.endDateInput);

            titleInput.setText(task.getTitle());
            startTimeInput.setText(task.getStartTime());
            endTimeInput.setText(task.getEndTime());
            startDateInput.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(task.getStartDate())));
            endDateInput.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(task.getEndDate())));

            String[] sessionTypes = getResources().getStringArray(R.array.session_types);
            for (int i = 0; i < sessionTypes.length; i++) {
                if (sessionTypes[i].equals(task.getSessionType())) {
                    sessionTypeSpinner.setSelection(i);
                    break;
                }
            }

            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
            for (int i = 0; i < days.length; i++) {
                weekdays[i].setChecked(task.getWeekdays() != null && task.getWeekdays().contains(days[i]));
            }

            startTimeInput.setOnClickListener(v -> showTimePicker(startTimeInput));
            endTimeInput.setOnClickListener(v -> showTimePicker(endTimeInput));
            startDateInput.setOnClickListener(v -> showDatePicker(startDateInput));
            endDateInput.setOnClickListener(v -> showDatePicker(endDateInput));

            builder.setPositiveButton("Update", (dialog, which) -> {
                String title = titleInput.getText().toString().trim();
                String sessionType = sessionTypeSpinner.getSelectedItem().toString();
                List<String> selectedWeekdays = new ArrayList<>();
                for (int i = 0; i < weekdays.length; i++) {
                    if (weekdays[i].isChecked()) selectedWeekdays.add(days[i]);
                }
                String startTime = startTimeInput.getText().toString();
                String endTime = endTimeInput.getText().toString();
                String startDateStr = startDateInput.getText().toString();
                String endDateStr = endDateInput.getText().toString();

                if (title.isEmpty() || selectedWeekdays.isEmpty() || startTime.isEmpty() || endTime.isEmpty() || startDateStr.isEmpty() || endDateStr.isEmpty()) {
                    Toast.makeText(getContext(), "All fields required", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isValidTimeRange(startTime, endTime)) {
                    Toast.makeText(getContext(), "Invalid time range: Start must be before or equal to End and between 00:00 and 23:59", Toast.LENGTH_LONG).show();
                    return;
                }

                if (!isValidDateRange(startDateStr, endDateStr)) {
                    Toast.makeText(getContext(), "Invalid date range: Start date must be before End date", Toast.LENGTH_LONG).show();
                    return;
                }

                task.setTitle(title);
                task.setSessionType(sessionType);
                task.setWeekdays(selectedWeekdays);
                task.setStartTime(startTime);
                task.setEndTime(endTime);
                task.setStartDate(parseDate(startDateStr).getTime());
                task.setEndDate(parseDate(endDateStr).getTime());

                checkActivityConflict(task, schedulerId, userId).thenAccept(hasConflict -> {
                    if (!hasConflict) {
                        updateTaskInFirestore(task);
                    } else {
                        Toast.makeText(getContext(), "Conflict with your existing sessions", Toast.LENGTH_LONG).show();
                    }
                });
            });
        } else if (task.getType().equals("Holiday")) {
            TextView startDateInput = dialogView.findViewById(R.id.startDateInput);
            TextView endDateInput = dialogView.findViewById(R.id.endDateInput);

            startDateInput.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(task.getStartDate())));
            endDateInput.setText(new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(task.getEndDate())));

            startDateInput.setOnClickListener(v -> showDatePicker(startDateInput));
            endDateInput.setOnClickListener(v -> showDatePicker(endDateInput));

            builder.setPositiveButton("Update", (dialog, which) -> {
                String startDateStr = startDateInput.getText().toString();
                String endDateStr = endDateInput.getText().toString();

                if (startDateStr.isEmpty() || endDateStr.isEmpty()) {
                    Toast.makeText(getContext(), "Dates required", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!isValidDateRange(startDateStr, endDateStr)) {
                    Toast.makeText(getContext(), "Invalid date range: Start date must be before End date", Toast.LENGTH_LONG).show();
                    return;
                }

                task.setStartDate(parseDate(startDateStr).getTime());
                task.setEndDate(parseDate(endDateStr).getTime());
                updateTaskInFirestore(task);
            });
        }

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateTaskInFirestore(Task task) {
        tasksRef.document(task.getId()).set(task)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), task.getType() + " updated", Toast.LENGTH_SHORT).show();
                    updateTaskList(localDateToTimestamp(selectedDate.getDate()));
                    updateEventSpots();
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Error updating " + task.getType() + ": " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}