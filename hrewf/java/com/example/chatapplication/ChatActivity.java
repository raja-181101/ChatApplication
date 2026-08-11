package com.example.comrst;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    // UI elements
    private RecyclerView recyclerView;
    private EditText messageInput;
    private Button sendButton, leaveButton,reportButton;
    private TextView chattingStatus;

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
    private String strangerUserId;
    private boolean isLeaving = false;
//    private String currentUserEmail;

    private DatabaseReference connectionRef;
    private ValueEventListener connectionListener;

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
        chatRoomRef.onDisconnect().removeValue();

        // Link UI elements
        recyclerView  = findViewById(R.id.recyclerView);
        messageInput  = findViewById(R.id.messageInput);
        sendButton    = findViewById(R.id.sendButton);
        leaveButton   = findViewById(R.id.leaveButton);
        reportButton = findViewById(R.id.ReportButton);
        chattingStatus = findViewById(R.id.chattingStatusText);

        // Setup RecyclerView
        messageList = new ArrayList<>();
        adapter     = new MessageAdapter(messageList, currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        checkFirebaseConnection();

        // Button clicks
        sendButton.setOnClickListener(v -> sendMessage());
        leaveButton.setOnClickListener(v -> confirmLeave());
        reportButton.setOnClickListener(v->reportUser());

        // Start listening for messages
        listenForMessages();

        // Listen if the other user leaves (chat room gets deleted)
        listenForChatRoomDeletion();

    }

    private void checkFirebaseConnection(){
        connectionRef = FirebaseDatabase.getInstance()
                .getReference(".info/connected");

        connectionListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                if (Boolean.TRUE.equals(connected)){
                    chattingStatus.setText("Chatting With Stranger");
                    Toast.makeText(ChatActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                }else {
                    chattingStatus.setText("Connection Lost...Waiting for User");
                    Toast.makeText(ChatActivity.this, "Connection Lost...Waiting for User", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        connectionRef.addValueEventListener(connectionListener);
    }

    private void reportUser() {
        chatRoomRef.child("users").get().addOnSuccessListener(snapshot ->{
            String user1 = snapshot.child("user1").getValue(String.class);
            String user2 = snapshot.child("user2").getValue(String.class);


            if(user1 != null && user1.equals(currentUserId)){
                strangerUserId = user2;
            }else {
                strangerUserId = user1;
            }
            Log.d("Report","Stranger ID: "+strangerUserId);
            showAlertDialog();

        });

    }

    private void checkAndReportUser(String strangerUserId, String selectedReason) {
        DatabaseReference reportsRef = FirebaseDatabase.getInstance()
                .getReference("reports")
                .child(strangerUserId);

        reportsRef.get().addOnSuccessListener(snapshot->{
           boolean alreadyReported = false;
           for (DataSnapshot report:snapshot.getChildren()){
               String reportedBy = report.child("reportedBy").getValue(String.class);
               if(reportedBy != null && reportedBy.equals(currentUserId)){
                 alreadyReported = true;
                 break;
               }
           }
           if (alreadyReported){
               Toast.makeText(this, "already Reported To This User: ", Toast.LENGTH_SHORT).show();
               return;
           }
           saveReport(strangerUserId,selectedReason);
           checkAndBanUser(strangerUserId);
        });
    }

    private void checkAndBanUser(String strangerUserId) {
        DatabaseReference reportsRef = FirebaseDatabase.getInstance()
                .getReference("reports")
                .child(strangerUserId);
        reportsRef.get().addOnSuccessListener(snapshot -> {
           long totalNum = snapshot.getChildrenCount();
           if(totalNum >= 3){
               FirebaseDatabase.getInstance()
                       .getReference("bannedUsers")
                       .child(strangerUserId)
                       .setValue(true);
               Log.d("BAN",strangerUserId+"Banned");
           }
        });
    }

    private void saveReport(String strangerUserId, String selectedReason) {
        DatabaseReference reportRef = FirebaseDatabase.getInstance()
                .getReference("reports")
                .child(strangerUserId)
                .push();
        Map<String,Object> reportData = new HashMap<>();
        reportData.put("reportedBy",currentUserId);
        reportData.put("reportedReason",selectedReason);
        reportData.put("ReportedAt",System.currentTimeMillis());
        reportRef.setValue(reportData).addOnSuccessListener(unused -> {
            Toast.makeText(this, "Reported Successfully ", Toast.LENGTH_SHORT).show();
        });
    }

    private void showAlertDialog() {
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
                    checkAndReportUser(strangerUserId,selectedReason);
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

                    if (sender != null && text != null && !text.isEmpty()) {
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
        chatRoomRef.onDisconnect().cancel();

        // Delete the entire chat room from Firebase
        // This triggers the other user's deletion listener and sends them back too
        chatRoomRef.removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                goBackToMatchmaking();
            }else {
                Toast.makeText(this, "Couldn't Leave Chatroom", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void goBackToMatchmaking() {
        if (isLeaving){
            return;
        }
        isLeaving = true;
        removeListeners();
        startActivity(new Intent(ChatActivity.this, MatchmakingActivity.class));
        finish();
    }

    private void removeListeners() {
        if(connectionRef!=null && connectionListener!=null){
            connectionRef.removeEventListener(connectionListener);
        }
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
