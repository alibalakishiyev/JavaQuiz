package com.ali.kali.comandlist;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ali.kali.LinuxMain;
import com.ali.systemIn.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class CommandList extends AppCompatActivity {


    private LinearLayout wordContainer;
    private AdView mAdView7;
    private FirebaseAuth fAuth;
    private String coursePath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_command_list);

        wordContainer = findViewById(R.id.wordContainer);

        coursePath = getIntent().getStringExtra("jsonFile");
        if (coursePath != null) {
            loadWordsFromJson(coursePath); // JSON və path birdir
        }

        fAuth = FirebaseAuth.getInstance();


        mAdView7 = findViewById(R.id.adView7);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView7.loadAd(adRequest);

        getOnBackPressedDispatcher().addCallback(this, callback);

    }

    private void loadWordsFromJson(String fileName) {
        try {
            AssetManager assetManager = getAssets();
            InputStream is = assetManager.open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            int readBytes = is.read(buffer);
            is.close();

            if (readBytes != size) {
                Toast.makeText(this, "Error reading full file", Toast.LENGTH_SHORT).show();
                return;
            }

            String jsonString = new String(buffer, "UTF-8");

            Gson gson = new GsonBuilder().create();
            LinuxCommandsWrapper wrapper = gson.fromJson(jsonString, LinuxCommandsWrapper.class);

            if (wrapper == null || wrapper.getLinux_commands() == null) {
                Toast.makeText(this, "Parsed JSON is empty or invalid", Toast.LENGTH_LONG).show();
                return;
            }

            for (CommandCategory category : wrapper.getLinux_commands()) {
                TextView categoryText = new TextView(this);
                categoryText.setText(category.getCategory());
                categoryText.setTextSize(24f);
                categoryText.setTextColor(Color.parseColor("#00897B")); // Teal
                categoryText.setPadding(16, 40, 16, 20);
                categoryText.setTypeface(null, Typeface.BOLD);
                wordContainer.addView(categoryText);

                for (LinuxCommands cmd : category.getCommands()) {
                    TextView commandText = new TextView(this);
                    commandText.setText("➤ " + cmd.getCommand());
                    commandText.setTextSize(22f);
                    commandText.setTextColor(Color.parseColor("#37474F")); // Blue Grey
                    commandText.setPadding(32, 20, 16, 10);
                    commandText.setTypeface(null, Typeface.BOLD);
                    commandText.setBackgroundColor(Color.parseColor("#ECEFF1")); // Light background

                    TextView descriptionText = new TextView(this);
                    descriptionText.setText("💬 " + cmd.getDescription());
                    descriptionText.setTextSize(18f);
                    descriptionText.setTextColor(Color.parseColor("#455A64"));
                    descriptionText.setPadding(48, 10, 16, 5);
                    descriptionText.setVisibility(View.GONE);

                    TextView exampleText = new TextView(this);
                    exampleText.setText("📘 Example: " + cmd.getExample());
                    exampleText.setTextSize(18f);
                    exampleText.setTextColor(Color.parseColor("#607D8B"));
                    exampleText.setPadding(48, 5, 16, 5);
                    exampleText.setVisibility(View.GONE);

                    TextView detailsText = new TextView(this);
                    detailsText.setText("ℹ️ " + cmd.getDetails());
                    detailsText.setTextSize(18f);
                    detailsText.setTextColor(Color.parseColor("#78909C"));
                    detailsText.setPadding(48, 5, 16, 20);
                    detailsText.setVisibility(View.GONE);

                    commandText.setOnClickListener(v -> {
                        int newVisibility = (descriptionText.getVisibility() == View.GONE) ? View.VISIBLE : View.GONE;
                        descriptionText.setVisibility(newVisibility);
                        exampleText.setVisibility(newVisibility);
                        detailsText.setVisibility(newVisibility);
                    });

                    wordContainer.addView(commandText);
                    wordContainer.addView(descriptionText);
                    wordContainer.addView(exampleText);
                    wordContainer.addView(detailsText);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "File not found or IO error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to parse JSON: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }



    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(CommandList.this);
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
                    startActivity(new Intent(getApplicationContext(), LinuxMain.class));
                    finish();
                }
            });
            materialAlertDialogBuilder.show();
        }
    };


}

