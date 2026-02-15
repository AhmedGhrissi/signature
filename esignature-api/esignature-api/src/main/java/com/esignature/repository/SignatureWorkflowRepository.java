package com.esignature.repository;

import com.esignature.model.entity.SignatureWorkflow;
import com.esignature.model.enums.SignatureStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SignatureWorkflowRepository extends JpaRepository<SignatureWorkflow, Long> {
    List<SignatureWorkflow> findByDocumentIdOrderBySignOrder(Long documentId);
    Optional<SignatureWorkflow> findBySignatureToken(String token);
    List<SignatureWorkflow> findByDocumentIdAndStatus(Long documentId, SignatureStatus status);
    List<SignatureWorkflow> findBySignerEmailAndStatus(String email, SignatureStatus status);
}
