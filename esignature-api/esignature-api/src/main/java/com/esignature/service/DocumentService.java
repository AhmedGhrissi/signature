package com.esignature.service;

import com.esignature.model.dto.*;
import com.esignature.model.entity.Document;
import com.esignature.model.entity.Signature;
import com.esignature.model.entity.SignatureWorkflow;
import com.esignature.model.enums.SignatureStatus;
import com.esignature.model.enums.SignatureType;
import com.esignature.repository.DocumentRepository;
import com.esignature.repository.SignatureRepository;
import com.esignature.repository.SignatureWorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {
    
    private final DocumentRepository documentRepository;
    private final SignatureRepository signatureRepository;
    private final SignatureWorkflowRepository workflowRepository;
    private final PdfSignatureService pdfSignatureService;
    private final CertificateService certificateService;
    
    @Value("${storage.location}")
    private String storageLocation;
    
    @Value("${storage.signed-location}")
    private String signedStorageLocation;
    
    /**
     * Upload un document
     */
    @Transactional
    public DocumentResponse uploadDocument(MultipartFile file, String uploadedBy) throws IOException {
        // Créer le répertoire si nécessaire
        Path uploadPath = Paths.get(storageLocation);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // Générer un nom de fichier unique
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path filePath = uploadPath.resolve(fileName);
        
        // Sauvegarder le fichier
        Files.write(filePath, file.getBytes());
        
        // Créer l'entité document
        Document document = Document.builder()
                .name(file.getOriginalFilename())
                .originalFilePath(filePath.toString())
                .mimeType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedBy(uploadedBy)
                .status(SignatureStatus.PENDING)
                .build();
        
        document = documentRepository.save(document);
        
        return mapToDocumentResponse(document);
    }
    
    /**
     * Signer un document
     */
    @Transactional
    public DocumentResponse signDocument(SignDocumentRequest request, String ipAddress, String userAgent) 
            throws Exception {
        
        Document document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new IllegalArgumentException("Document non trouvé"));
        
        // Vérifier le workflow si un token est fourni
        if (request.getSignatureToken() != null) {
            SignatureWorkflow workflow = workflowRepository.findBySignatureToken(request.getSignatureToken())
                    .orElseThrow(() -> new IllegalArgumentException("Token de signature invalide"));
            
            if (workflow.getStatus() != SignatureStatus.PENDING) {
                throw new IllegalStateException("Ce workflow de signature n'est plus valide");
            }
            
            if (workflow.getExpiresAt() != null && workflow.getExpiresAt().isBefore(LocalDateTime.now())) {
                workflow.setStatus(SignatureStatus.EXPIRED);
                workflowRepository.save(workflow);
                throw new IllegalStateException("Le lien de signature a expiré");
            }
        }
        
        // Lire le document original
        byte[] pdfBytes = Files.readAllBytes(Paths.get(document.getOriginalFilePath()));
        byte[] signedPdfBytes;
        
        // Créer l'entité signature
        Signature signature = Signature.builder()
                .document(document)
                .signerName(request.getSignerName())
                .signerEmail(request.getSignerEmail())
                .signatureType(request.getSignatureType())
                .pageNumber(request.getPageNumber())
                .xPosition(request.getXPosition())
                .yPosition(request.getYPosition())
                .width(request.getWidth())
                .height(request.getHeight())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        
        // Appliquer la signature selon le type
        switch (request.getSignatureType()) {
            case SIMPLE:
                signedPdfBytes = signWithSimpleSignature(pdfBytes, request, signature);
                break;
                
            case ADVANCED:
            case QUALIFIED:
                signedPdfBytes = signWithCertificate(pdfBytes, request, signature);
                break;
                
            default:
                throw new IllegalArgumentException("Type de signature non supporté");
        }
        
        // Sauvegarder le PDF signé
        Path signedPath = Paths.get(signedStorageLocation);
        if (!Files.exists(signedPath)) {
            Files.createDirectories(signedPath);
        }
        
        String signedFileName = "signed_" + UUID.randomUUID() + "_" + document.getName();
        Path signedFilePath = signedPath.resolve(signedFileName);
        Files.write(signedFilePath, signedPdfBytes);
        
        document.setSignedFilePath(signedFilePath.toString());
        document.setSignedAt(LocalDateTime.now());
        
        // Mettre à jour le statut si toutes les signatures sont complètes
        updateDocumentStatus(document);
        
        // Sauvegarder
        signature = signatureRepository.save(signature);
        document = documentRepository.save(document);
        
        // Mettre à jour le workflow si applicable
        if (request.getSignatureToken() != null) {
            updateWorkflowStatus(request.getSignatureToken(), signature);
        }
        
        return mapToDocumentResponse(document);
    }
    
    private byte[] signWithSimpleSignature(
            byte[] pdfBytes,
            SignDocumentRequest request,
            Signature signature
    ) throws IOException {
        
        if (request.getSignatureImageBase64() == null) {
            throw new IllegalArgumentException("Image de signature requise pour signature simple");
        }
        
        byte[] imageBytes = Base64.getDecoder().decode(request.getSignatureImageBase64());
        
        return pdfSignatureService.signPdfWithImage(
                pdfBytes,
                imageBytes,
                request.getPageNumber() != null ? request.getPageNumber() : 0,
                request.getXPosition() != null ? request.getXPosition() : 100f,
                request.getYPosition() != null ? request.getYPosition() : 100f,
                request.getWidth() != null ? request.getWidth() : 150f,
                request.getHeight() != null ? request.getHeight() : 50f
        );
    }
    
    private byte[] signWithCertificate(
            byte[] pdfBytes,
            SignDocumentRequest request,
            Signature signature
    ) throws Exception {
        
        if (request.getCertificateBase64() == null) {
            throw new IllegalArgumentException("Certificat requis pour signature avancée/qualifiée");
        }
        
        byte[] certBytes = Base64.getDecoder().decode(request.getCertificateBase64());
        KeyStore keyStore = certificateService.loadKeyStore(
                certBytes,
                request.getCertificatePassword()
        );
        
        String alias = certificateService.getFirstAlias(keyStore);
        
        // Extraire les informations du certificat
        var certInfo = certificateService.extractCertificateInfo(keyStore, alias);
        signature.setCertificateSerialNumber(certInfo.get("serialNumber"));
        signature.setCertificateIssuer(certInfo.get("issuer"));
        
        return pdfSignatureService.signPdfWithCertificate(
                pdfBytes,
                keyStore,
                alias,
                request.getCertificatePassword().toCharArray(),
                request.getSignatureType(),
                request.getSignerName(),
                request.getPageNumber() != null ? request.getPageNumber() : 0,
                request.getXPosition() != null ? request.getXPosition() : 100f,
                request.getYPosition() != null ? request.getYPosition() : 100f,
                request.getWidth() != null ? request.getWidth() : 200f,
                request.getHeight() != null ? request.getHeight() : 80f
        );
    }
    
    private void updateDocumentStatus(Document document) {
        List<SignatureWorkflow> workflows = workflowRepository.findByDocumentIdOrderBySignOrder(document.getId());
        
        if (workflows.isEmpty()) {
            // Pas de workflow, document signé directement
            document.setStatus(SignatureStatus.SIGNED);
        } else {
            // Vérifier si tous les signataires ont signé
            boolean allSigned = workflows.stream()
                    .allMatch(w -> w.getStatus() == SignatureStatus.SIGNED);
            
            if (allSigned) {
                document.setStatus(SignatureStatus.SIGNED);
            }
        }
    }
    
    private void updateWorkflowStatus(String token, Signature signature) {
        SignatureWorkflow workflow = workflowRepository.findBySignatureToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Workflow non trouvé"));
        
        workflow.setStatus(SignatureStatus.SIGNED);
        workflow.setSignedAt(LocalDateTime.now());
        workflow.setSignature(signature);
        workflowRepository.save(workflow);
        
        // Notifier le signataire suivant si applicable
        notifyNextSigner(workflow.getDocument().getId(), workflow.getSignOrder());
    }
    
    private void notifyNextSigner(Long documentId, Integer currentOrder) {
        List<SignatureWorkflow> nextWorkflows = workflowRepository.findByDocumentIdOrderBySignOrder(documentId)
                .stream()
                .filter(w -> w.getSignOrder() == currentOrder + 1 && w.getStatus() == SignatureStatus.PENDING)
                .toList();
        
        for (SignatureWorkflow workflow : nextWorkflows) {
            workflow.setNotifiedAt(LocalDateTime.now());
            workflowRepository.save(workflow);
            // TODO: Envoyer email avec token
            log.info("Notification envoyée à {} pour signer le document", workflow.getSignerEmail());
        }
    }
    
    /**
     * Récupérer un document par ID
     */
    public DocumentResponse getDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document non trouvé"));
        return mapToDocumentResponse(document);
    }
    
    /**
     * Télécharger le document signé
     */
    public byte[] downloadSignedDocument(Long documentId) throws IOException {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document non trouvé"));
        
        if (document.getSignedFilePath() == null) {
            throw new IllegalStateException("Document pas encore signé");
        }
        
        return Files.readAllBytes(Paths.get(document.getSignedFilePath()));
    }
    
    /**
     * Mapper vers DTO
     */
    private DocumentResponse mapToDocumentResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .name(document.getName())
                .mimeType(document.getMimeType())
                .fileSize(document.getFileSize())
                .uploadedBy(document.getUploadedBy())
                .status(document.getStatus())
                .createdAt(document.getCreatedAt())
                .signedAt(document.getSignedAt())
                .expiresAt(document.getExpiresAt())
                .downloadUrl(document.getSignedFilePath() != null ? 
                        "/documents/" + document.getId() + "/download" : null)
                .build();
    }
}
