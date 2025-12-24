package com.ali.postgresql.taskPostgreSqlManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.systemIn.R;

import java.util.ArrayList;
import java.util.List;

public class PostgreSqlTask extends AppCompatActivity implements PostgreSqlTaskAdapter.OnTaskClickListener {
    private static final String TAG = "PostgreSqlTask";
    private RecyclerView recyclerView;
    private PostgreSqlTaskAdapter adapter;
    private List<PostgreSqlTaskModel> tasks = new ArrayList<>();
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        recyclerView = findViewById(R.id.tasksRecyclerView);
        sharedPreferences = getSharedPreferences("sql_tasks_prefs", MODE_PRIVATE);

        loadTasks();
        setupRecyclerView();
    }

    private void loadTasks() {
        List<PostgreSqlTaskModel> loadedTasks = PostgreSqlJsonUtils.loadTasksFromJson(this);

        if (loadedTasks != null && !loadedTasks.isEmpty()) {
            tasks = loadedTasks;
            Log.d(TAG, "Yüklənən tasks: " + tasks.size());

            // Hər task üçün log
            for (PostgreSqlTaskModel task : tasks) {
                Log.d(TAG, "Task " + task.getId() + ": " + task.getTitle() +
                        ", Query: " + task.getInitialQuery());
            }
        } else {
            Log.e(TAG, "Tasks yüklənmədi, dummy data əlavə edilir");
            // Dummy data
            tasks.add(new PostgreSqlTaskModel());
            tasks.get(0).setId(1);
            tasks.get(0).setTitle("Test Task");
            tasks.get(0).setDescription("Test description");
            tasks.get(0).setInitialQuery("SELECT * FROM employees;");
            tasks.get(0).setSolution("SELECT * FROM employees;");
        }
    }

    private void setupRecyclerView() {
        adapter = new PostgreSqlTaskAdapter(tasks, this, sharedPreferences);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        Log.d(TAG, "RecyclerView setup edildi, item sayı: " + adapter.getItemCount());
    }

    @Override
    public void onTaskClick(PostgreSqlTaskModel task) {
        Log.d(TAG, "Task kliklendi: ID=" + task.getId() + ", Title=" + task.getTitle());

        // Detail activity-ə keçid
        Intent intent = new Intent(this, PostgreSqlTaskDetail.class);
        intent.putExtra("taskId", task.getId());
        intent.putExtra("title", task.getTitle());
        intent.putExtra("description", task.getDescription());
        intent.putExtra("initialQuery", task.getInitialQuery());
        intent.putExtra("solution", task.getSolution());
        startActivityForResult(intent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {
            int taskId = data.getIntExtra("taskId", -1);
            boolean completed = data.getBooleanExtra("completed", false);

            if (taskId != -1 && completed) {
                markTaskAsCompleted(taskId);
            }
        }
    }

    public void markTaskAsCompleted(int taskId) {
        Log.d(TAG, "Task tamamlandı: " + taskId);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("task_" + taskId + "_completed", true);
        editor.apply();

        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }

        Toast.makeText(this, "Tapşırıq tamamlandı!", Toast.LENGTH_SHORT).show();
    }
}