package com.evogroup.minicrm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class NoteRequest {

    @NotBlank
    private String content;

    @NotNull
    private Long clientId;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getClientId() { return clientId; }
    public void setClientId(Long clientId) { this.clientId = clientId; }
}
