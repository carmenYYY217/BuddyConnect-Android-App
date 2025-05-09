package edu.cuhk.csci3310.buddyconnect.friendRequest;

public class User {
    private String displayName;
    private String email;
    private String friendshipStatus;
    private String activityStatus;
    private String photoUrl;

    public User() {} // Needed for Firestore

    public User(String displayName, String email, String friendshipStatus, String photoUrl, String activityStatus) {
        this.displayName = displayName;
        this.email = email;
        this.friendshipStatus = friendshipStatus;
        this.photoUrl = photoUrl;
        this.activityStatus = activityStatus;
    }

    // Legacy constructor without photoUrl
    public User(String displayName, String email, String friendshipStatus) {
        this(displayName, email, friendshipStatus, null, null);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public String getStatus() {
        return friendshipStatus;
    }
    public String getPhotoUrl() { return photoUrl;}

    public void setFriendshipStatus(String friendshipStatus) {
        this.friendshipStatus = friendshipStatus;
    }
    public String getActivityStatus() {
        return activityStatus;
    }
}