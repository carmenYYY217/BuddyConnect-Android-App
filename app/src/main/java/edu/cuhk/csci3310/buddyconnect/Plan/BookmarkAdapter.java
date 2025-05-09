package edu.cuhk.csci3310.buddyconnect.Plan;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

import edu.cuhk.csci3310.buddyconnect.R;

public class BookmarkAdapter extends RecyclerView.Adapter<BookmarkAdapter.ViewHolder> {
    private List<Bookmark> bookmarkList;
    private OnEditClickListener editClickListener;
    private OnDeleteClickListener deleteClickListener;

    public BookmarkAdapter(List<Bookmark> bookmarkList, OnEditClickListener editClickListener, OnDeleteClickListener deleteClickListener) {
        this.bookmarkList = bookmarkList;
        this.editClickListener = editClickListener;
        this.deleteClickListener = deleteClickListener;
    }

    public interface OnEditClickListener {
        void onEditClick(int position);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(int position);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bookmark_item, parent, false);
        return new ViewHolder(view, editClickListener, deleteClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Bookmark bookmark = bookmarkList.get(position);

        // Show only non-empty fields
        if (bookmark.getName().isEmpty()) {
            holder.nameTextView.setVisibility(View.GONE);
        } else {
            holder.nameTextView.setVisibility(View.VISIBLE);
            holder.nameTextView.setText(bookmark.getName());
        }

        if (bookmark.getAddress().isEmpty()) {
            holder.addressTextView.setVisibility(View.GONE);
        } else {
            holder.addressTextView.setVisibility(View.VISIBLE);
            holder.addressTextView.setText(bookmark.getAddress());
        }

        if (bookmark.getComments().isEmpty()) {
            holder.commentsTextView.setVisibility(View.GONE);
        } else {
            holder.commentsTextView.setVisibility(View.VISIBLE);
            holder.commentsTextView.setText(bookmark.getComments());
        }

        if (bookmark.getDate().isEmpty()) {
            holder.dateTextView.setVisibility(View.GONE);
        } else {
            holder.dateTextView.setVisibility(View.VISIBLE);
            holder.dateTextView.setText("Date: " + bookmark.getDate());
        }

        if (bookmark.getTime().isEmpty()) {
            holder.timeTextView.setVisibility(View.GONE);
        } else {
            holder.timeTextView.setVisibility(View.VISIBLE);
            holder.timeTextView.setText("Time: " + bookmark.getTime());
        }

        if (bookmark.getNumberOfPeople() <= 0) {
            holder.peopleTextView.setVisibility(View.GONE);
        } else {
            holder.peopleTextView.setVisibility(View.VISIBLE);
            holder.peopleTextView.setText("People: " + bookmark.getNumberOfPeople());
        }

        if (bookmark.getRating() > 0) {
            holder.ratingBar.setVisibility(View.VISIBLE);
            // Ensure the RatingBar is configured correctly
            holder.ratingBar.setNumStars(5);
            holder.ratingBar.setStepSize(0.5f);
            holder.ratingBar.setRating(bookmark.getRating());
            // Debug log to confirm the rating value
            Log.d("BookmarkAdapter", "Setting rating for position " + position + ": " + bookmark.getRating());
        } else {
            holder.ratingBar.setVisibility(View.GONE);
        }

        if (bookmark.getImageUri() != null && !bookmark.getImageUri().isEmpty()) {
            holder.imageView.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(bookmark.getImageUriAsUri())
                    .into(holder.imageView);
        } else {
            holder.imageView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return bookmarkList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, addressTextView, commentsTextView, dateTextView, timeTextView, peopleTextView;
        RatingBar ratingBar;
        ImageView imageView, editIcon, deleteIcon;

        public ViewHolder(@NonNull View itemView, OnEditClickListener editClickListener, OnDeleteClickListener deleteClickListener) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            addressTextView = itemView.findViewById(R.id.addressTextView);
            commentsTextView = itemView.findViewById(R.id.commentsTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            peopleTextView = itemView.findViewById(R.id.peopleTextView);
            ratingBar = itemView.findViewById(R.id.ratingBar);
            imageView = itemView.findViewById(R.id.imageView);
            editIcon = itemView.findViewById(R.id.editIcon);
            deleteIcon = itemView.findViewById(R.id.deleteIcon);

            editIcon.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    editClickListener.onEditClick(position);
                }
            });

            deleteIcon.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    deleteClickListener.onDeleteClick(position);
                }
            });
        }
    }
}