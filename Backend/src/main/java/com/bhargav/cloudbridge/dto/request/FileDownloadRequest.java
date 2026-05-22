package com.bhargav.cloudbridge.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileDownloadRequest {

    // Collected before download — identity capture
    // Not for access control — just for tracking who downloaded
    @NotBlank(message = "Email is required to download")
    @Email(message = "Please enter a valid email address")
    private String receiverEmail;

    // Only required if file is password protected
    // null if no password was set by sender
    private String password;
}