package edu.cuhk.csci3310.buddyconnect.Record_scheduler;

import java.util.List;

public class Task {
    private String id; // Firestore document ID
    private String type; // "Task", "Event", "Session", "Holiday"
    private String title; // Required for Tasks, Events, Sessions
    private String sessionType; // For Sessions: "work" or "class"
    private List<String> weekdays; // For Sessions: e.g., ["Monday", "Wednesday"]
    private String startTime; // Format: "HH:mm", null for Holidays
    private String endTime; // Format: "HH:mm", null for Holidays
    private boolean isAllDay; // For Tasks and Events
    private long startDate; // Timestamp for start date
    private long endDate; // Timestamp for end date (for Sessions and Holidays)
    private String userId;
    private String creatorDisplayName;

    public Task() {} // Required for Firestore

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSessionType() { return sessionType; }
    public void setSessionType(String sessionType) { this.sessionType = sessionType; }
    public List<String> getWeekdays() { return weekdays; }
    public void setWeekdays(List<String> weekdays) { this.weekdays = weekdays; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public boolean isAllDay() { return isAllDay; }
    public void setAllDay(boolean allDay) { this.isAllDay = allDay; }
    public long getStartDate() { return startDate; }
    public void setStartDate(long startDate) { this.startDate = startDate; }
    public long getEndDate() { return endDate; }
    public void setEndDate(long endDate) { this.endDate = endDate; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getCreatorDisplayName() { return creatorDisplayName; }
    public void setCreatorDisplayName(String creatorDisplayName) { this.creatorDisplayName = creatorDisplayName; }
}