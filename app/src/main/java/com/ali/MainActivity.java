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
//import com.ali.postgresql.SplashPostgreSqActivity;
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

    ImageButton javamain, aboutcard, postgresql, pymain, aimain, linuxMain, helpButton, closeHelpButton;
    public static int checked;
    private AdView mAdView;
    private InterstitialAd mInterstitialAd;
    private long lastAdShownTime = 0;
    private final long AD_INTERVAL = 60000;
    private ImageView userIcon;
    private FirebaseAuth fAuth;
    private FirebaseFirestore fStore;
    private FirebaseChatHelper firebaseChatHelper;
    private String chatRoomId = "main_bot_chat";
    private List<ChatMessage> chatMessages = new ArrayList<>();
    private static final String CHAT_HISTORY_KEY = "chat_history";
    private ChatAdapter chatAdapter;
    private RecyclerView chatRecyclerView;
    private EditText messageInput;
    private Button sendButton;
    private RelativeLayout chatContainer;
    private boolean isFirstTime = true;
    private static final String PREF_USER_LOGGED_IN = "user_logged_in";
    private boolean wasUserLoggedIn = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();
        firebaseChatHelper = new FirebaseChatHelper();
        initializeViews();
        initializeChatAdapter();
        loadInterstitialAd();
        showInterstitialAd();
        loadChatHistory();


        SharedPreferences sharedPreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
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
        switch (checked) {
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

        // İstifadəçinin əvvəllər giriş edib-etmədiyini yoxlayaq
        SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
        wasUserLoggedIn = prefs.getBoolean(PREF_USER_LOGGED_IN, false);

        // İndiki giriş statusunu yoxlayaq
        boolean isCurrentlyLoggedIn = fAuth.getCurrentUser() != null;

        // Əgər istifadəçi çıxış edibsə, tarixçəni təmizləyək
        if (wasUserLoggedIn && !isCurrentlyLoggedIn) {
            clearChatHistory();
        }

        // Yeni giriş statusunu saxlayaq
        prefs.edit().putBoolean(PREF_USER_LOGGED_IN, isCurrentlyLoggedIn).apply();


        ImageButton closeChatButton = findViewById(R.id.closeChatButton);

        // Help button klik hadisəsi
        helpButton.setOnClickListener(v -> toggleChatVisibility());

        // Mesaj göndər düyməsi
        sendButton.setOnClickListener(v -> sendUserMessage());

        // Chat bağlama düyməsi
        closeChatButton.setOnClickListener(v -> {
            chatContainer.setVisibility(View.GONE);
            // Listenerləri silin AMMA mesajları silməyin
            firebaseChatHelper.removeListeners(chatRoomId);
        });

        javamain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadInterstitialAd();
                startActivity(new Intent(MainActivity.this, SplashJavaActivity.class));

            }
        });

        postgresql.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadInterstitialAd();
                startActivity(new Intent(MainActivity.this, SplashPostgreSqActivity.class));

            }
        });

        pymain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadInterstitialAd();
                startActivity(new Intent(MainActivity.this, SplashPyActivity.class));

            }
        });

        aimain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadInterstitialAd();
                startActivity(new Intent(MainActivity.this, SplashAiActivity.class));

            }
        });

        linuxMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadInterstitialAd();
                startActivity(new Intent(MainActivity.this, SplashLinuxActivity.class));

            }
        });


        aboutcard = findViewById(R.id.aboutCard);
        aboutcard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, AboutActivity.class));

            }
        });

        // İstifadəçi İkonu
        userIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // PopupMenu yarat
                PopupMenu popupMenu = new PopupMenu(MainActivity.this, userIcon);
                popupMenu.getMenuInflater().inflate(R.menu.user_menu, popupMenu.getMenu());

                // **Anonim istifadəçi yoxlanışı**
                if (fAuth.getCurrentUser() == null) {
                    // Anonim istifadəçidirsə, yalnız Login və Sign Up göstər
                    popupMenu.getMenu().findItem(R.id.menu_profile).setVisible(false);
                    popupMenu.getMenu().findItem(R.id.menu_logout).setVisible(false);
                    popupMenu.getMenu().findItem(R.id.menu_login).setVisible(true);
                    popupMenu.getMenu().findItem(R.id.menu_signup).setVisible(true);
                } else {
                    // İstifadəçi daxil olubsa, yalnız Profil və Logout göstər
                    popupMenu.getMenu().findItem(R.id.menu_profile).setVisible(true);
                    popupMenu.getMenu().findItem(R.id.menu_logout).setVisible(true);
                    popupMenu.getMenu().findItem(R.id.menu_login).setVisible(false);
                    popupMenu.getMenu().findItem(R.id.menu_signup).setVisible(false);
                }

                // PopupMenu itemlərinə klik hadisəsi
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (item.getItemId() == R.id.menu_profile) {
                            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                            return true;
                        } else if (item.getItemId() == R.id.menu_logout) {

                            // Çıxışdan əvvəl tarixçəni saxlama flagini sıfırlayaq
                            SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
                            prefs.edit().putBoolean("keep_history", false).apply();


                            fAuth.signOut();
                            clearChatHistory(); // Çıxış edildikdə dərhal sil
                            startActivity(new Intent(MainActivity.this, Login.class));
                            Toast.makeText(MainActivity.this, "Uğurla çıxış edildi!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity.this, Login.class));
                            userIcon.setVisibility(View.VISIBLE);
                            return true;
                        } else if (item.getItemId() == R.id.menu_login) {
                            startActivity(new Intent(MainActivity.this, Login.class));
                            return true;
                        } else if (item.getItemId() == R.id.menu_signup) {
                            startActivity(new Intent(MainActivity.this, Register.class));
                            return true;
                        }
                        return false;
                    }
                });

                // PopupMenu göstər
                popupMenu.show();
            }
        });

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        getOnBackPressedDispatcher().addCallback(this, callback);
    }

    private void initializeViews() {
        javamain = findViewById(R.id.javamain);
        pymain = findViewById(R.id.pymain);
        aimain = findViewById(R.id.aimain);
        postgresql = findViewById(R.id.postgresql);
        linuxMain = findViewById(R.id.linuxMain);
        aboutcard = findViewById(R.id.aboutCard);
        userIcon = findViewById(R.id.userIcon);
        helpButton = findViewById(R.id.helpButton);
        closeHelpButton = findViewById(R.id.closeChatButton);
        chatContainer = findViewById(R.id.chatContainer);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        mAdView = findViewById(R.id.adView);
    }

    private void initializeChatAdapter() {
        String currentUserId = fAuth.getCurrentUser() != null ? fAuth.getCurrentUser().getUid() : "";
        chatAdapter = new ChatAdapter(chatMessages, currentUserId);
        chatRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        chatRecyclerView.setAdapter(chatAdapter);
    }

    private void loadChatHistory() {
        SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
        String chatHistoryJson = prefs.getString(CHAT_HISTORY_KEY, null);

        if (chatHistoryJson != null) {
            try {
                Type type = new TypeToken<List<ChatMessage>>() {
                }.getType();
                List<ChatMessage> savedMessages = new Gson().fromJson(chatHistoryJson, type);
                if (savedMessages != null) {
                    chatMessages.clear();
                    chatMessages.addAll(savedMessages);
                    // Adapter artıq inisializasiya edilib, birbaşa notify edə bilərik
                    chatAdapter.notifyDataSetChanged();
                }
            } catch (Exception e) {
                Log.e(TAG, "Chat tarixçəsini yükləyərkən xəta", e);
            }
        }
    }

    private void saveChatHistory() {
        SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        String chatHistoryJson = new Gson().toJson(chatMessages);
        editor.putString(CHAT_HISTORY_KEY, chatHistoryJson);
        editor.apply();
    }


    private void toggleChatVisibility() {
        if (chatContainer.getVisibility() == View.VISIBLE) {
            chatContainer.setVisibility(View.GONE);
            firebaseChatHelper.removeListeners(chatRoomId);

            // Chat bağlananda tarixçəni qorumaq üçün flagi true edirik
            SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
            prefs.edit().putBoolean("keep_history", true).apply();
        } else {
            chatContainer.setVisibility(View.VISIBLE);
            setupChat();
        }
    }

    private void setupChat() {
        // Adapter null deyilsə yeniləyin
        if (chatAdapter != null) {
            chatAdapter.notifyDataSetChanged();
        }

        // Yalnız ilk dəfə və boş olduqda salam mesajı əlavə edin
        if (isFirstTime && chatMessages.isEmpty()) {
            addMessage(new ChatMessage("bot_001", "Salam! Sualınızı yaza bilərsiniz.",
                    "text", System.currentTimeMillis(), false));
            isFirstTime = false;
        }

        // Firebase listenerləri qoşun
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

    private void addMessage(ChatMessage message) {
        // Əvvəlcə mesajı listə əlavə edin
        chatMessages.add(message);

        // Sonra adapteri yeniləyin
        if (chatAdapter != null) {
            chatAdapter.notifyItemInserted(chatMessages.size() - 1);
            chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
        } else {
            Log.e(TAG, "Adapter null olduğu üçün mesaj əlavə edilə bilmədi");
        }

        // Tarixçəni saxlayın
        saveChatHistory();
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

        // Mesajı yerli olaraq əlavə et
        ChatMessage userMessage = new ChatMessage(
                fAuth.getCurrentUser().getUid(),
                messageText,
                "text",
                System.currentTimeMillis(),
                true
        );
        addMessage(userMessage);
        messageInput.setText("");

        // Firebase-ə göndər
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

    private void clearChatHistory() {
        SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
        prefs.edit()
                .remove(CHAT_HISTORY_KEY)
                .apply();
        chatMessages.clear();

        if (chatAdapter != null) {
            chatAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveChatHistory();
    }

    @Override
    protected void onDestroy() {
        // YALNIZ tətbiq tam bağlananda tarixçəni sil
        if (isFinishing()) {
            SharedPreferences prefs = getSharedPreferences("ChatPrefs", MODE_PRIVATE);
            boolean keepHistory = prefs.getBoolean("keep_history", true);

            if (!keepHistory) {
                clearChatHistory();
            }
        }
        super.onDestroy();
    }



    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, "ca-app-pub-5367924704859976/2123097507", adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                Log.d("MainActivity", "Interstitial reklamı uğurla yükləndi.");
                showInterstitialAd();

                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        Log.d("MainActivity", "Reklam bağlandı. Yeni reklam yüklənir...");
                        mInterstitialAd = null; // Mövcud reklam obyektini null edin.
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
                Log.d("MainActivity", "Interstitial reklamı yüklənmədi: " + loadAdError.getMessage());
            }
        });

    }

    private void showInterstitialAd() {
        long currentTime = System.currentTimeMillis();
        if (mInterstitialAd != null && (currentTime - lastAdShownTime >= AD_INTERVAL)) {
            Log.d("MainActivity", "Reklam göstərilir...");
            mInterstitialAd.show(MainActivity.this);
            lastAdShownTime = currentTime; // Son reklam göstərilmə vaxtını yeniləyin
        } else if (mInterstitialAd == null) {
            Log.d("MainActivity", "Reklam hazır deyil.");
        } else {
            Log.d("MainActivity", "Reklam vaxtı tamamlanmayıb. Gözlənilir...");
        }
    }


    OnBackPressedCallback callback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(MainActivity.this);
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
                        startActivity(new Intent(getApplicationContext(), MainActivity.class));
                        finish();
                    }else{
                        startActivity(new Intent(MainActivity.this, Login.class));
                        finish();
                    }

                }
            });
            materialAlertDialogBuilder.show();
        }
    };


}



