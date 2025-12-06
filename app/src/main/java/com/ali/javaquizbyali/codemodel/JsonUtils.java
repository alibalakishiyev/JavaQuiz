package com.ali.javaquizbyali.codemodel;

import android.content.Context;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class JsonUtils {

    private static final String TAG = "JsonUtils";

    public static List<TaskModel> loadTasksFromJson(Context context) {
        try {
            Log.d(TAG, "JSON faylı yüklənir...");

            // Əvvəlcə assets/task/tasks.json yoxla
            InputStream is = null;
            try {
                is = context.getAssets().open("task/tasks.json");
                Log.d(TAG, "tasks.json faylı task/ qovluğunda tapıldı");
            } catch (Exception e) {
                // Əgər tapılmazsa, birbaşa assets/tasks.json yoxla
                try {
                    is = context.getAssets().open("tasks.json");
                    Log.d(TAG, "tasks.json faylı assets kök qovluğunda tapıldı");
                } catch (Exception e2) {
                    Log.e(TAG, "tasks.json faylı heç yerdə tapılmadı");
                    return null;
                }
            }

            int size = is.available();
            byte[] buffer = new byte[size];
            int bytesRead = is.read(buffer);
            is.close();

            Log.d(TAG, "Fayl oxundu, ölçü: " + size + " bytes, oxunan: " + bytesRead + " bytes");

            String json = new String(buffer, StandardCharsets.UTF_8);
            Log.d(TAG, "JSON məzmunu (ilk 500 simvol): " + json.substring(0, Math.min(500, json.length())));

            Gson gson = new Gson();
            Type taskListType = new TypeToken<TaskList>() {}.getType();
            TaskList taskList = gson.fromJson(json, taskListType);

            if (taskList != null && taskList.getTasks() != null) {
                Log.d(TAG, taskList.getTasks().size() + " task uğurla yükləndi");
                for (TaskModel task : taskList.getTasks()) {
                    Log.d(TAG, "Yüklənən task: " + task.getId() + " - " + task.getTitle());
                }
            } else {
                Log.e(TAG, "Task list null və ya boş");
            }

            return taskList != null ? taskList.getTasks() : null;

        } catch (Exception e) {
            Log.e(TAG, "JSON faylı yüklənərkən xəta: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static class TaskList {
        private List<TaskModel> tasks;

        public List<TaskModel> getTasks() {
            return tasks;
        }

        public void setTasks(List<TaskModel> tasks) {
            this.tasks = tasks;
        }
    }
}