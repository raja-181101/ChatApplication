package com.example.chatapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    // Two view types: messages sent by ME vs messages by the OTHER person
    private static final int VIEW_TYPE_SENT     = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messageList;
    private String currentUserId; // So we know which messages are "mine"

    public MessageAdapter(List<Message> messageList, String currentUserId) {
        this.messageList      = messageList;
        this.currentUserId = currentUserId;
    }

    // Decide which layout to use based on who sent the message
    @Override
    public int getItemViewType(int position) {
        Message message = messageList.get(position);
        if (message.getSender().equals(currentUserId)) {
            return VIEW_TYPE_SENT;     // My message
        } else {
            return VIEW_TYPE_RECEIVED; // Other person's message
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (viewType == VIEW_TYPE_SENT) {
            // Use the "sent" layout (right-aligned bubble)
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
        } else {
            // Use the "received" layout (left-aligned bubble)
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
        }
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);

        // Set the message text
        holder.messageText.setText(message.getText());

        // For received messages, also show who sent it
        if (holder.senderText != null) {
            holder.senderText.setText(message.getSender());
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // ViewHolder — holds references to the views in each list item
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageText;
        TextView senderText; // Only used in received messages

        MessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            senderText  = itemView.findViewById(R.id.senderText); // May be null for sent
        }
    }
}
