package com.example.chatapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    // UI elements
    private RecyclerView recyclerView;
    private EditText messageInput;
    private Button sendButton, leaveButton,reportButton;

    // Firebase
    private DatabaseReference chatRoomRef;
    private DatabaseReference chatsRef;
    private ValueEventListener messageListener;
    private ValueEventListener chatRoomStatusListener;

    // Data
    private List<Message> messageList;
    private MessageAdapter adapter;

    // User info passed from MatchmakingActivity
    private String chatRoomId;
    private String currentUserId;
//    private String currentUserEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Get data passed from MatchmakingActivity
        chatRoomId       = getIntent().getStringExtra("chatRoomId");
        currentUserId    = getIntent().getStringExtra("currentUserId");
//        currentUserEmail = getIntent().getStringExtra("currentUserEmail");

        // Firebase reference for this specific chat room
        chatsRef     = FirebaseDatabase.getInstance().getReference("chats");
        chatRoomRef  = chatsRef.child(chatRoomId);

        // Link UI elements
        recyclerView  = findViewById(R.id.recyclerView);
        messageInput  = findViewById(R.id.messageInput);
        sendButton    = findViewById(R.id.sendButton);
        leaveButton   = findViewById(R.id.leaveButton);
        reportButton = findViewById(R.id.ReportButton);

        // Setup RecyclerView
        messageList = new ArrayList<>();
        adapter     = new MessageAdapter(messageList, currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Button clicks
        sendButton.setOnClickListener(v -> sendMessage());
        leaveButton.setOnClickListener(v -> confirmLeave());
        reportButton.setOnClickListener(v->reportUser());

        // Start listening for messages
        listenForMessages();

        // Listen if the other user leaves (chat room gets deleted)
        listenForChatRoomDeletion();

    }

    private void reportUser() {
        String[] reasons = {
                "Spam",
                "Harassment",
                "Abusive Language",
                "Sexual Content",
                "Other"
        };
        new AlertDialog.Builder(this).setTitle("Report")
                .setItems(reasons,(dialog,which)->{
                    String selectedReason = reasons[which];
                    System.out.println("Selected Reason:  "+selectedReason+" ------------------------>");
                    Toast.makeText(this, "Selected Reason:"+selectedReason, Toast.LENGTH_SHORT).show();
                }).show();


    }

    // Listen for new messages in real time
    private void listenForMessages() {
        DatabaseReference messagesRef = chatRoomRef.child("messages");

        messageListener = messagesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                messageList.clear();

                for (DataSnapshot msgSnapshot : snapshot.getChildren()) {
                    // Build Message object from Firebase data
                    String sender    = msgSnapshot.child("sender").getValue(String.class);
                    String text      = msgSnapshot.child("text").getValue(String.class);
                    long   timestamp = msgSnapshot.child("timestamp").getValue(long.class) != null
                            ? msgSnapshot.child("timestamp").getValue(long.class) : 0L;

                    if (sender != null && text != null) {
                        messageList.add(new Message(sender, text, timestamp));
                    }
                }

                // Refresh the list and scroll to bottom
                adapter.notifyDataSetChanged();
                if (messageList.size() > 0) {
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(ChatActivity.this,
                        "Failed to load messages.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Detect if the chat room was deleted (other user left)
    private void listenForChatRoomDeletion() {
        chatRoomStatusListener = chatRoomRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // If chat room no longer exists, the other user left
                if (!snapshot.exists()) {
                    Toast.makeText(ChatActivity.this,
                            "The other user has left the chat.", Toast.LENGTH_LONG).show();
                    goBackToMatchmaking();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) { }
        });
    }

    // Send a message
    private void sendMessage() {
        String text = messageInput.getText().toString().trim();

        if (TextUtils.isEmpty(text)) return; // Don't send empty messages

        // Create a new message entry in Firebase
        DatabaseReference newMessageRef = chatRoomRef.child("messages").push();
        newMessageRef.child("sender").setValue(currentUserId);
        newMessageRef.child("text").setValue(text);
        newMessageRef.child("timestamp").setValue(System.currentTimeMillis());

        // Clear the input field
        messageInput.setText("");
    }

    // Confirm before leaving
    private void confirmLeave() {
        new AlertDialog.Builder(this)
                .setTitle("Leave Chat")
                .setMessage("Are you sure you want to leave? The chat will end for both users.")
                .setPositiveButton("Leave", (dialog, which) -> leaveChat())
                .setNegativeButton("Stay", null)
                .show();
    }

    // Delete the chat room and go back to matchmaking
    private void leaveChat() {
        // Remove all listeners first
        removeListeners();

        // Delete the entire chat room from Firebase
        // This triggers the other user's deletion listener and sends them back too
        chatRoomRef.removeValue().addOnCompleteListener(task -> goBackToMatchmaking());
    }

    private void goBackToMatchmaking() {
        removeListeners();
        startActivity(new Intent(ChatActivity.this, MatchmakingActivity.class));
        finish();
    }

    private void removeListeners() {
        if (messageListener != null) {
            chatRoomRef.child("messages").removeEventListener(messageListener);
            messageListener = null;
        }
        if (chatRoomStatusListener != null) {
            chatRoomRef.removeEventListener(chatRoomStatusListener);
            chatRoomStatusListener = null;
        }
    }

    @Override
    public void onBackPressed() {
        // Intercept back button — treat it as leaving the chat
        super.onBackPressed();
        confirmLeave();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        removeListeners();
    }
}
