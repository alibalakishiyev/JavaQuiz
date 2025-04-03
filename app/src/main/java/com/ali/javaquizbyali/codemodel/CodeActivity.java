package com.ali.javaquizbyali.codemodel;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.ali.systemIn.R;
import org.codehaus.janino.SimpleCompiler;

import java.lang.reflect.Method;

public class CodeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_code);

        EditText codeInput = findViewById(R.id.codeInput);
        Button runCodeButton = findViewById(R.id.runCodeButton);
        TextView outputResult = findViewById(R.id.outputResult);
        Button backButton = findViewById(R.id.backButton);

        runCodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String userCode = codeInput.getText().toString();
                String result = runJavaCode(userCode);
                outputResult.setText(result);
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CodeActivity.this, Task.class);
                startActivity(intent);
                finish();
            }
        });
    }

    private String runJavaCode(String userCode) {
        String classTemplate =
                "public class DynamicClass {" +
                        "    public static String run() {" +
                        "        try {" +
                        "            " + userCode +
                        "        } catch (Exception e) {" +
                        "            return \"Xəta: \" + e.getMessage();" +
                        "        }" +
                        "    }" +
                        "}";

        try {
            SimpleCompiler compiler = new SimpleCompiler();
            compiler.cook(classTemplate);
            Class<?> dynamicClass = compiler.getClassLoader().loadClass("DynamicClass");
            Method method = dynamicClass.getMethod("run");
            return (String) method.invoke(null);
        } catch (Exception e) {
            return "Xəta: " + e.getMessage();
        }
    }
}
