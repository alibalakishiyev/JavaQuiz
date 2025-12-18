package com.ali.postgresql.taskPostgreSqlManager;

import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.aimain.taskAimanager.AiTaskModel;
import com.ali.systemIn.R;

import java.util.List;

public class PostgreSqlTaskAdapter extends RecyclerView.Adapter<PostgreSqlTaskAdapter.PythonTaskViewHolder> {

    private static final String TAG = "PostgreSqlTaskAdapter";
    private List<PostgreSqlTaskModel> tasks;
    private OnTaskClickListener listener;
    private SharedPreferences sharedPreferences;

    public interface OnTaskClickListener {
        void onTaskClick(PostgreSqlTaskModel task);
    }

    public PostgreSqlTaskAdapter(List<PostgreSqlTaskModel> tasks, OnTaskClickListener listener, SharedPreferences sharedPreferences) {
        this.tasks = tasks;
        this.listener = listener;
        this.sharedPreferences = sharedPreferences;
    }

    @NonNull
    @Override
    public PythonTaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.task_item, parent, false);
        return new PythonTaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PythonTaskViewHolder holder, int position) {
        PostgreSqlTaskModel task = tasks.get(position);
        Log.d(TAG, "Binding Sql task: " + task.getTitle() + " position: " + position);
        holder.bind(task, listener, sharedPreferences);
    }

    @Override
    public int getItemCount() {
        return tasks != null ? tasks.size() : 0;
    }

    static class PythonTaskViewHolder extends RecyclerView.ViewHolder {
        private TextView taskTitle;
        private TextView taskDescription;
        private TextView taskStatus;
        private View taskItemLayout;

        public PythonTaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.taskTitle);
            taskDescription = itemView.findViewById(R.id.taskDescription);
            taskStatus = itemView.findViewById(R.id.taskStatus);
            taskItemLayout = itemView.findViewById(R.id.taskItemLayout);


        }

        public void bind(PostgreSqlTaskModel task, OnTaskClickListener listener, SharedPreferences sharedPreferences) {
            // Task məlumatlarını set et
            taskTitle.setText(task.getId() + ". " + task.getTitle());
            taskDescription.setText(task.getDescription());

            // Statusu yoxla
            boolean isCompleted = sharedPreferences.getBoolean("sql_task_" + task.getId() + "_completed", false);
            updateStatus(isCompleted);

            // Click listener
            if (taskItemLayout != null) {
                taskItemLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (listener != null) {
                            listener.onTaskClick(task);
                        }
                    }
                });
            }
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