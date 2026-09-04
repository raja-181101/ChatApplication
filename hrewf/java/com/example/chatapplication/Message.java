package com.example.comrst;

// Simple model class to represent one chat message
public class Message {

    private String sender;    // Email of the person who sent it
    private String text;      // The message content
    private long   timestamp; // When it was sent (milliseconds)

    // Empty constructor required by Firebase
    public Message() { }

    // Constructor we use in code
    public Message(String sender, String text, long timestamp) {
        this.sender    = sender;
        this.text      = text;
        this.timestamp = timestamp;
    }

    // Getters
    public String getSender()    { return sender; }
    public String getText()      { return text; }
    public long   getTimestamp() { return timestamp; }

    // Setters (required by Firebase)
    public void setSender(String sender)       { this.sender = sender; }
    public void setText(String text)           { this.text = text; }
    public void setTimestamp(long timestamp)   { this.timestamp = timestamp; }
}
