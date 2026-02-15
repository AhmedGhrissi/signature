package com.esignature.model.dto;

import com.esignature.model.enums.SignatureType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateWorkflowRequest {
    
    @NotNull(message = "L'ID du document est requis")
    private Long documentId;
    
    @NotNull(message = "La liste des signataires est requise")
    private List<WorkflowSignerDto> signers;
    
    private Integer expirationDays;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkflowSignerDto {
        
        @NotBlank(message = "Le nom du signataire est requis")
        private String name;
        
        @NotBlank(message = "L'email du signataire est requis")
        @Email(message = "Email invalide")
        private String email;
        
        @NotNull(message = "L'ordre de signature est requis")
        @Min(value = 1, message = "L'ordre doit être >= 1")
        private Integer signOrder;
        
        @NotNull(message = "Le type de signature est requis")
        private SignatureType requiredSignatureType;
    }
}
