package com.esignature.repository;

import com.esignature.model.entity.Signature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SignatureRepository extends JpaRepository<Signature, Long> {
    List<Signature> findByDocumentId(Long documentId);
    List<Signature> findBySignerEmail(String signerEmail);
}
