package com.ali.pymain;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ali.systemIn.R;
import com.chaquo.python.PyException;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

public class PythonConsole extends AppCompatActivity {

    private EditText codeInput;
    private TextView outputText;
    private Python py;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_python_console);

        codeInput = findViewById(R.id.codeInput);
        Button runBtn = findViewById(R.id.runBtn);
        outputText = findViewById(R.id.outputText);

        // Python'ı başlat
        if (!Python.isStarted()) {
            // Chaquopy-də Python başlatmaq üçün AndroidPlatform istifadə et
            Python.start(new AndroidPlatform(this));
        }
        py = Python.getInstance();

        runBtn.setOnClickListener(v -> {
            String code = codeInput.getText().toString();
            try {
                // Python kodunu işlətmək üçün executor modulunu çağırırıq
                String result = py.getModule("executor")
                        .callAttr("run_code", code)
                        .toString();
                showResult(result, false);
            } catch (Exception e) {  // Bütün xətaları tutmaq üçün
                showResult("Xəta: " + e.getMessage(), true);
            }
        });


    }

    private void showResult(String text, boolean isError) {
        outputText.setTextColor(isError ? 0xFFFF0000 : 0xFF00AA00);
        outputText.setText(text);
    }
}
