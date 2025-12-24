package com.ali.postgresql.taskPostgreSqlManager;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.ali.MainActivity;
import com.ali.autorized.Login;
import com.ali.postgresql.PostgreSqlMain;
import com.ali.systemIn.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Map;

public class PostgreSqlTaskDetail extends AppCompatActivity {
    private static final String TAG = "SqlTaskDetail";
    private TextView taskTitle, taskDescription, initialQueryText;
    private EditText sqlQueryInput;
    private Button executeButton, showTablesButton, checkButton, showSolutionButton;
    private Button adminModeButton, resetButton, newTableButton;
    private LinearLayout resultContainer;
    private HorizontalScrollView horizontalScrollView;
    private ScrollView verticalScrollView;
    private TextView scrollHint;
    private int taskId;
    private String correctSolution;
    private VirtualDatabaseHelper dbHelper;
    private boolean isAdminMode = false;

    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;

    private AdView SqlAdView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postgre_sql_task_detail);

        Log.d(TAG, "Detail Activity yaradıldı");

        findViews();
        dbHelper = new VirtualDatabaseHelper(this);
        loadIntentData();
        setupButtonListeners();
        showTableList();
    }

    private void findViews() {
        taskTitle = findViewById(R.id.taskTitle);
        taskDescription = findViewById(R.id.taskDescription);
        initialQueryText = findViewById(R.id.initialQueryText);
        sqlQueryInput = findViewById(R.id.sqlQueryInput);
        executeButton = findViewById(R.id.executeButton);
        showTablesButton = findViewById(R.id.showTablesButton);
        checkButton = findViewById(R.id.checkButton);
        showSolutionButton = findViewById(R.id.showSolutionButton);
        adminModeButton = findViewById(R.id.adminModeButton);
        resetButton = findViewById(R.id.resetButton);
        newTableButton = findViewById(R.id.newTableButton);
        resultContainer = findViewById(R.id.resultContainer);
        horizontalScrollView = findViewById(R.id.horizontalScrollView);
        verticalScrollView = findViewById(R.id.verticalScrollView);
        scrollHint = findViewById(R.id.scrollHint);
    }

    private void loadIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            taskId = intent.getIntExtra("taskId", 0);
            String title = intent.getStringExtra("title");
            String description = intent.getStringExtra("description");
            String initialQuery = intent.getStringExtra("initialQuery");
            correctSolution = intent.getStringExtra("solution");

            if (title != null) taskTitle.setText(taskId + ". " + title);
            if (description != null) taskDescription.setText(description);
            if (initialQuery != null) {
                initialQueryText.setText(initialQuery);
                sqlQueryInput.setText(initialQuery);
            }
        }

        fAuth = FirebaseAuth.getInstance();
    }

    private void setupButtonListeners() {
        executeButton.setOnClickListener(v -> executeSqlQuery());
        showTablesButton.setOnClickListener(v -> showTableList());
        checkButton.setOnClickListener(v -> checkTaskSolution());
        showSolutionButton.setOnClickListener(v -> showSolution());
        adminModeButton.setOnClickListener(v -> toggleAdminMode());
        resetButton.setOnClickListener(v -> resetDatabase());
        newTableButton.setOnClickListener(v -> showCreateTableDialog());

        findViewById(R.id.btnSelectAll).setOnClickListener(v -> {
            sqlQueryInput.setText("SELECT * FROM employees;");
            executeSqlQuery();
        });

        findViewById(R.id.btnWhere).setOnClickListener(v -> {
            sqlQueryInput.setText("SELECT * FROM employees WHERE salary > 3000;");
            executeSqlQuery();
        });

        findViewById(R.id.btnJoin).setOnClickListener(v -> {
            sqlQueryInput.setText("SELECT e.name, d.budget FROM employees e JOIN departments d ON e.department = d.name;");
            executeSqlQuery();
        });

        findViewById(R.id.btnGroupBy).setOnClickListener(v -> {
            sqlQueryInput.setText("SELECT department, AVG(salary) FROM employees GROUP BY department;");
            executeSqlQuery();
        });

        SqlAdView = findViewById(R.id.SqlAdView);
        AdRequest adRequest = new AdRequest.Builder().build();
        SqlAdView.loadAd(adRequest);

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void executeSqlQuery() {
        String userQuery = sqlQueryInput.getText().toString().trim();

        if (TextUtils.isEmpty(userQuery)) {
            Toast.makeText(this, "SQL sorğusu daxil edin", Toast.LENGTH_SHORT).show();
            return;
        }

        List<List<String>> result = dbHelper.executeQuery(userQuery);
        displayQueryResult(result, "Sorğu Nəticəsi");
    }

    private void displayQueryResult(List<List<String>> result, String title) {
        resultContainer.removeAllViews();
        scrollHint.setVisibility(View.VISIBLE);

        if (result == null || result.isEmpty()) {
            showMessage("Nəticə yoxdur", R.color.gray_medium);
            scrollHint.setVisibility(View.GONE);
            return;
        }

        // Başlıq
        TextView resultTitle = createTextView(title, 18, Typeface.BOLD, R.color.primary);
        resultContainer.addView(resultTitle);

        // Əgər xəta mesajıdırsa
        if (result.size() == 1 && result.get(0).size() == 2 &&
                result.get(0).get(0).equals("Xəta")) {
            showMessage(result.get(0).get(1), R.color.red);
            scrollHint.setVisibility(View.GONE);
            return;
        }

        // Nəticə sayını göstər
        if (result.size() > 1) {
            TextView resultCount = createTextView(
                    "Nəticə: " + (result.size() - 1) + " sətir tapıldı",
                    14, Typeface.BOLD, R.color.gray_dark
            );
            resultContainer.addView(resultCount);
        }

        // Cədvəli dinamik yarat
        displayTableWithDynamicWidth(result);
    }

    private void displayTableWithDynamicWidth(List<List<String>> result) {
        if (result.isEmpty()) return;

        List<String> headers = result.get(0);
        int columnCount = headers.size();

        // Cədvəl yarat
        TableLayout table = new TableLayout(this);
        table.setStretchAllColumns(false);
        table.setShrinkAllColumns(false);
        table.setPadding(4, 4, 4, 4);
        table.setBackgroundColor(ContextCompat.getColor(this, R.color.gray_light));

        // Cədvəl üçün minimum en
        int minTableWidth = columnCount * 200; // Hər sütun üçün 200dp
        table.setMinimumWidth(minTableWidth);

        // Header row
        TableRow headerRow = new TableRow(this);
        headerRow.setBackgroundColor(ContextCompat.getColor(this, R.color.primary));

        for (String header : headers) {
            TextView headerCell = createTableCell(header, Typeface.BOLD,
                    R.color.white, R.color.primary_dark, 12, 12);
            headerCell.setMinWidth(180); // Header minimum eni
            headerRow.addView(headerCell);
        }
        table.addView(headerRow);

        // Data rows
        for (int i = 1; i < result.size() && i < 100; i++) { // Maksimum 100 sətir
            TableRow dataRow = new TableRow(this);
            dataRow.setBackgroundColor(i % 2 == 0 ?
                    ContextCompat.getColor(this, R.color.white) :
                    ContextCompat.getColor(this, R.color.gray_very_light));

            List<String> rowData = result.get(i);
            for (int j = 0; j < rowData.size(); j++) {
                String cell = rowData.get(j);
                TextView dataCell = createTableCell(cell, Typeface.NORMAL,
                        R.color.black, android.R.color.transparent, 12, 8);
                dataCell.setMinWidth(180); // Data cell minimum eni

                // Uzun mətnləri qısalt
                if (cell.length() > 25) {
                    String shortText = cell.substring(0, 22) + "...";
                    dataCell.setText(shortText);

                    // Tooltip üçün
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        dataCell.setTooltipText(cell);
                    } else {
                        dataCell.setOnLongClickListener(v -> {
                            Toast.makeText(PostgreSqlTaskDetail.this,
                                    cell, Toast.LENGTH_LONG).show();
                            return true;
                        });
                    }
                }
                dataRow.addView(dataCell);
            }
            table.addView(dataRow);
        }

        // Cədvəli əlavə et
        resultContainer.addView(table);

        // Çox sətur varsa mesaj
        if (result.size() > 100) {
            TextView moreText = createTextView(
                    "... və " + (result.size() - 100) + " sətir daha",
                    12, Typeface.ITALIC, R.color.gray_dark
            );
            moreText.setPadding(16, 8, 16, 16);
            resultContainer.addView(moreText);
        }

        // Scroll bar göstəricisini yoxla
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        if (minTableWidth > screenWidth) {
            scrollHint.setText("↔️ " + columnCount + " sütun var. Sağa-sola sürüşdürün");
            scrollHint.setVisibility(View.VISIBLE);
        } else {
            scrollHint.setVisibility(View.GONE);
        }

        // Scroll-u sıfırla
        horizontalScrollView.post(() -> {
            horizontalScrollView.scrollTo(0, 0);
            verticalScrollView.scrollTo(0, 0);
        });
    }

    private void showTableList() {
        List<String> tables = dbHelper.getTableList();

        resultContainer.removeAllViews();
        scrollHint.setVisibility(View.GONE);

        TextView title = createTextView("📋 Mövcud Cədvəllər", 18, Typeface.BOLD, R.color.primary);
        resultContainer.addView(title);

        // Statistika
        Map<String, Object> stats = dbHelper.getDatabaseStats();
        TextView statsText = createTextView(
                "Cədvəl sayı: " + stats.get("table_count") +
                        " | Ümumi sətir: " + stats.get("total_rows"),
                12, Typeface.NORMAL, R.color.gray_dark
        );
        resultContainer.addView(statsText);

        for (String tableName : tables) {
            // Cədvəl kartı
            LinearLayout tableCard = new LinearLayout(this);
            tableCard.setOrientation(LinearLayout.VERTICAL);
            tableCard.setBackground(ContextCompat.getDrawable(this, R.drawable.card_background));
            tableCard.setPadding(16, 12, 16, 12);
            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            cardParams.setMargins(0, 0, 0, 8);
            tableCard.setLayoutParams(cardParams);

            // Cədvəl adı
            TextView tableNameView = createTextView("📊 " + tableName, 16, Typeface.BOLD, R.color.black);
            tableNameView.setOnClickListener(v -> showTableDetails(tableName));
            tableCard.addView(tableNameView);

            // Action buttons
            LinearLayout buttonLayout = new LinearLayout(this);
            buttonLayout.setOrientation(LinearLayout.HORIZONTAL);

            Button viewBtn = createSmallButton("Göstər", v -> showTableStructure(tableName));
            Button selectBtn = createSmallButton("SELECT *", v -> {
                sqlQueryInput.setText("SELECT * FROM " + tableName + " LIMIT 10");
                executeSqlQuery();
            });
            Button dropBtn = createSmallButton("Sil", v -> dropTable(tableName));
            dropBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.red));

            buttonLayout.addView(viewBtn);
            buttonLayout.addView(selectBtn);
            if (isAdminMode) {
                buttonLayout.addView(dropBtn);
            }

            tableCard.addView(buttonLayout);
            resultContainer.addView(tableCard);
        }
    }

    private void showTableDetails(String tableName) {
        resultContainer.removeAllViews();
        scrollHint.setVisibility(View.VISIBLE);

        TextView title = createTextView("🔧 Cədvəl: " + tableName, 18, Typeface.BOLD, R.color.primary);
        resultContainer.addView(title);

        // Nümunə sorğular
        LinearLayout quickQueries = new LinearLayout(this);
        quickQueries.setOrientation(LinearLayout.VERTICAL);

        String[] sampleQueries = {
                "SELECT * FROM " + tableName + " LIMIT 5",
                "SELECT COUNT(*) FROM " + tableName,
                "SELECT * FROM " + tableName + " ORDER BY 1 DESC LIMIT 5"
        };

        for (String query : sampleQueries) {
            Button queryBtn = createSmallButton(query, v -> {
                sqlQueryInput.setText(query);
                executeSqlQuery();
            });
            queryBtn.setTextSize(10);
            quickQueries.addView(queryBtn);
        }

        resultContainer.addView(quickQueries);

        // Struktur cədvəli
        List<List<String>> structure = dbHelper.getTableStructure(tableName);
        displayTableWithDynamicWidth(structure);

        // Nümunə məlumatlar
        List<List<String>> sampleData = dbHelper.getSampleData(tableName, 5);
        if (sampleData.size() > 1) {
            TextView sampleTitle = createTextView("Nümunə Məlumatlar (ilk 5)", 16, Typeface.BOLD, R.color.primary);
            sampleTitle.setPadding(0, 16, 0, 8);
            resultContainer.addView(sampleTitle);
            displayTableWithDynamicWidth(sampleData);
        }
    }

    private void showTableStructure(String tableName) {
        List<List<String>> structure = dbHelper.getTableStructure(tableName);
        displayTableWithDynamicWidth(structure);
    }

    private void checkTaskSolution() {
        String userQuery = sqlQueryInput.getText().toString().trim();

        if (TextUtils.isEmpty(userQuery)) {
            Toast.makeText(this, "SQL sorğusu daxil edin", Toast.LENGTH_SHORT).show();
            return;
        }

        if (correctSolution == null || correctSolution.isEmpty()) {
            Toast.makeText(this, "Həll məlumatı yoxdur", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d(TAG, "User query: " + userQuery);
        Log.d(TAG, "Correct solution: " + correctSolution);

        List<List<String>> userResult = dbHelper.executeQuery(userQuery);
        List<List<String>> correctResult = dbHelper.executeQuery(correctSolution);

        boolean isCorrect = compareResults(userResult, correctResult);

        if (isCorrect) {
            Toast.makeText(this, "✅ SQL sorğusu düzgündür!", Toast.LENGTH_SHORT).show();

            Intent resultIntent = new Intent();
            resultIntent.putExtra("taskId", taskId);
            resultIntent.putExtra("completed", true);
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "❌ SQL sorğusu düzgün deyil.", Toast.LENGTH_LONG).show();
            displayComparison(userResult, correctResult);
        }
    }

    private boolean compareResults(List<List<String>> userResult, List<List<String>> correctResult) {
        if (userResult == null || correctResult == null) return false;
        if (userResult.size() != correctResult.size()) return false;

        for (int i = 0; i < userResult.size(); i++) {
            List<String> userRow = userResult.get(i);
            List<String> correctRow = correctResult.get(i);

            if (userRow.size() != correctRow.size()) return false;

            for (int j = 0; j < userRow.size(); j++) {
                if (!userRow.get(j).equals(correctRow.get(j))) {
                    return false;
                }
            }
        }

        return true;
    }

    private void displayComparison(List<List<String>> userResult, List<List<String>> correctResult) {
        resultContainer.removeAllViews();
        scrollHint.setVisibility(View.VISIBLE);

        if (userResult != null && !userResult.isEmpty()) {
            displayTableWithDynamicWidth(userResult);
        }

        if (correctResult != null && !correctResult.isEmpty()) {
            TextView divider = createTextView("═".repeat(50), 14, Typeface.NORMAL, R.color.gray_medium);
            divider.setGravity(android.view.Gravity.CENTER);
            resultContainer.addView(divider);
            displayTableWithDynamicWidth(correctResult);
        }
    }

    private void showSolution() {
        if (correctSolution != null && !correctSolution.isEmpty()) {
            sqlQueryInput.setText(correctSolution);
            List<List<String>> result = dbHelper.executeQuery(correctSolution);
            displayTableWithDynamicWidth(result);
            Toast.makeText(this, "Həll göstərildi və icra edildi", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Həll məlumatı yoxdur", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleAdminMode() {
        isAdminMode = !isAdminMode;

        if (isAdminMode) {
            adminModeButton.setText("🔓 Admin Modu: AÇIQ");
            adminModeButton.setBackgroundColor(ContextCompat.getColor(this, R.color.green));
            resetButton.setVisibility(View.VISIBLE);
            newTableButton.setVisibility(View.VISIBLE);
            Toast.makeText(this, "Admin modu aktiv. Bütün SQL əmrləri icra oluna bilər.", Toast.LENGTH_LONG).show();
        } else {
            adminModeButton.setText("🔒 Admin Modu: BAĞLI");
            adminModeButton.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
            resetButton.setVisibility(View.GONE);
            newTableButton.setVisibility(View.GONE);
            Toast.makeText(this, "Admin modu deaktiv. Yalnız SELECT əmrləri icra oluna bilər.", Toast.LENGTH_SHORT).show();
        }

        showTableList();
    }

    private void resetDatabase() {
        dbHelper.resetDatabase();
        Toast.makeText(this, "Verilənlər bazası sıfırlandı", Toast.LENGTH_SHORT).show();
        showTableList();
    }

    private void showCreateTableDialog() {
        sqlQueryInput.setText("CREATE TABLE students (\n" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    name TEXT NOT NULL,\n" +
                "    grade INTEGER,\n" +
                "    major TEXT\n" +
                ");");
        Toast.makeText(this, "CREATE TABLE nümunəsi əlavə edildi. Admin modunda icra edin.", Toast.LENGTH_LONG).show();
    }

    private void dropTable(String tableName) {
        if (dbHelper.dropTable(tableName)) {
            Toast.makeText(this, "Cədvəl silindi: " + tableName, Toast.LENGTH_SHORT).show();
            showTableList();
        } else {
            Toast.makeText(this, "Cədvəl silinmədi", Toast.LENGTH_SHORT).show();
        }
    }

    // ============ HELPER METHODS ============

    private TextView createTextView(String text, int size, int style, int colorRes) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(size);
        textView.setTypeface(null, style);
        textView.setTextColor(ContextCompat.getColor(this, colorRes));
        textView.setPadding(16, 8, 16, 8);
        return textView;
    }

    private TextView createTableCell(String text, int style, int textColorRes, int bgColorRes, int size, int padding) {
        TextView cell = new TextView(this);
        cell.setText(text);
        cell.setTypeface(null, style);
        cell.setTextColor(ContextCompat.getColor(this, textColorRes));
        cell.setBackgroundColor(ContextCompat.getColor(this, bgColorRes));
        cell.setTextSize(size);
        cell.setPadding(padding, padding, padding, padding);
        cell.setMinWidth(180); // Dinamik minimum en
        return cell;
    }

    private Button createSmallButton(String text, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(10);
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1
        );
        params.setMargins(4, 4, 4, 4);
        button.setLayoutParams(params);
        return button;
    }

    private void showMessage(String message, int bgColorRes) {
        TextView messageView = createTextView(message, 14, Typeface.NORMAL, R.color.black);
        messageView.setBackgroundColor(ContextCompat.getColor(this, bgColorRes));
        messageView.setGravity(android.view.Gravity.CENTER);
        resultContainer.addView(messageView);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(PostgreSqlTaskDetail.this);
            materialAlertDialogBuilder.setTitle(R.string.app_name);
            materialAlertDialogBuilder.setMessage("Are you sure want to exit the app?");
            materialAlertDialogBuilder.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {
                    dialog.dismiss();
                }
            });
            materialAlertDialogBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int i) {

                    if (fAuth.getCurrentUser() != null) {
                        startActivity(new Intent(getApplicationContext(), PostgreSqlMain.class));
                        finish();
                    }else{
                        startActivity(new Intent(PostgreSqlTaskDetail.this, Login.class));
                        finish();
                    }

                }
            });
            materialAlertDialogBuilder.show();
        }
    };

}