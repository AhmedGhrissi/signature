package com.esignature.service;

import com.esignature.model.dto.CreateWorkflowRequest;
import com.esignature.model.entity.Document;
import com.esignature.model.entity.SignatureWorkflow;
import com.esignature.model.enums.SignatureStatus;
import com.esignature.repository.DocumentRepository;
import com.esignature.repository.SignatureWorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowService {
    
    private final DocumentRepository documentRepository;
    private final SignatureWorkflowRepository workflowRepository;
    
    /**
     * Créer un workflow de signature avec plusieurs signataires
     */
    @Transactional
    public List<SignatureWorkflow> createWorkflow(CreateWorkflowRequest request) {
        Document document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new IllegalArgumentException("Document non trouvé"));
        
        // Définir l'expiration du document si spécifiée
        if (request.getExpirationDays() != null) {
            document.setExpiresAt(LocalDateTime.now().plusDays(request.getExpirationDays()));
            documentRepository.save(document);
        }
        
        // Créer les workflows pour chaque signataire
        List<SignatureWorkflow> workflows = request.getSigners().stream()
                .map(signer -> {
                    SignatureWorkflow workflow = SignatureWorkflow.builder()
                            .document(document)
                            .signerName(signer.getName())
                            .signerEmail(signer.getEmail())
                            .signOrder(signer.getSignOrder())
                            .requiredSignatureType(signer.getRequiredSignatureType())
                            .status(SignatureStatus.PENDING)
                            .signatureToken(UUID.randomUUID().toString())
                            .build();
                    
                    if (request.getExpirationDays() != null) {
                        workflow.setExpiresAt(LocalDateTime.now().plusDays(request.getExpirationDays()));
                    }
                    
                    return workflow;
                })
                .toList();
        
        // Sauvegarder tous les workflows
        workflows = workflowRepository.saveAll(workflows);
        
        // Notifier le premier signataire (ordre 1)
        workflows.stream()
                .filter(w -> w.getSignOrder() == 1)
                .forEach(w -> {
                    w.setNotifiedAt(LocalDateTime.now());
                    workflowRepository.save(w);
                    // TODO: Envoyer l'email avec le lien de signature
                    log.info("Notification envoyée à {} (ordre 1) - Token: {}", 
                            w.getSignerEmail(), w.getSignatureToken());
                });
        
        return workflows;
    }
    
    /**
     * Obtenir les workflows d'un document
     */
    public List<SignatureWorkflow> getDocumentWorkflows(Long documentId) {
        return workflowRepository.findByDocumentIdOrderBySignOrder(documentId);
    }
    
    /**
     * Obtenir un workflow par token
     */
    public SignatureWorkflow getWorkflowByToken(String token) {
        return workflowRepository.findBySignatureToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de signature invalide"));
    }
    
    /**
     * Rejeter une signature
     */
    @Transactional
    public SignatureWorkflow rejectSignature(String token, String reason) {
        SignatureWorkflow workflow = getWorkflowByToken(token);
        
        if (workflow.getStatus() != SignatureStatus.PENDING) {
            throw new IllegalStateException("Cette signature ne peut plus être rejetée");
        }
        
        workflow.setStatus(SignatureStatus.REJECTED);
        workflow.setRejectionReason(reason);
        
        // Mettre à jour le document
        Document document = workflow.getDocument();
        document.setStatus(SignatureStatus.REJECTED);
        documentRepository.save(document);
        
        return workflowRepository.save(workflow);
    }
    
    /**
     * Obtenir les signatures en attente pour un utilisateur
     */
    public List<SignatureWorkflow> getPendingSignatures(String email) {
        return workflowRepository.findBySignerEmailAndStatus(email, SignatureStatus.PENDING);
    }
}
