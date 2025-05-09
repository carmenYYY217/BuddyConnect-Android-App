package edu.cuhk.csci3310.buddyconnect.Plan;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class Bookmark implements Parcelable {
    private String name;
    private String address;
    private String comments;
    private String date;
    private String time;
    private int numberOfPeople;
    private float rating;
    private String imageUri; // Store as String for Firebase

    // Default constructor required for Firebase
    public Bookmark() {
        this.name = "";
        this.address = "";
        this.comments = "";
        this.date = "";
        this.time = "";
        this.numberOfPeople = 0;
        this.rating = 0.0f;
        this.imageUri = null;
    }

    public Bookmark(String name, String address, String comments, String date, String time, int numberOfPeople, float rating, Uri imageUri) {
        this.name = name != null ? name : "";
        this.address = address != null ? address : "";
        this.comments = comments != null ? comments : "";
        this.date = date != null ? date : "";
        this.time = time != null ? time : "";
        this.numberOfPeople = numberOfPeople;
        this.rating = rating;
        this.imageUri = imageUri != null ? imageUri.toString() : null;
    }

    protected Bookmark(Parcel in) {
        name = in.readString();
        address = in.readString();
        comments = in.readString();
        date = in.readString();
        time = in.readString();
        numberOfPeople = in.readInt();
        rating = in.readFloat();
        imageUri = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(address);
        dest.writeString(comments);
        dest.writeString(date);
        dest.writeString(time);
        dest.writeInt(numberOfPeople);
        dest.writeFloat(rating);
        dest.writeString(imageUri);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Bookmark> CREATOR = new Creator<Bookmark>() {
        @Override
        public Bookmark createFromParcel(Parcel in) {
            return new Bookmark(in);
        }

        @Override
        public Bookmark[] newArray(int size) {
            return new Bookmark[size];
        }
    };

    // Getters and setters for Firebase
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public int getNumberOfPeople() { return numberOfPeople; }
    public void setNumberOfPeople(int numberOfPeople) { this.numberOfPeople = numberOfPeople; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }

    public String getImageUri() { return imageUri; }
    public void setImageUri(String imageUri) { this.imageUri = imageUri; }

    // Helper method to get Uri object
    @NonNull
    public Uri getImageUriAsUri() {
        return imageUri != null ? Uri.parse(imageUri) : Uri.EMPTY;
    }
}