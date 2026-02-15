package com.esignature.model.entity;

import com.esignature.model.enums.SignatureStatus;
import com.esignature.model.enums.SignatureType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "signature_workflows")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SignatureWorkflow {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;
    
    @Column(nullable = false)
    private String signerName;
    
    @Column(nullable = false)
    private String signerEmail;
    
    @Column(nullable = false)
    private Integer signOrder;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignatureType requiredSignatureType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignatureStatus status;
    
    @Column(nullable = false, unique = true)
    private String signatureToken;
    
    @Column
    private LocalDateTime notifiedAt;
    
    @Column
    private LocalDateTime signedAt;
    
    @Column
    private LocalDateTime expiresAt;
    
    @OneToOne
    @JoinColumn(name = "signature_id")
    private Signature signature;
    
    @Column(columnDefinition = "TEXT")
    private String rejectionReason;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = SignatureStatus.PENDING;
        }
    }
}
