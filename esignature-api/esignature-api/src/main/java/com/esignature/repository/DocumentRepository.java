package com.esignature.repository;

import com.esignature.model.entity.Document;
import com.esignature.model.enums.SignatureStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByUploadedBy(String uploadedBy);
    List<Document> findByStatus(SignatureStatus status);
    List<Document> findByExpiresAtBefore(LocalDateTime dateTime);
}
