package com.d208.mr_patent_backend.domain.chat.entity;

import java.io.Serializable;
import java.util.Objects;

//복합키 쓰기위해
public class ChatRoomId implements Serializable {
    private String roomId;
    private Integer userId;

    public ChatRoomId() {}

    public ChatRoomId(String roomId, Integer userId) {
        this.roomId = roomId;
        this.userId = userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ChatRoomId)) return false;
        ChatRoomId that = (ChatRoomId) o;
        return Objects.equals(roomId, that.roomId) &&
                Objects.equals(userId, that.userId);
    }
    @Override
    public int hashCode() {
        return Objects.hash(roomId, userId);
    }
}