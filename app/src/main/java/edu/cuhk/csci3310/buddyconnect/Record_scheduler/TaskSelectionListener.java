package edu.cuhk.csci3310.buddyconnect.Record_scheduler;

public interface TaskSelectionListener {
    void onTaskSelectionChanged(int selectedCount);
    void onTaskSelected(Task task);
}