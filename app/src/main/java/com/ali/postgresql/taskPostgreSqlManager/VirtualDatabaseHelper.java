package com.ali.postgresql.taskPostgreSqlManager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VirtualDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "VirtualDatabaseHelper";
    private static final String DATABASE_NAME = "virtual_sql_trainer.db";
    private static final int DATABASE_VERSION = 4;
    private Context context;

    // Cədvəl strukturları
    private static final Map<String, String[]> TABLE_SCHEMAS = new HashMap<String, String[]>() {{
        put("employees", new String[]{
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "name TEXT NOT NULL",
                "age INTEGER",
                "salary REAL",
                "department TEXT",
                "city TEXT",
                "hire_date TEXT",
                "email TEXT",
                "phone TEXT",
                "bonus REAL DEFAULT 0"
        });

        put("departments", new String[]{
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "name TEXT NOT NULL UNIQUE",
                "location TEXT",
                "budget REAL",
                "manager_id INTEGER",
                "employee_count INTEGER DEFAULT 0"
        });

        put("products", new String[]{
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "name TEXT NOT NULL",
                "price REAL",
                "category TEXT",
                "stock INTEGER",
                "supplier TEXT",
                "created_date TEXT",
                "description TEXT",
                "rating REAL DEFAULT 0",
                "discount REAL DEFAULT 0"
        });

        put("orders", new String[]{
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "customer_id INTEGER",
                "product_id INTEGER",
                "quantity INTEGER",
                "order_date TEXT",
                "total_amount REAL",
                "status TEXT DEFAULT 'pending'",
                "shipping_address TEXT",
                "payment_method TEXT",
                "discount_applied REAL DEFAULT 0"
        });

        put("customers", new String[]{
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "first_name TEXT NOT NULL",
                "last_name TEXT NOT NULL",
                "email TEXT UNIQUE",
                "phone TEXT",
                "city TEXT",
                "country TEXT DEFAULT 'Azerbaijan'",
                "registration_date TEXT",
                "loyalty_points INTEGER DEFAULT 0",
                "total_spent REAL DEFAULT 0"
        });

        put("suppliers", new String[]{
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "company_name TEXT NOT NULL",
                "contact_person TEXT",
                "phone TEXT",
                "email TEXT",
                "city TEXT",
                "rating REAL DEFAULT 0",
                "products_supplied INTEGER DEFAULT 0"
        });

        put("students", new String[]{
                "id INTEGER PRIMARY KEY AUTOINCREMENT",
                "full_name TEXT NOT NULL",
                "faculty TEXT",
                "course INTEGER",
                "gpa REAL",
                "scholarship REAL DEFAULT 0",
                "enrollment_date TEXT",
                "email TEXT"
        });
    }};

    public VirtualDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(TAG, "Virtual verilənlər bazası yaradılır...");

        // Bütün cədvəlləri yarat
        for (Map.Entry<String, String[]> entry : TABLE_SCHEMAS.entrySet()) {
            String tableName = entry.getKey();
            String[] columns = entry.getValue();

            StringBuilder createTableSQL = new StringBuilder();
            createTableSQL.append("CREATE TABLE ").append(tableName).append(" (");

            for (int i = 0; i < columns.length; i++) {
                createTableSQL.append(columns[i]);
                if (i < columns.length - 1) {
                    createTableSQL.append(", ");
                }
            }
            createTableSQL.append(");");

            db.execSQL(createTableSQL.toString());
            Log.d(TAG, "Cədvəl yaradıldı: " + tableName);
        }

        // JSON fayllarından məlumatları yüklə
        loadInitialDataFromJSON(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Verilənlər bazası yenilənir: " + oldVersion + " -> " + newVersion);

        // Köhnə cədvəlləri sil
        for (String tableName : TABLE_SCHEMAS.keySet()) {
            db.execSQL("DROP TABLE IF EXISTS " + tableName);
        }

        // Yenisini yarat
        onCreate(db);
    }

    // JSON fayllarından məlumatları yüklə
    private void loadInitialDataFromJSON(SQLiteDatabase db) {
        Log.d(TAG, "JSON fayllarından məlumatlar yüklənir...");

        try {
            // Employees yüklə
            loadEmployeesFromJSON(db);

            // Departments yüklə
            loadDepartmentsFromJSON(db);

            // Products yüklə
            loadProductsFromJSON(db);

            // Customers yüklə
            loadCustomersFromJSON(db);

            // Suppliers yüklə
            loadSuppliersFromJSON(db);

            // Orders yüklə
            loadOrdersFromJSON(db);

            // Students yüklə
            loadStudentsFromJSON(db);

            Log.d(TAG, "Bütün JSON məlumatları uğurla yükləndi");
        } catch (Exception e) {
            Log.e(TAG, "JSON yükləmə xətası: " + e.getMessage());
            createDefaultData(db);
        }
    }

    private void createDefaultData(SQLiteDatabase db) {
        Log.d(TAG, "Default məlumatlar yaradılır...");

        String[] employees = {
                "INSERT INTO employees (name, age, salary, department, city, hire_date, email, phone, bonus) VALUES ('Ali Məmmədov', 28, 2500.00, 'IT', 'Baku', '2020-05-15', 'ali@company.com', '+994501234567', 500.00)",
                "INSERT INTO employees (name, age, salary, department, city, hire_date, email, phone, bonus) VALUES ('Aysel Həsənova', 32, 3200.00, 'HR', 'Gəncə', '2019-08-20', 'aysel@company.com', '+994502345678', 400.00)",
                "INSERT INTO employees (name, age, salary, department, city, hire_date, email, phone, bonus) VALUES ('Mehmet Əliyev', 25, 1800.00, 'IT', 'Baku', '2021-03-10', 'mehmet@company.com', '+994503456789', 300.00)"
        };

        for (String sql : employees) {
            db.execSQL(sql);
        }
    }

    // JSON fayllarını oxumaq üçün helper metod
    private String readJSONFile(String fileName) {
        StringBuilder json = new StringBuilder();
        try {
            InputStream is = context.getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            reader.close();
            is.close();
        } catch (IOException e) {
            Log.e(TAG, "JSON fayl oxuma xətası: " + fileName + " - " + e.getMessage());
            return null;
        }
        return json.toString();
    }

    // Employees JSON yüklə
    private void loadEmployeesFromJSON(SQLiteDatabase db) {
        String jsonString = readJSONFile("sql_query/virtual_data/employees.json");
        if (jsonString == null) return;

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                ContentValues values = new ContentValues();

                values.put("name", obj.getString("name"));
                values.put("age", obj.getInt("age"));
                values.put("salary", obj.getDouble("salary"));
                values.put("department", obj.getString("department"));
                values.put("city", obj.getString("city"));
                values.put("hire_date", obj.getString("hire_date"));
                values.put("email", obj.getString("email"));
                values.put("phone", obj.getString("phone"));
                values.put("bonus", obj.getDouble("bonus"));

                db.insert("employees", null, values);
            }
            Log.d(TAG, "Employees yükləndi: " + jsonArray.length() + " sətir");
        } catch (JSONException e) {
            Log.e(TAG, "Employees JSON parsing error: " + e.getMessage());
        }
    }

    // Products JSON yüklə
    private void loadProductsFromJSON(SQLiteDatabase db) {
        String jsonString = readJSONFile("sql_query/virtual_data/products.json");
        if (jsonString == null) return;

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                ContentValues values = new ContentValues();

                values.put("name", obj.getString("name"));
                values.put("price", obj.getDouble("price"));
                values.put("category", obj.getString("category"));
                values.put("stock", obj.getInt("stock"));
                values.put("supplier", obj.getString("supplier"));
                values.put("description", obj.getString("description"));
                values.put("rating", obj.getDouble("rating"));
                values.put("discount", obj.getDouble("discount"));
                values.put("created_date", "2023-01-01");

                db.insert("products", null, values);
            }
            Log.d(TAG, "Products yükləndi: " + jsonArray.length() + " sətir");
        } catch (JSONException e) {
            Log.e(TAG, "Products JSON parsing error: " + e.getMessage());
        }
    }

    // Customers JSON yüklə
    private void loadCustomersFromJSON(SQLiteDatabase db) {
        String jsonString = readJSONFile("sql_query/virtual_data/customers.json");
        if (jsonString == null) return;

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                ContentValues values = new ContentValues();

                values.put("first_name", obj.getString("first_name"));
                values.put("last_name", obj.getString("last_name"));
                values.put("email", obj.getString("email"));
                values.put("phone", obj.getString("phone"));
                values.put("city", obj.getString("city"));
                values.put("country", obj.getString("country"));
                values.put("loyalty_points", obj.getInt("loyalty_points"));
                values.put("total_spent", obj.getDouble("total_spent"));
                values.put("registration_date", "2023-01-01");

                db.insert("customers", null, values);
            }
            Log.d(TAG, "Customers yükləndi: " + jsonArray.length() + " sətir");
        } catch (JSONException e) {
            Log.e(TAG, "Customers JSON parsing error: " + e.getMessage());
        }
    }

    // Departments JSON yüklə
    private void loadDepartmentsFromJSON(SQLiteDatabase db) {
        String jsonString = readJSONFile("sql_query/virtual_data/departments.json");
        if (jsonString == null) return;

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                ContentValues values = new ContentValues();

                values.put("name", obj.getString("name"));
                values.put("location", obj.getString("location"));
                values.put("budget", obj.getDouble("budget"));
                values.put("manager_id", obj.getInt("manager_id"));
                values.put("employee_count", obj.getInt("employee_count"));

                db.insert("departments", null, values);
            }
            Log.d(TAG, "Departments yükləndi: " + jsonArray.length() + " sətir");
        } catch (JSONException e) {
            Log.e(TAG, "Departments JSON parsing error: " + e.getMessage());
        }
    }

    // Suppliers JSON yüklə
    private void loadSuppliersFromJSON(SQLiteDatabase db) {
        String jsonString = readJSONFile("sql_query/virtual_data/suppliers.json");
        if (jsonString == null) return;

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                ContentValues values = new ContentValues();

                values.put("company_name", obj.getString("company_name"));
                values.put("contact_person", obj.getString("contact_person"));
                values.put("phone", obj.getString("phone"));
                values.put("email", obj.getString("email"));
                values.put("city", obj.getString("city"));
                values.put("rating", obj.getDouble("rating"));
                values.put("products_supplied", obj.getInt("products_supplied"));

                db.insert("suppliers", null, values);
            }
            Log.d(TAG, "Suppliers yükləndi: " + jsonArray.length() + " sətir");
        } catch (JSONException e) {
            Log.e(TAG, "Suppliers JSON parsing error: " + e.getMessage());
        }
    }

    // Orders JSON yüklə
    private void loadOrdersFromJSON(SQLiteDatabase db) {
        String jsonString = readJSONFile("sql_query/virtual_data/orders.json");
        if (jsonString == null) return;

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                ContentValues values = new ContentValues();

                values.put("customer_id", obj.getInt("customer_id"));
                values.put("product_id", obj.getInt("product_id"));
                values.put("quantity", obj.getInt("quantity"));
                values.put("order_date", obj.getString("order_date"));
                values.put("total_amount", obj.getDouble("total_amount"));
                values.put("status", obj.getString("status"));
                values.put("shipping_address", obj.getString("shipping_address"));
                values.put("payment_method", obj.getString("payment_method"));
                values.put("discount_applied", obj.getDouble("discount_applied"));

                db.insert("orders", null, values);
            }
            Log.d(TAG, "Orders yükləndi: " + jsonArray.length() + " sətir");
        } catch (JSONException e) {
            Log.e(TAG, "Orders JSON parsing error: " + e.getMessage());
        }
    }

    // Students JSON yüklə
    private void loadStudentsFromJSON(SQLiteDatabase db) {
        String jsonString = readJSONFile("sql_query/virtual_data/students.json");
        if (jsonString == null) return;

        try {
            JSONArray jsonArray = new JSONArray(jsonString);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                ContentValues values = new ContentValues();

                values.put("full_name", obj.getString("full_name"));
                values.put("faculty", obj.getString("faculty"));
                values.put("course", obj.getInt("course"));
                values.put("gpa", obj.getDouble("gpa"));
                values.put("scholarship", obj.getDouble("scholarship"));
                values.put("enrollment_date", obj.getString("enrollment_date"));
                values.put("email", obj.getString("email"));

                db.insert("students", null, values);
            }
            Log.d(TAG, "Students yükləndi: " + jsonArray.length() + " sətir");
        } catch (JSONException e) {
            Log.e(TAG, "Students JSON parsing error: " + e.getMessage());
        }
    }

    // ============ SQL İCRASI ============

    public List<List<String>> executeQuery(String sqlQuery) {
        List<List<String>> result = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getWritableDatabase();
            String normalizedQuery = sqlQuery.trim().toLowerCase();

            Log.d(TAG, "SQL sorğusu icra edilir: " + sqlQuery);

            if (normalizedQuery.startsWith("select") ||
                    normalizedQuery.startsWith("pragma") ||
                    normalizedQuery.startsWith("explain") ||
                    normalizedQuery.startsWith("with")) {

                cursor = db.rawQuery(sqlQuery, null);

                if (cursor != null) {
                    List<String> headers = new ArrayList<>();
                    for (int i = 0; i < cursor.getColumnCount(); i++) {
                        headers.add(cursor.getColumnName(i));
                    }
                    result.add(headers);

                    while (cursor.moveToNext()) {
                        List<String> row = new ArrayList<>();
                        for (int i = 0; i < cursor.getColumnCount(); i++) {
                            String value = cursor.getString(i);
                            row.add(value != null ? value : "NULL");
                        }
                        result.add(row);
                    }

                    if (result.size() == 1) {
                        result.add(createRow("INFO", "Nəticə tapılmadı"));
                    }

                    Log.d(TAG, "Sorğu uğurlu, " + (result.size() - 1) + " sətir tapıldı");
                }

            } else if (normalizedQuery.startsWith("insert")) {

                db.execSQL(sqlQuery);

                Cursor changesCursor = db.rawQuery("SELECT last_insert_rowid()", null);
                long lastInsertId = -1;
                if (changesCursor != null && changesCursor.moveToFirst()) {
                    lastInsertId = changesCursor.getLong(0);
                }
                if (changesCursor != null) changesCursor.close();

                result.add(createRow("SUCCESS", "INSERT əməliyyatı uğurlu"));
                result.add(createRow("Last Insert ID", String.valueOf(lastInsertId)));

            } else if (normalizedQuery.startsWith("update")) {

                db.execSQL(sqlQuery);

                Cursor changesCursor = db.rawQuery("SELECT changes()", null);
                int affectedRows = 0;
                if (changesCursor != null && changesCursor.moveToFirst()) {
                    affectedRows = changesCursor.getInt(0);
                }
                if (changesCursor != null) changesCursor.close();

                result.add(createRow("SUCCESS", "UPDATE əməliyyatı uğurlu"));
                result.add(createRow("Affected Rows", String.valueOf(affectedRows)));

            } else if (normalizedQuery.startsWith("delete")) {

                db.execSQL(sqlQuery);

                Cursor changesCursor = db.rawQuery("SELECT changes()", null);
                int affectedRows = 0;
                if (changesCursor != null && changesCursor.moveToFirst()) {
                    affectedRows = changesCursor.getInt(0);
                }
                if (changesCursor != null) changesCursor.close();

                result.add(createRow("SUCCESS", "DELETE əməliyyatı uğurlu"));
                result.add(createRow("Affected Rows", String.valueOf(affectedRows)));

            } else if (normalizedQuery.startsWith("create table")) {

                db.execSQL(sqlQuery);
                String tableName = extractTableNameFromCreate(sqlQuery);
                result.add(createRow("SUCCESS", "Cədvəl uğurla yaradıldı"));
                result.add(createRow("Table Name", tableName));

            } else if (normalizedQuery.startsWith("alter table")) {

                db.execSQL(sqlQuery);
                result.add(createRow("SUCCESS", "Cədvəl uğurla dəyişdirildi"));

            } else if (normalizedQuery.startsWith("drop table")) {

                db.execSQL(sqlQuery);
                result.add(createRow("SUCCESS", "Cədvəl uğurla silindi"));

            } else if (normalizedQuery.startsWith("begin transaction") ||
                    normalizedQuery.startsWith("commit") ||
                    normalizedQuery.startsWith("rollback")) {

                db.execSQL(sqlQuery);
                result.add(createRow("SUCCESS", "Transaction əməliyyatı tamamlandı"));

            } else {
                db.execSQL(sqlQuery);
                result.add(createRow("SUCCESS", "SQL əmri icra edildi"));
            }

            Log.d(TAG, "SQL sorğusu uğurla icra edildi");

        } catch (Exception e) {
            Log.e(TAG, "SQL sorğu xətası: " + e.getMessage());
            return createErrorResult("SQL xətası: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        return result;
    }

    public List<List<String>> executeAdminQuery(String sqlQuery) {
        return executeQuery(sqlQuery);
    }

    private String extractTableNameFromCreate(String sql) {
        try {
            String lower = sql.toLowerCase();
            int createIndex = lower.indexOf("create table");
            if (createIndex == -1) return "unknown";

            String afterCreate = sql.substring(createIndex + "create table".length()).trim();
            int spaceIndex = afterCreate.indexOf(' ');
            int bracketIndex = afterCreate.indexOf('(');

            int endIndex = Math.min(
                    spaceIndex == -1 ? afterCreate.length() : spaceIndex,
                    bracketIndex == -1 ? afterCreate.length() : bracketIndex
            );

            return afterCreate.substring(0, endIndex).trim();
        } catch (Exception e) {
            return "unknown";
        }
    }

    private List<String> createRow(String col1, String col2) {
        List<String> row = new ArrayList<>();
        row.add(col1);
        row.add(col2);
        return row;
    }

    private List<List<String>> createErrorResult(String message) {
        List<List<String>> result = new ArrayList<>();
        List<String> errorRow = new ArrayList<>();
        errorRow.add("Xəta");
        errorRow.add(message);
        result.add(errorRow);
        return result;
    }

    // ============ CƏDVƏL ƏMƏLLƏRİ ============

    public List<String> getTableList() {
        List<String> tables = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();
            cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name", null);

            while (cursor != null && cursor.moveToNext()) {
                tables.add(cursor.getString(0));
            }

        } catch (Exception e) {
            Log.e(TAG, "Cədvəl siyahısı alma xətası: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        return tables;
    }

    public List<List<String>> getTableStructure(String tableName) {
        List<List<String>> structure = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();
            cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);

            if (cursor != null) {
                List<String> headers = new ArrayList<>();
                headers.add("Sütun");
                headers.add("Tip");
                headers.add("Null Ola bilər");
                headers.add("Default");
                headers.add("PK");
                structure.add(headers);

                while (cursor.moveToNext()) {
                    List<String> row = new ArrayList<>();
                    row.add(cursor.getString(1));
                    row.add(cursor.getString(2));
                    row.add(cursor.getInt(3) == 0 ? "NO" : "YES");
                    row.add(cursor.getString(4) != null ? cursor.getString(4) : "NULL");
                    row.add(cursor.getInt(5) == 1 ? "✓" : "");
                    structure.add(row);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Cədvəl strukturu alma xətası: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        return structure;
    }

    public boolean createTable(String tableName, String[] columns) {
        SQLiteDatabase db = null;

        try {
            db = this.getWritableDatabase();

            StringBuilder createTableSQL = new StringBuilder();
            createTableSQL.append("CREATE TABLE ").append(tableName).append(" (");

            for (int i = 0; i < columns.length; i++) {
                createTableSQL.append(columns[i]);
                if (i < columns.length - 1) {
                    createTableSQL.append(", ");
                }
            }
            createTableSQL.append(");");

            db.execSQL(createTableSQL.toString());
            Log.d(TAG, "Cədvəl yaradıldı: " + tableName);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Cədvəl yaradarkən xəta: " + e.getMessage());
            return false;
        } finally {
            if (db != null) db.close();
        }
    }

    public boolean dropTable(String tableName) {
        SQLiteDatabase db = null;

        try {
            db = this.getWritableDatabase();

            if (tableName.startsWith("sqlite_")) {
                return false;
            }

            db.execSQL("DROP TABLE IF EXISTS " + tableName);
            Log.d(TAG, "Cədvəl silindi: " + tableName);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Cədvəl silərkən xəta: " + e.getMessage());
            return false;
        } finally {
            if (db != null) db.close();
        }
    }

    public boolean insertData(String tableName, ContentValues values) {
        SQLiteDatabase db = null;

        try {
            db = this.getWritableDatabase();
            long result = db.insert(tableName, null, values);

            if (result == -1) {
                Log.e(TAG, "Məlumat əlavə edilmədi");
                return false;
            }

            Log.d(TAG, "Məlumat əlavə edildi, ID: " + result);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Məlumat əlavə edərkən xəta: " + e.getMessage());
            return false;
        } finally {
            if (db != null) db.close();
        }
    }

    public boolean updateData(String tableName, ContentValues values, String whereClause, String[] whereArgs) {
        SQLiteDatabase db = null;

        try {
            db = this.getWritableDatabase();
            int rowsAffected = db.update(tableName, values, whereClause, whereArgs);

            Log.d(TAG, rowsAffected + " sətir yeniləndi");
            return rowsAffected > 0;

        } catch (Exception e) {
            Log.e(TAG, "Məlumat yenilənərkən xəta: " + e.getMessage());
            return false;
        } finally {
            if (db != null) db.close();
        }
    }

    public boolean deleteData(String tableName, String whereClause, String[] whereArgs) {
        SQLiteDatabase db = null;

        try {
            db = this.getWritableDatabase();
            int rowsAffected = db.delete(tableName, whereClause, whereArgs);

            Log.d(TAG, rowsAffected + " sətir silindi");
            return rowsAffected > 0;

        } catch (Exception e) {
            Log.e(TAG, "Məlumat silərkən xəta: " + e.getMessage());
            return false;
        } finally {
            if (db != null) db.close();
        }
    }

    // Cədvəldən nümunə məlumatları gətir
    public List<List<String>> getSampleData(String tableName, int limit) {
        String query = "SELECT * FROM " + tableName + " LIMIT " + limit;
        return executeQuery(query);
    }

    // ============ VERİLƏNLƏR BAZASI ƏMƏLLƏRİ ============

    // Verilənlər bazasını sıfırla
    public void resetDatabase() {
        SQLiteDatabase db = null;

        try {
            db = this.getWritableDatabase();

            for (String tableName : TABLE_SCHEMAS.keySet()) {
                db.execSQL("DROP TABLE IF EXISTS " + tableName);
            }

            for (Map.Entry<String, String[]> entry : TABLE_SCHEMAS.entrySet()) {
                String tableName = entry.getKey();
                String[] columns = entry.getValue();

                StringBuilder createTableSQL = new StringBuilder();
                createTableSQL.append("CREATE TABLE ").append(tableName).append(" (");

                for (int i = 0; i < columns.length; i++) {
                    createTableSQL.append(columns[i]);
                    if (i < columns.length - 1) {
                        createTableSQL.append(", ");
                    }
                }
                createTableSQL.append(");");

                db.execSQL(createTableSQL.toString());
            }

            loadInitialDataFromJSON(db);

            Log.d(TAG, "Verilənlər bazası sıfırlandı");

        } catch (Exception e) {
            Log.e(TAG, "Verilənlər bazası sıfırlarkən xəta: " + e.getMessage());
        } finally {
            if (db != null) db.close();
        }
    }

    // Cədvəlin sütunlarını gətir
    public List<String> getTableColumns(String tableName) {
        List<String> columns = new ArrayList<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();
            cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);

            while (cursor != null && cursor.moveToNext()) {
                columns.add(cursor.getString(1));
            }

        } catch (Exception e) {
            Log.e(TAG, "Sütunlar alınarkən xəta: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        return columns;
    }

    // Verilənlər bazası statistika
    public Map<String, Object> getDatabaseStats() {
        Map<String, Object> stats = new HashMap<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();

            cursor = db.rawQuery("SELECT COUNT(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'", null);
            if (cursor != null && cursor.moveToFirst()) {
                stats.put("table_count", cursor.getInt(0));
            }
            cursor.close();

            int totalRows = 0;
            List<String> tables = getTableList();
            for (String table : tables) {
                cursor = db.rawQuery("SELECT COUNT(*) FROM " + table, null);
                if (cursor != null && cursor.moveToFirst()) {
                    totalRows += cursor.getInt(0);
                }
                cursor.close();
            }
            stats.put("total_rows", totalRows);

            stats.put("version", DATABASE_VERSION);

        } catch (Exception e) {
            Log.e(TAG, "Statistika alınarkən xəta: " + e.getMessage());
        } finally {
            if (db != null) db.close();
        }

        return stats;
    }

    // Cədvəlin bütün məlumatlarını sil
    public boolean truncateTable(String tableName) {
        SQLiteDatabase db = null;

        try {
            db = this.getWritableDatabase();
            db.execSQL("DELETE FROM " + tableName);
            db.execSQL("DELETE FROM sqlite_sequence WHERE name='" + tableName + "'");

            Log.d(TAG, "Cədvəl təmizləndi: " + tableName);
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Cədvəl təmizlənərkən xəta: " + e.getMessage());
            return false;
        } finally {
            if (db != null) db.close();
        }
    }

    // Cədvəldəki məlumat sayını gətir
    public int getRowCount(String tableName) {
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + tableName, null);

            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }

        } catch (Exception e) {
            Log.e(TAG, "Sətir sayı alınarkən xəta: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        return 0;
    }

    // SQL əmrini yoxla
    public String validateSQL(String sql) {
        String normalized = sql.trim();

        if (normalized.isEmpty()) {
            return "Xəta: Boş SQL sorğusu";
        }

        String lowerSql = normalized.toLowerCase();
        String[] dangerousPatterns = {
                ";--", "/*", "*/", "xp_", "exec(", "union select", "drop database",
                "shutdown", "--", "/*", "*/", "@@", "char(", "nchar(", "varchar(",
                "nvarchar(", "alter database", "1=1", "or 1=", "waitfor delay"
        };

        for (String pattern : dangerousPatterns) {
            if (lowerSql.contains(pattern)) {
                return "Xəta: Təhlükəli SQL əmri aşkar edildi";
            }
        }

        return normalized;
    }

    // Sütun tiplərini gətir
    public Map<String, String> getColumnTypes(String tableName) {
        Map<String, String> columnTypes = new HashMap<>();
        SQLiteDatabase db = null;
        Cursor cursor = null;

        try {
            db = this.getReadableDatabase();
            cursor = db.rawQuery("PRAGMA table_info(" + tableName + ")", null);

            while (cursor != null && cursor.moveToNext()) {
                String columnName = cursor.getString(1);
                String columnType = cursor.getString(2);
                columnTypes.put(columnName, columnType);
            }

        } catch (Exception e) {
            Log.e(TAG, "Sütun tipləri alınarkən xəta: " + e.getMessage());
        } finally {
            if (cursor != null) cursor.close();
            if (db != null) db.close();
        }

        return columnTypes;
    }

    // SQL əmrini test et
    public String testSQLQuery(String sql) {
        try {
            List<List<String>> result = executeQuery(sql);
            if (result.isEmpty()) {
                return "Sorğu boş nəticə verdi";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Sorğu uğurlu!\n");
            sb.append("Tapılan sətir sayı: ").append(result.size() - 1).append("\n");

            if (result.size() > 1 && result.get(0).size() > 0) {
                sb.append("Sütun sayı: ").append(result.get(0).size()).append("\n");
            }

            return sb.toString();

        } catch (Exception e) {
            return "Xəta: " + e.getMessage();
        }
    }
}