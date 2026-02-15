package com.esignature.model.dto;

import com.esignature.model.enums.SignatureStatus;
import com.esignature.model.enums.SignatureType;
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
public class DocumentResponse {
    private Long id;
    private String name;
    private String mimeType;
    private Long fileSize;
    private String uploadedBy;
    private SignatureStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime signedAt;
    private LocalDateTime expiresAt;
    private List<SignatureResponse> signatures;
    private List<WorkflowResponse> workflows;
    private String downloadUrl;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class SignatureResponse {
    private Long id;
    private String signerName;
    private String signerEmail;
    private SignatureType signatureType;
    private LocalDateTime signedAt;
    private String certificateSerialNumber;
    private String certificateIssuer;
}

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class WorkflowResponse {
    private Long id;
    private String signerName;
    private String signerEmail;
    private Integer signOrder;
    private SignatureType requiredSignatureType;
    private SignatureStatus status;
    private LocalDateTime signedAt;
    private LocalDateTime expiresAt;
}
