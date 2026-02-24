package com.smarttraffic.backend.dto.chat;

import java.util.ArrayList;
import java.util.List;

public class ChatResponse {
    private String message;
    private List<String> image = new ArrayList<>();

    public ChatResponse() {
    }

    public ChatResponse(String message, List<String> image) {
        this.message = message;
        this.image = image;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getImage() {
        return image;
    }

    public void setImage(List<String> image) {
        this.image = image;
    }
}
