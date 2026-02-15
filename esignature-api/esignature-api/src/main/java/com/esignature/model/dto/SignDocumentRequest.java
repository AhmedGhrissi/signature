package com.esignature.model.dto;

import com.esignature.model.enums.SignatureType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignDocumentRequest {
    
    @NotNull(message = "L'ID du document est requis")
    private Long documentId;
    
    @NotBlank(message = "Le nom du signataire est requis")
    private String signerName;
    
    @NotBlank(message = "L'email du signataire est requis")
    @Email(message = "Email invalide")
    private String signerEmail;
    
    @NotNull(message = "Le type de signature est requis")
    private SignatureType signatureType;
    
    // Pour signature simple
    private String signatureImageBase64;
    
    // Pour signature avancée/qualifiée
    private String certificateBase64;
    private String certificatePassword;
    
    // Position de la signature sur le PDF
    private Integer pageNumber;
    private Float xPosition;
    private Float yPosition;
    private Float width;
    private Float height;
    
    // Token pour workflow
    private String signatureToken;
}
