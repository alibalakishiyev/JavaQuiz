package com.ali.postgresql.taskPostgreSqlManager;

import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.systemIn.R;

import java.util.List;

public class PostgreSqlTaskAdapter extends RecyclerView.Adapter<PostgreSqlTaskAdapter.TaskViewHolder> {
    private static final String TAG = "SqlTaskAdapter";
    private List<PostgreSqlTaskModel> tasks;
    private OnTaskClickListener listener;
    private SharedPreferences sharedPreferences;

    public interface OnTaskClickListener {
        void onTaskClick(PostgreSqlTaskModel task);
    }

    public PostgreSqlTaskAdapter(List<PostgreSqlTaskModel> tasks, OnTaskClickListener listener,
                                 SharedPreferences sharedPreferences) {
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
        PostgreSqlTaskModel task = tasks.get(position);
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

        public TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.taskTitle);
            taskDescription = itemView.findViewById(R.id.taskDescription);
            taskStatus = itemView.findViewById(R.id.taskStatus);
        }

        public void bind(PostgreSqlTaskModel task, OnTaskClickListener listener,
                         SharedPreferences sharedPreferences) {
            // Task məlumatlarını set et
            taskTitle.setText(task.getId() + ". " + task.getTitle());
            taskDescription.setText(task.getDescription());

            // Statusu yoxla
            String statusKey = "task_" + task.getId() + "_completed";
            boolean isCompleted = sharedPreferences.getBoolean(statusKey, false);

            Log.d(TAG, "Task " + task.getId() + " status: " + isCompleted + " (key: " + statusKey + ")");

            // Statusu yenilə
            if (isCompleted) {
                taskStatus.setText("Tamamlandı");
                taskStatus.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.green));
            } else {
                taskStatus.setText("Tamamlanmadı");
                taskStatus.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.red));
            }

            // Click listener
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onTaskClick(task);
                    }
                }
            });
        }
    }
}