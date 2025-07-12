package dev.usr.database.payload.ai;

import lombok.Data;
import java.util.List;

@Data
public class ChatRequest {
    private String model;
    private List<ChatMessage> messages;
    private Double temperature;
    private Integer max_tokens;
    private Boolean stream = true;
}