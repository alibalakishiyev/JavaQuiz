package com.ali.chatBot;


import android.content.Context;

import okhttp3.MediaType;


public class ChatBotHelper {
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    private final Context context;
    private final ChatMessageListener listener;
    private final String apiKey;

    public interface ChatMessageListener {
        void onMessageAdded(ChatMessage message);
        void onError(String errorMessage);
    }

    public ChatBotHelper(Context context, String apiKey, ChatMessageListener listener) {
        this.context = context;
        this.apiKey = apiKey;
        this.listener = listener;
    }


}
