package com.ali;

import static com.ali.quizutility.AboutActivity.MyPREFERENCES;
import static com.ali.user.EditProfile.TAG;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.aimain.SplashAiActivity;
import com.ali.autorized.Login;
import com.ali.autorized.Register;
import com.ali.chatBot.ChatAdapter;
import com.ali.chatBot.ChatMessage;
import com.ali.chatBot.FirebaseChatHelper;
import com.ali.javaquizbyali.SplashJavaActivity;
import com.ali.kali.SplashLinuxActivity;
import com.ali.postgresql.SplashPostgreSqActivity;
import com.ali.pymain.SplashPyActivity;
import com.ali.quizutility.AboutActivity;
import com.ali.systemIn.R;
import com.ali.user.ProfileActivity;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.reflect.TypeToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private ImageButton javamain, aimain, pymain, linuxMain, postgresql, aboutCardBtn, helpButton, closeChatButton;
    private ImageView userIcon;
    private CardView javaCard, aiCard, pythonCard, linuxCard, postgresqlCard, aboutCard;
    private AdView mAdView;
    private InterstitialAd mInterstitialAd;

    // Chat components
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private Button sendButton;
    private RelativeLayout chatContainer;
    private List<ChatMessage> chatMessages = new ArrayList<>();
    private ChatAdapter chatAdapter;
    private FirebaseChatHelper firebaseChatHelper;

    // Firebase
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;

    // Constants
    private static final String CHAT_HISTORY_KEY = "chat_history";
    private static final String PREF_USER_LOGGED_IN = "user_logged_in";
    private final long AD_INTERVAL = 60000;
    private long lastAdShownTime = 0;
    private boolean isFirstTime = true;
    private boolean wasUserLoggedIn = false;
    private String chatRoomId = "main_bot_chat";
    public static int checked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        initializeFirebase();
        initializeViews();
        setupTheme();
        setupClickListeners();
        setupChat();
        loadInterstitialAd();
        loadChatHistory();
        setupBackPressedCallback();

        // Banner reklam
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    private void initializeFirebase() {
        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        firebaseChatHelper = new FirebaseChatHelper();
    }

    private void initializeViews() {
        // Buttons
        javamain = findViewById(R.id.javamain);
        aimain = findViewById(R.id.aimain);
        pymain = findViewById(R.id.pymain);
        linuxMain = findViewById(R.id.linuxMain);
        postgresql = findViewById(R.id.postgresql);
        aboutCardBtn = findViewById(R.id.aboutCardBtn);
        helpButton = findViewById(R.id.helpButton);
        closeChatButton = findViewById(R.id.closeChatButton);
        userIcon = findViewById(R.id.userIcon);

        // Cards
        javaCard = findViewById(R.id.javaCard);
        aiCard = findViewById(R.id.aiCard);
        pythonCard = findViewById(R.id.pythonCard);
        linuxCard = findViewById(R.id.linuxCard);
        postgresqlCard = findViewById(R.id.postgresqlCard);
        aboutCard = findViewById(R.id.aboutCard);

        // Chat
        chatContainer = findViewById(R.id.chatContainer);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);

        // Ad
        mAdView = findViewById(R.id.adView);
    }

    private void setupTheme() {
        SharedPreferences sharedPreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        int default1;
        switch (Configuration.UI_MODE_NIGHT_MASK & AppCompatDelegate.getDefaultNightMode()) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                default1 = 0;
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                default1 = 1;
                break;
            default:
                default1 = 2;
        }
        checked = sharedPreferences.getInt("checked", default1);
        applyTheme(checked);

        // İstifadəçi giriş statusunu yoxla
        SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
        wasUserLoggedIn = prefs.getBoolean(PREF_USER_LOGGED_IN, false);
        boolean isCurrentlyLoggedIn = fAuth.getCurrentUser() != null;

        if (wasUserLoggedIn && !isCurrentlyLoggedIn) {
            clearChatHistory();
        }
        prefs.edit().putBoolean(PREF_USER_LOGGED_IN, isCurrentlyLoggedIn).apply();
    }

    private void applyTheme(int themeMode) {
        switch (themeMode) {
            case 0:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case 1:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case 2:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case 3:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY);
                break;
        }
    }

    private void setupClickListeners() {
        javamain.setOnClickListener(v -> navigateWithAd(SplashJavaActivity.class));
        aimain.setOnClickListener(v -> navigateWithAd(SplashAiActivity.class));
        pymain.setOnClickListener(v -> navigateWithAd(SplashPyActivity.class));
        linuxMain.setOnClickListener(v -> navigateWithAd(SplashLinuxActivity.class));
        postgresql.setOnClickListener(v -> navigateWithAd(SplashPostgreSqActivity.class));
        aboutCardBtn.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, AboutActivity.class)));
        helpButton.setOnClickListener(v -> toggleChatVisibility());
        closeChatButton.setOnClickListener(v -> closeChat());
        sendButton.setOnClickListener(v -> sendUserMessage());
        userIcon.setOnClickListener(this::showUserMenu);
    }

    private void navigateWithAd(Class<?> targetActivity) {
        loadInterstitialAd();
        startActivity(new Intent(MainActivity.this, targetActivity));
    }

    private void showUserMenu(View view) {
        PopupMenu popupMenu = new PopupMenu(MainActivity.this, view);
        popupMenu.getMenuInflater().inflate(R.menu.user_menu, popupMenu.getMenu());

        boolean isLoggedIn = fAuth.getCurrentUser() != null;
        popupMenu.getMenu().findItem(R.id.menu_profile).setVisible(isLoggedIn);
        popupMenu.getMenu().findItem(R.id.menu_logout).setVisible(isLoggedIn);
        popupMenu.getMenu().findItem(R.id.menu_login).setVisible(!isLoggedIn);
        popupMenu.getMenu().findItem(R.id.menu_signup).setVisible(!isLoggedIn);

        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_profile) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
            } else if (id == R.id.menu_logout) {
                logout();
            } else if (id == R.id.menu_login) {
                startActivity(new Intent(MainActivity.this, Login.class));
            } else if (id == R.id.menu_signup) {
                startActivity(new Intent(MainActivity.this, Register.class));
            }
            return true;
        });
        popupMenu.show();
    }

    private void logout() {
        SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
        prefs.edit().putBoolean("keep_history", false).apply();
        fAuth.signOut();
        clearChatHistory();
        Toast.makeText(this, "Uğurla çıxış edildi!", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(MainActivity.this, Login.class));
        finish();
    }

    private void setupChat() {
        String currentUserId = fAuth.getCurrentUser() != null ? fAuth.getCurrentUser().getUid() : "";
        chatAdapter = new ChatAdapter(chatMessages, currentUserId);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);

        if (isFirstTime && chatMessages.isEmpty()) {
            addMessage(new ChatMessage("bot_001", "Salam! 👋 Sualınızı yaza bilərsiniz.", "text", System.currentTimeMillis(), false));
            isFirstTime = false;
        }

        firebaseChatHelper.removeListeners(chatRoomId);
        firebaseChatHelper.listenForMessages(chatRoomId, new FirebaseChatHelper.MessageListener() {
            @Override
            public void onMessageReceived(ChatMessage message) {
                runOnUiThread(() -> addMessage(message));
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "Chat error: " + error);
            }
        });
    }

    private void toggleChatVisibility() {
        if (chatContainer.getVisibility() == View.VISIBLE) {
            chatContainer.setVisibility(View.GONE);
            firebaseChatHelper.removeListeners(chatRoomId);
            SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
            prefs.edit().putBoolean("keep_history", true).apply();
        } else {
            chatContainer.setVisibility(View.VISIBLE);
            setupChat();
        }
    }

    private void closeChat() {
        chatContainer.setVisibility(View.GONE);
        firebaseChatHelper.removeListeners(chatRoomId);
    }

    private void sendUserMessage() {
        String messageText = messageInput.getText().toString().trim();
        if (messageText.isEmpty()) {
            Toast.makeText(this, "Mesaj boş ola bilməz", Toast.LENGTH_SHORT).show();
            return;
        }
        if (fAuth.getCurrentUser() == null) {
            Toast.makeText(this, "Mesaj göndərmək üçün daxil olmalısınız", Toast.LENGTH_SHORT).show();
            return;
        }

        ChatMessage userMessage = new ChatMessage(fAuth.getCurrentUser().getUid(), messageText, "text", System.currentTimeMillis(), true);
        addMessage(userMessage);
        messageInput.setText("");

        firebaseChatHelper.sendMessage(chatRoomId, messageText, new FirebaseChatHelper.MessageCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Mesaj uğurla göndərildi");
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Xəta: " + error, Toast.LENGTH_SHORT).show();
                    chatMessages.remove(userMessage);
                    chatAdapter.notifyDataSetChanged();
                });
            }
        });
    }

    private void addMessage(ChatMessage message) {
        chatMessages.add(message);
        if (chatAdapter != null) {
            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
            chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
        }
        saveChatHistory();
    }

    private void loadChatHistory() {
        SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
        String chatHistoryJson = prefs.getString(CHAT_HISTORY_KEY, null);
        if (chatHistoryJson != null) {
            try {
                Type type = new TypeToken<List<ChatMessage>>(){}.getType();
                List<ChatMessage> savedMessages = new Gson().fromJson(chatHistoryJson, type);
                if (savedMessages != null) {
                    chatMessages.clear();
                    chatMessages.addAll(savedMessages);
                    if (chatAdapter != null) chatAdapter.notifyDataSetChanged();
                }
            } catch (Exception e) {
                Log.e(TAG, "Chat tarixçəsini yükləyərkən xəta", e);
            }
        }
    }

    private void saveChatHistory() {
        SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
        prefs.edit().putString(CHAT_HISTORY_KEY, new Gson().toJson(chatMessages)).apply();
    }

    private void clearChatHistory() {
        SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
        prefs.edit().remove(CHAT_HISTORY_KEY).apply();
        chatMessages.clear();
        if (chatAdapter != null) chatAdapter.notifyDataSetChanged();
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, "ca-app-pub-5367924704859976/2123097507", adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                showInterstitialAd();
                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        mInterstitialAd = null;
                        loadInterstitialAd();
                    }
                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        Log.d("MainActivity", "Reklam göstərilmədi: " + adError.getMessage());
                    }
                    @Override
                    public void onAdShowedFullScreenContent() {
                        Log.d("MainActivity", "Reklam göstərilir.");
                    }
                });
            }
            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                mInterstitialAd = null;
            }
        });
    }

    private void showInterstitialAd() {
        long currentTime = System.currentTimeMillis();
        if (mInterstitialAd != null && (currentTime - lastAdShownTime >= AD_INTERVAL)) {
            mInterstitialAd.show(MainActivity.this);
            lastAdShownTime = currentTime;
        }
    }

    private void setupBackPressedCallback() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                new MaterialAlertDialogBuilder(MainActivity.this)
                        .setTitle("Çıxış")
                        .setMessage("Proqramdan çıxmaq istədiyinizə əminsiniz?")
                        .setNegativeButton("Xeyr", (dialog, i) -> dialog.dismiss())
                        .setPositiveButton("Bəli", (dialog, i) -> {
                            if (fAuth.getCurrentUser() != null) {
                                finishAffinity();
                            } else {
                                startActivity(new Intent(MainActivity.this, Login.class));
                                finish();
                            }
                        })
                        .show();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveChatHistory();
    }

    @Override
    protected void onDestroy() {
        if (isFinishing()) {
            SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
            if (!prefs.getBoolean("keep_history", true)) {
                clearChatHistory();
            }
        }
        super.onDestroy();
    }
}