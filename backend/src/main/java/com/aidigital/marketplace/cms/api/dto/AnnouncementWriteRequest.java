package com.aidigital.marketplace.cms.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AnnouncementWriteRequest(
        @NotBlank @Size(max = 128) String title,
        @NotBlank @Size(max = 8000) String body,
        @NotBlank @Pattern(regexp = "ENABLED|DISABLED") String status) {}
