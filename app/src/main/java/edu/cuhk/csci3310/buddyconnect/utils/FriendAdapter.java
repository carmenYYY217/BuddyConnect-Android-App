package edu.cuhk.csci3310.buddyconnect.utils;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import java.util.List;

import edu.cuhk.csci3310.buddyconnect.R;
import edu.cuhk.csci3310.buddyconnect.friendRequest.User;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {
    private List<User> friendsList;
    private Context context;

    public FriendAdapter(Context context, List<User> friendsList) {
        this.context = context;
        this.friendsList = friendsList;
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        User friend = friendsList.get(position);
        holder.displayName.setText(friend.getDisplayName());

        // Set the background based on activity status
        if ("active".equals(friend.getActivityStatus())) {
            holder.profileIcon.setBackgroundResource(R.drawable.circle_icon_active);
        } else {
            holder.profileIcon.setBackgroundResource(R.drawable.circle_icon);
        }

        // Load the profile image
        String photoUrl = friend.getPhotoUrl();
        Log.d("FriendAdapter", "Loading photo for " + friend.getDisplayName() + ": " + photoUrl);
        if (photoUrl != null && !photoUrl.isEmpty()) {
            Glide.with(context)
                    .load(photoUrl)
                    .circleCrop()
                    .error(R.drawable.ic_account_icon)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable com.bumptech.glide.load.engine.GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            Log.e("GlideError", "Failed to load image: " + photoUrl, e);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                            Log.d("GlideError", "Image loaded successfully: " + photoUrl);
                            return false;
                        }
                    })
                    .into(holder.profileIcon);
        } else {
            Log.d("FriendAdapter", "No photoUrl for " + friend.getDisplayName() + ", using default icon");
            Glide.with(context)
                    .load(R.drawable.ic_account_icon)
                    .circleCrop()
                    .into(holder.profileIcon);
        }
    }

    @Override
    public int getItemCount() {
        return friendsList.size();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        ImageView profileIcon;
        TextView displayName;
        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            profileIcon = itemView.findViewById(R.id.friendIcon);
            displayName = itemView.findViewById(R.id.friendDisplayName);
        }
    }
}