package com.zeneo.omechle.model;

import com.google.firebase.Timestamp;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

public class Message implements Serializable {

    private String text;
    private String from;
    private Date sentAt;
    private String roomId;
    private boolean me;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isMe() {
        return me;
    }

    public void setMe(boolean me) {
        this.me = me;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public Date getSentAt() {
        return sentAt;
    }

    public void setSentAt(Date sentAt) {
        this.sentAt = sentAt;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public static Message fromMap(Map<String, Object> objectMap) {
        Message message = new Message();
        message.setText((String) objectMap.get("text"));
        message.setRoomId((String) objectMap.get("roomId"));
        message.setFrom((String) objectMap.get("from"));
        message.setSentAt(new Date(((Timestamp) Objects.requireNonNull(objectMap.get("sentAt"))).getSeconds()*1000));
        return message;
    }

    @Override
    public String toString() {
        return "Message{" +
                "text='" + text + '\'' +
                ", from='" + from + '\'' +
                ", sentAt=" + sentAt +
                ", roomId='" + roomId + '\'' +
                ", me=" + me +
                '}';
    }
}
