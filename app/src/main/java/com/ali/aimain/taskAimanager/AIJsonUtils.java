package com.ali.aimain.taskAimanager;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class AIJsonUtils {

    private static final String TAG = "AIJsonUtils";

    public static List<AiTaskModel> loadTasksFromJson(Context context) {
        try {
            Log.d(TAG, "Ai JSON faylı yüklənir...");

            // Əvvəlcə assets/python_tasks.json yoxla
            InputStream is = null;
            try {
                is = context.getAssets().open("ai_tasks.json");
                Log.d(TAG, "ai_tasks.json faylı tapıldı");
            } catch (Exception e) {
                // Əgər tapılmazsa, digər yerlərdə yoxla
                try {
                    is = context.getAssets().open("task/ai_tasks.json");
                    Log.d(TAG, "ai_tasks.json task qovluğunda tapıldı");
                } catch (Exception e2) {
                    Log.e(TAG, "ai_tasks.json faylı heç yerdə tapılmadı");
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
                Log.d(TAG, taskList.getTasks().size() + " Ai task uğurla yükləndi");
                for (AiTaskModel task : taskList.getTasks()) {
                    Log.d(TAG, "Yüklənən task: " + task.getId() + " - " + task.getTitle());
                }
            } else {
                Log.e(TAG, "Ai task list null və ya boş");
            }

            return taskList != null ? taskList.getTasks() : null;

        } catch (Exception e) {
            Log.e(TAG, "Ai JSON faylı yüklənərkən xəta: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static class TaskList {
        private List<AiTaskModel> tasks;

        public List<AiTaskModel> getTasks() {
            return tasks;
        }

        public void setTasks(List<AiTaskModel> tasks) {
            this.tasks = tasks;
        }
    }
}