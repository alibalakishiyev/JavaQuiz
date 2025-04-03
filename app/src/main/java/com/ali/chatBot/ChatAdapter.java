package com.ali.chatBot;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.systemIn.R;

import java.util.ArrayList;
import java.util.List;


public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {
    private List<ChatMessage> messages;
    private String currentUserId;


    public ChatAdapter(List<ChatMessage> messages, String currentUserId) {
        this.messages = messages != null ? messages : new ArrayList<>();
        this.currentUserId = currentUserId != null ? currentUserId : "";
    }

    public void updateMessages(List<ChatMessage> newMessages) {
        this.messages = newMessages != null ? newMessages : new ArrayList<>();
        notifyDataSetChanged();
    }



    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        ChatMessage message = messages.get(position);
        holder.messageText.setText(message.getText()); // getText() istifadə edin

        boolean isUserMessage = currentUserId.equals(message.getSenderId());
        holder.senderText.setText(isUserMessage ? "Siz" : "Bot");

        // Mesajın kimə aid olduğuna görə düzənləmə
        if (isUserMessage) {
            holder.messageText.setBackgroundResource(R.drawable.user_message_bubble);
            ((LinearLayout.LayoutParams) holder.messageText.getLayoutParams()).gravity = Gravity.END;
        } else {
            holder.messageText.setBackgroundResource(R.drawable.bot_message_bubble);
            ((LinearLayout.LayoutParams) holder.messageText.getLayoutParams()).gravity = Gravity.START;
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView senderText;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            senderText = itemView.findViewById(R.id.senderText);
        }
    }
}