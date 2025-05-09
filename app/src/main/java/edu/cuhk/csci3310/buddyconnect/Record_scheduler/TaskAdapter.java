package edu.cuhk.csci3310.buddyconnect.Record_scheduler;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import edu.cuhk.csci3310.buddyconnect.R;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {
    private List<Task> tasks;
    private final SchedulerDialogFragment fragment;
    private final TaskSelectionListener selectionListener;
    private boolean isEditMode = false;
    private boolean isDeleteMode = false;
    private final Set<Task> selectedTasks = new HashSet<>();
    private final String currentUserId;

    public TaskAdapter(List<Task> tasks, SchedulerDialogFragment fragment, TaskSelectionListener selectionListener, String currentUserId) {
        this.tasks = tasks != null ? tasks : new ArrayList<>();
        this.fragment = fragment;
        this.selectionListener = selectionListener;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.task_item, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        Task task = tasks.get(position);
        holder.bind(task, currentUserId.equals(task.getUserId()));

        holder.itemView.setOnClickListener(v -> {
            if (isEditMode && currentUserId.equals(task.getUserId())) {
                fragment.showEditDialog(task);
            } else if (isDeleteMode && currentUserId.equals(task.getUserId())) {
                holder.checkBox.setChecked(!holder.checkBox.isChecked());
            }
        });

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isDeleteMode && currentUserId.equals(task.getUserId())) {
                if (isChecked) {
                    selectedTasks.add(task);
                } else {
                    selectedTasks.remove(task);
                }
                selectionListener.onTaskSelectionChanged(selectedTasks.size());
            }
        });

        if (isDeleteMode && currentUserId.equals(task.getUserId())) {
            holder.checkBox.setVisibility(View.VISIBLE);
            holder.checkBox.setChecked(selectedTasks.contains(task));
        } else {
            holder.checkBox.setVisibility(View.GONE);
            holder.checkBox.setChecked(false);
        }
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public void updateTasks(List<Task> newTasks) {
        this.tasks = newTasks != null ? newTasks : new ArrayList<>();
        selectedTasks.clear(); // Clear selections when tasks are updated
        selectionListener.onTaskSelectionChanged(selectedTasks.size());
        notifyDataSetChanged();
    }

    public void setEditMode(boolean editMode) {
        this.isEditMode = editMode;
        if (!editMode) {
            this.isDeleteMode = false;
            selectedTasks.clear();
            selectionListener.onTaskSelectionChanged(selectedTasks.size());
        }
        notifyDataSetChanged();
    }

    public void setDeleteMode(boolean deleteMode) {
        this.isDeleteMode = deleteMode;
        if (!deleteMode) {
            selectedTasks.clear();
            selectionListener.onTaskSelectionChanged(selectedTasks.size());
        }
        notifyDataSetChanged();
    }

    public Set<Task> getSelectedTasks() {
        return selectedTasks;
    }

    public void clearSelections() {
        selectedTasks.clear();
        selectionListener.onTaskSelectionChanged(selectedTasks.size());
        notifyDataSetChanged();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, timeTextView, typeTextView, creatorTextView;
        CheckBox checkBox;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            timeTextView = itemView.findViewById(R.id.timeEditText);
            typeTextView = itemView.findViewById(R.id.typeTextView);
            creatorTextView = itemView.findViewById(R.id.creatorTextView);
            checkBox = itemView.findViewById(R.id.taskCheckBox);
        }

        void bind(Task task, boolean isOwner) {
            if (task.getType().equals("Holiday")) {
                titleTextView.setText("Holiday");
            } else {
                titleTextView.setText(task.getTitle() != null ? task.getTitle() : "No Title");
            }

            if (task.getType().equals("Holiday")) {
                timeTextView.setText("");
            } else if (task.isAllDay()) {
                timeTextView.setText("All Day");
            } else {
                timeTextView.setText(task.getStartTime() + " - " + task.getEndTime());
            }

            if (task.getType().equals("Session")) {
                typeTextView.setText(task.getSessionType());
            } else {
                typeTextView.setText(task.getType());
            }
            typeTextView.setVisibility(View.VISIBLE);

            creatorTextView.setText(isOwner ? "" : "By: " + task.getCreatorDisplayName());
            creatorTextView.setVisibility(isOwner ? View.GONE : View.VISIBLE);
        }
    }
}