package com.ali.postgresql.taskPostgreSqlManager;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class PostgreSqlJsonUtils {
    private static final String TAG = "SqlJsonUtils";

    public static List<PostgreSqlTaskModel> loadTasksFromJson(Context context) {
        try {
            Log.d(TAG, "SQL JSON faylı yüklənir...");

            // Assets qovluğunda axtar
            String[] possiblePaths = {
                    "task/sql_task.json",
                    "sql_task.json",
                    "tasks/sql_task.json"
            };

            InputStream is = null;
            String foundPath = null;

            for (String path : possiblePaths) {
                try {
                    is = context.getAssets().open(path);
                    foundPath = path;
                    Log.d(TAG, "Fayl tapıldı: " + path);
                    break;
                } catch (Exception e) {
                    // Növbəti yolu yoxla
                }
            }

            if (is == null) {
                Log.e(TAG, "Heç bir SQL JSON faylı tapılmadı!");
                return null;
            }

            // Faylı oxu
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String json = new String(buffer, StandardCharsets.UTF_8);
            Log.d(TAG, "JSON faylı oxundu, ölçü: " + size + " bytes");

            // JSON-u parse et
            Gson gson = new Gson();
            Type responseType = new TypeToken<SqlTaskResponse>() {}.getType();
            SqlTaskResponse response = gson.fromJson(json, responseType);

            if (response != null && response.getTasks() != null) {
                Log.d(TAG, response.getTasks().size() + " SQL task yükləndi");
                return response.getTasks();
            } else {
                Log.e(TAG, "JSON parse edilərkən xəta");
                return null;
            }

        } catch (Exception e) {
            Log.e(TAG, "JSON yükləmə xətası: " + e.getMessage(), e);
            return null;
        }
    }

    // JSON response structure
    private static class SqlTaskResponse {
        private List<PostgreSqlTaskModel> tasks;

        public List<PostgreSqlTaskModel> getTasks() {
            return tasks;
        }

        public void setTasks(List<PostgreSqlTaskModel> tasks) {
            this.tasks = tasks;
        }
    }
}