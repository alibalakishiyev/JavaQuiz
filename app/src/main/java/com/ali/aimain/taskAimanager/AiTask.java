package com.ali.aimain.taskAimanager;


import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.pymain.PythonMain;
import com.ali.pymain.taskmanager.PythonConsole;
import com.ali.systemIn.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class AiTask extends AppCompatActivity {

    private static final String TAG = "AiTaskActivity";
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "AiTaskProgress";
    private List<AiTaskModel> tasks;
    private AiTaskAdapter taskAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        Log.d(TAG, "Ai Task Activity başladı");

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Load tasks from JSON
        tasks = AIJsonUtils.loadTasksFromJson(this);

        if (tasks == null || tasks.isEmpty()) {
            Log.e(TAG, "JSON faylı yüklənmədi VƏ YA boşdur");
            Toast.makeText(this, "Ai taskları tapılmadı!", Toast.LENGTH_LONG).show();
            tasks = new ArrayList<>();
        } else {
            Log.d(TAG, tasks.size() + " Ai task JSON-dan yükləndi");
            Toast.makeText(this, tasks.size() + " Ai task yükləndi", Toast.LENGTH_SHORT).show();
        }

        setupRecyclerView();
        getOnBackPressedDispatcher().addCallback(this, callback);

    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.tasksRecyclerView);
        if (recyclerView == null) {
            Log.e(TAG, "RecyclerView tapılmadı!");
            return;
        }

        Log.d(TAG, "RecyclerView tapıldı, setup edilir...");

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Listener-i burada yaradırıq
        AiTaskAdapter.OnTaskClickListener clickListener = new AiTaskAdapter.OnTaskClickListener() {
            @Override
            public void onTaskClick(AiTaskModel task) {
                Log.d(TAG, "=== LISTENER ÇAĞIRILDI ===");
                Log.d(TAG, "Clicked task: " + task.getTitle());
                Log.d(TAG, "Task ID: " + task.getId());
                openPythonConsole(task);
            }
        };

        taskAdapter = new AiTaskAdapter(tasks, clickListener, sharedPreferences);
        recyclerView.setAdapter(taskAdapter);

        Log.d(TAG, "RecyclerView quruldu, " + tasks.size() + " task");
    }

    private void openPythonConsole(AiTaskModel task) {
        Log.d(TAG, "=== OPEN AI CONSOLE ===");
        Log.d(TAG, "Task: " + task.getTitle());
        Log.d(TAG, "Initial Code uzunluğu: " + task.getInitialCode().length());

        try {
            Intent intent = new Intent(AiTask.this, PythonConsole.class);
            intent.putExtra("TASK_ID", task.getId());
            intent.putExtra("TASK_TITLE", task.getTitle());
            intent.putExtra("TASK_DESCRIPTION", task.getDescription());
            intent.putExtra("INITIAL_CODE", task.getInitialCode());

            // Convert tests to JSON
            com.google.gson.Gson gson = new com.google.gson.Gson();
            String testsJson = "[]";
            if (task.getTests() != null && !task.getTests().isEmpty()) {
                testsJson = gson.toJson(task.getTests());
                Log.d(TAG, task.getTests().size() + " test JSON-a çevrildi");
            }

            String solution = task.getSolution() != null ? task.getSolution() : "";

            intent.putExtra("TASK_TESTS", testsJson);
            intent.putExtra("TASK_SOLUTION", solution);

            Log.d(TAG, "Bütün extra-lar əlavə edildi, PythonConsole başladılır...");
            startActivity(intent);
            Log.d(TAG, "startActivity çağırıldı - UĞURLU!");

        } catch (Exception e) {
            Log.e(TAG, "PythonConsole başlatma XƏTASI: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Xəta: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume çağırıldı");
        if (taskAdapter != null) {
            taskAdapter.notifyDataSetChanged();
        }
    }

    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(AiTask.this);
            materialAlertDialogBuilder.setTitle(R.string.app_name);
            materialAlertDialogBuilder.setMessage("Are you sure want to exit the quiz?");
            materialAlertDialogBuilder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    dialog.dismiss();
                }
            });
            materialAlertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    startActivity(new Intent(AiTask.this , PythonMain.class));
                    finish();
                }
            });

            materialAlertDialogBuilder.show();
        }

    };
}