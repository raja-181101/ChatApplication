package com.example.chatapplication;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ChildEventListener;
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
    private TextView chattingStatus;

    // Firebase
    private DatabaseReference chatRoomRef;
    private DatabaseReference connectionRef;
    private DatabaseReference firebasePresenceDataRef;
    private DatabaseReference strangerPresenceRef;

    //EventListeners
    private ChildEventListener messageListener;
    private ValueEventListener chatRoomStatusListener;
    private ValueEventListener connectionListener;
    private ValueEventListener strangerPresenceListener;

    // Data
    private List<Message> messageList;
    private MessageAdapter adapter;
    private String currentUserId;
    private String strangerUserId;
    private boolean isLeaving = false;
    private boolean isFirebaseConnected;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // User info passed from MatchmakingActivity
        String chatRoomId = getIntent().getStringExtra("chatRoomId");
        currentUserId    = getIntent().getStringExtra("currentUserId");

        // Firebase reference for this specific chat room
        DatabaseReference chatsRef = FirebaseDatabase.getInstance().getReference("chats");
        assert chatRoomId != null;
        chatRoomRef  = chatsRef.child(chatRoomId);

        // Firebase reference for User Presence
        firebasePresenceDataRef = FirebaseDatabase.getInstance().getReference("presence");
        DatabaseReference presenceRef = firebasePresenceDataRef.child(currentUserId);
        presenceRef.child("online").setValue(true);
        presenceRef.child("online").onDisconnect().setValue(false);


        // Link UI elements
        recyclerView  = findViewById(R.id.recyclerView);
        messageInput  = findViewById(R.id.messageInput);
        Button sendButton = findViewById(R.id.sendButton);
        Button leaveButton = findViewById(R.id.leaveButton);
        Button reportButton = findViewById(R.id.ReportButton);
        chattingStatus = findViewById(R.id.chattingStatusText);

        // Setup RecyclerView
        messageList = new ArrayList<>();
        adapter     = new MessageAdapter(messageList, currentUserId);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        //Check For Stranger Presence
        strangerPresenceNode();

        //Check For Self Connection
        checkFirebaseConnection();

        // Button clicks
        sendButton.setOnClickListener(v -> sendMessage());
        leaveButton.setOnClickListener(v -> confirmLeave());
        reportButton.setOnClickListener(v->reportUser());

        // Start listening for messages
        listenForMessages();

        // Listen if the other user leaves (chat room gets deleted)
        listenForChatRoomDeletion();

        //On BackPressed Event
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                confirmLeave();
            }
        });

    }

    //Check Self Connection
    private void checkFirebaseConnection(){
        connectionRef = FirebaseDatabase.getInstance()
                .getReference(".info/connected");

        connectionListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean connected = snapshot.getValue(Boolean.class);
                isFirebaseConnected = Boolean.TRUE.equals(connected);
                if (isFirebaseConnected){
                    chattingStatus.setText(R.string.chatting_with_stranger);

                    //i need to update timestamp

                    Toast.makeText(ChatActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                }else {
                    chattingStatus.setText(R.string.user_lost_connection);
                    Toast.makeText(ChatActivity.this, "Connection Lost...Waiting for User", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        connectionRef.addValueEventListener(connectionListener);
    }

    //Check For Stranger Connection
    private void strangerPresenceNode(){
        chatRoomRef.child("users").get().addOnSuccessListener(snapshot ->{
            String user1 = snapshot.child("user1").child("uid").getValue(String.class);
            String user2 = snapshot.child("user2").child("uid").getValue(String.class);
            strangerUserId = currentUserId.equals(user1)? user2: user1;
            System.out.println("------------------Stranger User ID: "+strangerUserId+" --------------------");
            strangerPresenceRef = firebasePresenceDataRef.child(strangerUserId).child("online");
            strangerPresenceListener = strangerPresenceRef.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Boolean online = snapshot.getValue(Boolean.class);
                    System.out.println("----------------Stranger Presence: "+online+" --------------------");
                    if (Boolean.TRUE.equals(online)){
                        chattingStatus.setText(R.string.chatting_with_stranger);
                    }else {
                        chattingStatus.setText(R.string.stranger_lost_connection);
                        Toast.makeText(ChatActivity.this, "Stranger Lost Connection...", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });

        });

    }

    //Get Stranger UserId and Report User
    private void reportUser() {
        chatRoomRef.child("users").get().addOnSuccessListener(snapshot ->{
            String user1 = snapshot.child("user1").child("uid").getValue(String.class);
            String user2 = snapshot.child("user2").child("uid").getValue(String.class);


            if(user1 != null && user1.equals(currentUserId)){
                strangerUserId = user2;
            }else {
                strangerUserId = user1;
            }
            Log.d("Report","Stranger ID: "+strangerUserId);
            showAlertDialog();

        });

    }

    // Check The user Reported or not
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

    //Check And Ban The User
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

    // Save The Reports From The User
    private void saveReport(String strangerUserId, String selectedReason) {
        DatabaseReference reportRef = FirebaseDatabase.getInstance()
                .getReference("reports")
                .child(strangerUserId)
                .push();
        Map<String,Object> reportData = new HashMap<>();
        reportData.put("reportedBy",currentUserId);
        reportData.put("reportedReason",selectedReason);
        reportData.put("ReportedAt",System.currentTimeMillis());
        reportRef.setValue(reportData).addOnSuccessListener(unused ->
                Toast.makeText(this, "Reported Successfully ", Toast.LENGTH_SHORT).show()
        );
    }

    //Show What to Report for the user
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


        messageListener = messagesRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot msgSnapshot, @Nullable String previousChildName) {

                String sender    = msgSnapshot.child("sender").getValue(String.class);
                String text      = msgSnapshot.child("text").getValue(String.class);

                Long timeStampTemp = msgSnapshot.child("timestamp").getValue(Long.class);
                long   timestamp = timeStampTemp != null ? timeStampTemp : 0L;

                if (sender != null && text != null && !text.isEmpty()) {
                    messageList.add(new Message(sender, text, timestamp));
                    adapter.notifyItemInserted(messageList.size()-1);
                    recyclerView.scrollToPosition(messageList.size()-1);
                }

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Failed To Load Message! ", Toast.LENGTH_SHORT).show();

            }
        });
    }

    // Detect if the chat room was deleted (other user left)
    private void listenForChatRoomDeletion() {
        chatRoomStatusListener = chatRoomRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isFirebaseConnected){
                    return;
                }
                // If chat room no longer exists, the other user left
                if (!snapshot.exists()) {
                    Toast.makeText(ChatActivity.this,
                            "The other user has left the chat.", Toast.LENGTH_LONG).show();
                    goBackToMatchmaking();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    // Send a message
    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (TextUtils.isEmpty(text)) return;
        DatabaseReference newMessageRef = chatRoomRef.child("messages").push();
        HashMap<String,Object> data = new HashMap<>();
        data.put("sender",currentUserId);
        data.put("text",text);
        data.put("timestamp",System.currentTimeMillis());
        newMessageRef.setValue(data);
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
        // This triggers the other user's deletion listener and sends them back too
        if (isFirebaseConnected) {
            chatRoomRef.removeValue().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    goBackToMatchmaking();
                } else {
                    Toast.makeText(this, "Couldn't Leave Chatroom", Toast.LENGTH_SHORT).show();
                }
            });
        }else {
            goBackToMatchmaking();
        }
    }

    // Go To Matchmaking Activity
    private void goBackToMatchmaking() {
        if (isLeaving){
            return;
        }
        isLeaving = true;
        removeListeners();
        startActivity(new Intent(ChatActivity.this, MatchmakingActivity.class));
        finish();
    }

    // Remove Listeners
    private void removeListeners()  {


        if (strangerPresenceListener!=null&&strangerPresenceRef!=null){
            strangerPresenceRef.removeEventListener(strangerPresenceListener);
            strangerPresenceListener = null;
        }
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
    protected void onDestroy() {
        super.onDestroy();
        removeListeners();
    }

}
