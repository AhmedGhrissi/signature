package com.esignature.model.entity;

import com.esignature.model.enums.SignatureStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "documents")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Document {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String originalFilePath;
    
    @Column
    private String signedFilePath;
    
    @Column(nullable = false)
    private String mimeType;
    
    @Column(nullable = false)
    private Long fileSize;
    
    @Column(nullable = false)
    private String uploadedBy;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SignatureStatus status;
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime signedAt;
    
    @Column
    private LocalDateTime expiresAt;
    
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Signature> signatures = new ArrayList<>();
    
    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SignatureWorkflow> workflows = new ArrayList<>();
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = SignatureStatus.PENDING;
        }
    }
}
