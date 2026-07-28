package com.example.chatapplication;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;


public class MatchmakingActivity extends AppCompatActivity {
    // UI Elements
    private Button findChatButton;
    private ProgressBar progressBar;
    private TextView statusText;

    // Firebase Database Reference
    private DatabaseReference waitingRoomRef;
    private DatabaseReference chatsRef;
    private DatabaseReference myMatchRef;
    private DatabaseReference bannedUser;

    // Event Listeners
    private ValueEventListener matchListener;

    // Data Variables
    private boolean isMatched = false;
    private boolean isSearching = false;
    private String currentUserId;

    //Connection Variables
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_matchmaking);

        //Get Current User Id
            FirebaseAuth mAuth = FirebaseAuth.getInstance();
            FirebaseUser user = mAuth.getCurrentUser();
            if (user == null) {
                startActivity(new Intent(this, LauncherActivity.class));
                finish();
                return;
            }
            currentUserId    = user.getUid();


        // Get Database References
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            waitingRoomRef = database.getReference("waiting_room");
            chatsRef = database.getReference("chats");
            bannedUser = database.getReference("bannedUsers");

        // Link UI elements
        findChatButton = findViewById(R.id.findChatButton);
        progressBar    = findViewById(R.id.progressBar);
        statusText     = findViewById(R.id.statusText);

        // Network Callback if Connection Lost
        registerRequestCallback();

        // Call Matchmaking Method
        findChatButton.setOnClickListener(v -> {

            if (!isSearching){
                if (!NetworkUtils.isInternetConnected(this)){
                    Toast.makeText(this, "Internet is Not Connected", Toast.LENGTH_SHORT).show();
                    return;
                }
                startMatchmaking();
            }else {
                stopMatch();
            }
        });

    }

    // Network Callback for Reconnection
    private void registerRequestCallback(){
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        networkCallback = new ConnectivityManager.NetworkCallback(){
            @Override
            public void onAvailable(@NonNull Network network) {
                runOnUiThread(()->{

                });
            }

            @Override
            public void onLost(@NonNull Network network) {
                runOnUiThread(()->{
                    Toast.makeText(MatchmakingActivity.this, "Network Lost...", Toast.LENGTH_SHORT).show();
                    if (isSearching){
                        stopMatch();
                    }
                });
            }
        };
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }

    // Stop Matchmaking
    private void stopMatch(){

            isSearching = false;
            removeListeners();
            waitingRoomRef.child(currentUserId).removeValue();
            resetUI();

    }

    // Start Matchmaking
    private void startMatchmaking() {
        bannedUser
                .child(currentUserId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.exists()){
                        Toast.makeText(this, "You Are Banned", Toast.LENGTH_SHORT).show();
                        System.out.println("<<<----------------------You are Banned----------------->>>>");
                        return;
                    }
                    isMatched   = false;
                    isSearching = true;

//                    findChatButton.setEnabled(false);
                    progressBar.setVisibility(View.VISIBLE);
                    findChatButton.setText(R.string.stop_search_btn);
                    statusText.setText(R.string.looking_someone_txt);


                    // First clean up any old entry for ourselves
                    waitingRoomRef.child(currentUserId).removeValue()
                            .addOnCompleteListener(task -> scanAndMatch());
                });



    }

    // Scan the waiting room and try to match using a transaction
    private void scanAndMatch() {
        waitingRoomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isSearching) return;

                String foundUserId = null;

                // Find someone else waiting (not ourselves)
                for (DataSnapshot child : snapshot.getChildren()) {
                    String uid = child.getKey();
                    if (uid != null && !uid.equals(currentUserId)) {
                        foundUserId = uid;
                        break;
                    }
                }

                if (foundUserId != null) {
                    // Try to "claim" this waiting user using a transaction
                    // Transaction prevents two people matching the same person simultaneously
                    tryClaimUser(foundUserId);
                } else {
                    // Nobody waiting — add ourselves and listen for a match
                    addSelfAndWait();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError("Connection error. Try again.");
                resetUI();
            }
        });
    }

    // Use a Firebase Transaction to atomically claim a waiting user
    private void tryClaimUser(String otherUserId) {
        DatabaseReference otherUserRef = waitingRoomRef.child(otherUserId);

        otherUserRef.runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                // If the user is still there, remove them (claim them)
                if (currentData.getValue() != null) {
                    currentData.setValue(null); // Remove from waiting room
                    return Transaction.success(currentData);
                } else {
                    // Someone else already claimed them — abort
                    return Transaction.abort();
                }
            }

            @Override
            public void onComplete(DatabaseError error, boolean committed, DataSnapshot snapshot) {
                if (committed && isSearching) {
                    // Successfully claimed! Create chat room
                    isMatched = true;
                    createChatRoom(otherUserId);
                } else {
                    // Claim failed — that user was taken. Try again.
                    scanAndMatch();
                }
            }
        });
    }

    // Create a chat room for the two matched users
    private void createChatRoom(String otherUserId) {
        // Consistent room ID regardless of who matched whom
        String chatRoomId;
        if (currentUserId.compareTo(otherUserId) < 0) {
            chatRoomId = currentUserId + "_" + otherUserId;
        } else {
            chatRoomId = otherUserId + "_" + currentUserId;
        }

        DatabaseReference roomRef = chatsRef.child(chatRoomId);
        long currentTime = System.currentTimeMillis();

        Map<String, Object> user1 = new HashMap<>();
        user1.put("uid",currentUserId);
        user1.put("lastSeen",currentTime);

        Map<String, Object> user2 = new HashMap<>();
        user2.put("uid",otherUserId);
        user2.put("lastSeen",currentTime);


        // Write both users into the chat room
        roomRef.child("users").child("user1").setValue(user1);
        roomRef.child("users").child("user2").setValue(user2)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && isSearching) {
                        openChatRoom(chatRoomId);
                    } else {
                        showError("Failed to create chat room. Try again.");
                        resetUI();
                    }
                });
    }

    // Add ourselves to waiting room and listen for someone to match us
    private void addSelfAndWait() {
        DatabaseReference myRef = waitingRoomRef.child(currentUserId);

        // Add to waiting room
        myRef.setValue(currentUserId);

        // Remove ourselves if we disconnect unexpectedly
        myRef.onDisconnect().removeValue();
        statusText.setText(R.string.waiting_for_someone_txt);

        // The other user will create a chat room with our UID in it
        myMatchRef = chatsRef;
        matchListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isMatched || !isSearching) return;

                for (DataSnapshot room : snapshot.getChildren()) {
                    // Check if this room has us as a user
                    for (DataSnapshot userEntry : room.child("users").getChildren()) {
                        if (currentUserId.equals(userEntry.child("uid").getValue(String.class))) {
                            // We've been matched!
                            isMatched = true;
                            isSearching = false;
                            waitingRoomRef.child(currentUserId).removeValue();
                            openChatRoom(room.getKey());
                            return;
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                showError("Connection error. Try again.");
                resetUI();
            }
        };

        chatsRef.addValueEventListener(matchListener);
    }

    // Open Chatroom
    private void openChatRoom(String chatRoomId) {
        removeListeners();

        Intent intent = new Intent(MatchmakingActivity.this, ChatActivity.class);
        intent.putExtra("chatRoomId", chatRoomId);
        intent.putExtra("currentUserId", currentUserId);
        startActivity(intent);
        finish();

        resetUI();
    }

    // Remove Event Listeners
    private void removeListeners() {
        if (matchListener != null && myMatchRef != null) {
            chatsRef.removeEventListener(matchListener);
            matchListener = null;
        }
    }

    // Reset The UI
    private void resetUI() {
        isSearching = false;
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            statusText.setText(R.string.find_someone_txt);
            findChatButton.setText(R.string.start_search_btn);
        });
    }

    // Show Error
    private void showError(String msg) {
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }


    @Override
    protected void onStop() {
        super.onStop();
        isSearching = false;
        removeListeners();
        waitingRoomRef.child(currentUserId).removeValue();
        resetUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (connectivityManager!=null && networkCallback!=null){
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
    }
}
