package com.esignature.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationResponse {
    private Boolean isValid;
    private String message;
    private List<SignatureVerification> signatures;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignatureVerification {
        private String signerName;
        private String signerEmail;
        private Boolean isValid;
        private LocalDateTime signedAt;
        private String certificateIssuer;
        private String certificateSerialNumber;
        private LocalDateTime certificateValidFrom;
        private LocalDateTime certificateValidTo;
        private Boolean certificateValid;
        private List<String> validationErrors;
    }
}
