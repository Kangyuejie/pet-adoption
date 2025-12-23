package com.petadoption.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MessageRequest {

    @NotNull(message = "Receiver ID is required")
    private Long receiverId;

    @NotBlank(message = "Content is required")
    private String content;

    private Long petId;
}
