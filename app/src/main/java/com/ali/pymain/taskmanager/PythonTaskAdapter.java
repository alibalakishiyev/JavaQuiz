package com.ali.pymain.taskmanager;

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

public class PythonTaskAdapter extends RecyclerView.Adapter<PythonTaskAdapter.PythonTaskViewHolder> {

    private static final String TAG = "PythonTaskAdapter";
    private List<PythonTaskModel> tasks;
    private OnTaskClickListener listener;
    private SharedPreferences sharedPreferences;

    public interface OnTaskClickListener {
        void onTaskClick(PythonTaskModel task);
    }

    public PythonTaskAdapter(List<PythonTaskModel> tasks, OnTaskClickListener listener, SharedPreferences sharedPreferences) {
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
        PythonTaskModel task = tasks.get(position);
        Log.d(TAG, "Binding Python task: " + task.getTitle() + " position: " + position);
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

            Log.d("PythonTaskViewHolder", "ViewHolder yaradıldı");
        }

        public void bind(PythonTaskModel task, OnTaskClickListener listener, SharedPreferences sharedPreferences) {
            Log.d("PythonTaskViewHolder", "bind çağırıldı - Task: " + task.getTitle());

            // Task məlumatlarını set et
            taskTitle.setText(task.getId() + ". " + task.getTitle());
            taskDescription.setText(task.getDescription());

            // Statusu yoxla
            boolean isCompleted = sharedPreferences.getBoolean("python_task_" + task.getId() + "_completed", false);
            updateStatus(isCompleted);

            // Click listener
            if (taskItemLayout != null) {
                taskItemLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.d("PythonTaskViewHolder", "=== PYTHON TASK CLICKED ===");
                        Log.d("PythonTaskViewHolder", "Clicked task: " + task.getTitle());

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