package com.ali.javaquizbyali.codemodel;

import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.systemIn.R;

import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskViewHolder> {

    private static final String TAG = "TaskAdapter";
    private List<TaskModel> tasks;
    private OnTaskClickListener listener;
    private SharedPreferences sharedPreferences;

    public interface OnTaskClickListener {
        void onTaskClick(TaskModel task);
    }

    public TaskAdapter(List<TaskModel> tasks, OnTaskClickListener listener, SharedPreferences sharedPreferences) {
        this.tasks = tasks;
        this.listener = listener;
        this.sharedPreferences = sharedPreferences;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_item, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        TaskModel task = tasks.get(position);
        holder.bind(task, listener, sharedPreferences);
    }

    @Override
    public int getItemCount() {
        return tasks != null ? tasks.size() : 0;
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private TextView taskTitle;
        private TextView taskDescription;
        private TextView taskStatus;
        private View taskItemLayout;

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.taskTitle);
            taskDescription = itemView.findViewById(R.id.taskDescription);
            taskStatus = itemView.findViewById(R.id.taskStatus);
            taskItemLayout = itemView.findViewById(R.id.taskItemLayout);

            Log.d("TaskViewHolder", "ViewHolder yaradıldı");
        }

        public void bind(TaskModel task, OnTaskClickListener listener, SharedPreferences sharedPreferences) {
            Log.d("TaskViewHolder", "bind çağırıldı - Task: " + task.getTitle());

            // Task məlumatlarını set et
            taskTitle.setText(task.getId() + ". " + task.getTitle());
            taskDescription.setText(task.getDescription());

            // Statusu yoxla
            boolean isCompleted = sharedPreferences.getBoolean("task_" + task.getId() + "_completed", false);
            updateStatus(isCompleted);

            // ƏSAS DÜZƏLİŞ: Sadəcə BİR click listener istifadə edirik
            if (taskItemLayout != null) {
                taskItemLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d("TaskViewHolder", "=== TASK CLICKED - BIRINCI DEFE ===");
                        Log.d("TaskViewHolder", "Clicked task: " + task.getTitle());

                        if (listener != null) {
                            listener.onTaskClick(task);
                        }
                    }
                });
            } else {
                // Fallback: Əgər layout tapılmazsa, itemView istifadə et
                itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d("TaskViewHolder", "=== ITEMVIEW CLICKED ===");
                        Log.d("TaskViewHolder", "Clicked task: " + task.getTitle());

                        if (listener != null) {
                            listener.onTaskClick(task);
                        }
                    }
                });
            }

            // TextView-lərə click listener ƏLAVƏ ETMİRİK - bu qarışıqlıq yaradır
        }

        private void updateStatus(boolean isCompleted) {
            if (isCompleted) {
                taskStatus.setText("Tamamlandı");
                taskStatus.setTextColor(itemView.getResources().getColor(android.R.color.white));
                taskStatus.setBackgroundColor(itemView.getResources().getColor(android.R.color.holo_green_dark));
            } else {
                taskStatus.setText("Tamamlanmadı");
                taskStatus.setTextColor(itemView.getResources().getColor(android.R.color.white));
                taskStatus.setBackgroundColor(itemView.getResources().getColor(android.R.color.holo_red_dark));
            }
        }
    }
}