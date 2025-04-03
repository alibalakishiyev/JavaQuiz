package com.ali.chatBot;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class FirebaseChatHelper {
    private static final String TAG = "FirebaseChatHelper";
    private final DatabaseReference databaseRef;
    private final FirebaseAuth firebaseAuth;
    private static final String BOT_ID = "bot_001";
    private ChildEventListener activeListener; // Track active listener

    public interface MessageCallback {
        void onSuccess();
        void onError(String error);
    }

    public interface MessageListener {
        void onMessageReceived(ChatMessage message);
        void onError(String error);
    }

    public FirebaseChatHelper() {
        this.databaseRef = FirebaseDatabase.getInstance().getReference("chats");
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    public void sendMessage(String roomId, String text, final MessageCallback callback) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();

        if (currentUser == null) {
            callback.onError("İstifadəçi girişi yoxdur");
            return;
        }

        String currentUserId = currentUser.getUid();
        DatabaseReference messagesRef = databaseRef.child("messages").child(roomId);
        String messageId = messagesRef.push().getKey();

        if (messageId == null) {
            callback.onError("Mesaj ID yaradıla bilmədi");
            return;
        }

        ChatMessage message = new ChatMessage(
                currentUserId,
                text != null ? text : "",
                "text",
                System.currentTimeMillis(),
                false
        );

        messagesRef.child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "İstifadəçi mesajı uğurla göndərildi");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Mesaj göndərilmədi: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    public void sendBotMessage(String roomId, String text, final MessageCallback callback) {
        DatabaseReference messagesRef = databaseRef.child("messages").child(roomId);
        String messageId = messagesRef.push().getKey();

        if (messageId == null) {
            callback.onError("Mesaj ID yaradıla bilmədi");
            return;
        }

        ChatMessage message = new ChatMessage(
                BOT_ID,
                text != null ? text : "",
                "text",  // message type
                System.currentTimeMillis(),
                true     // isUserMessage
        );

        messagesRef.child(messageId).setValue(message)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Bot mesajı uğurla göndərildi");
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Bot mesajı göndərilmədi: " + e.getMessage());
                    callback.onError(e.getMessage());
                });
    }

    public void listenForMessages(String roomId, final MessageListener listener) {
        DatabaseReference roomRef = databaseRef.child("messages").child(roomId);

        // Remove previous listener if exists
        removeListeners(roomId);

        // First check if room exists
        roomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    listener.onError("Söhbət otağı tapılmadı");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                listener.onError(error.getMessage());
            }
        });

        // Create new listener
        activeListener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                try {
                    ChatMessage message = snapshot.getValue(ChatMessage.class);
                    if (message == null) {
                        Log.w(TAG, "Alınan mesaj null-dır");
                        listener.onError("Yanlış mesaj formatı");
                        return;
                    }
                    listener.onMessageReceived(message);
                } catch (Exception e) {
                    Log.e(TAG, "Mesaj emal edilərkən xəta: " + e.getMessage());
                    listener.onError("Mesaj emal edilə bilmədi");
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Implement if needed
            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                // Implement if needed
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                // Implement if needed
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Dinləyici ləğv edildi: " + error.getMessage());
                listener.onError(error.getMessage());
            }
        };

        roomRef.addChildEventListener(activeListener);
    }

    public void removeListeners(String roomId) {
        if (activeListener != null) {
            try {
                DatabaseReference roomRef = databaseRef.child("messages").child(roomId);
                roomRef.removeEventListener(activeListener);
                Log.d(TAG, "Listeners successfully removed from room: " + roomId);
            } catch (Exception e) {
                Log.e(TAG, "Error removing listeners: " + e.getMessage());
            } finally {
                activeListener = null;
            }
        }
    }
}