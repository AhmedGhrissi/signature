package com.esignature.model.entity;

import com.esignature.model.enums.SignatureType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "signatures")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Signature {
    
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
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignatureType signatureType;
    
    @Column
    private String signatureImagePath;
    
    @Column(columnDefinition = "TEXT")
    private String certificateData;
    
    @Column
    private String certificateSerialNumber;
    
    @Column
    private String certificateIssuer;
    
    @Column(columnDefinition = "TEXT")
    private String digitalSignature;
    
    @Column
    private Integer pageNumber;
    
    @Column
    private Float xPosition;
    
    @Column
    private Float yPosition;
    
    @Column
    private Float width;
    
    @Column
    private Float height;
    
    @Column(nullable = false)
    private LocalDateTime signedAt;
    
    @Column
    private String ipAddress;
    
    @Column
    private String userAgent;
    
    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    @PrePersist
    protected void onCreate() {
        if (signedAt == null) {
            signedAt = LocalDateTime.now();
        }
    }
}
