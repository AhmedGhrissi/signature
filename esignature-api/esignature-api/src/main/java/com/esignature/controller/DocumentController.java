package com.esignature.controller;

import com.esignature.model.dto.*;
import com.esignature.service.DocumentService;
import com.esignature.service.VerificationService;
import com.esignature.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Documents", description = "Gestion des documents et signatures électroniques")
public class DocumentController {
    
    private final DocumentService documentService;
    private final VerificationService verificationService;
    private final WorkflowService workflowService;
    
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload un document", description = "Téléverse un document pour signature")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "uploadedBy", defaultValue = "system") String uploadedBy
    ) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            DocumentResponse response = documentService.uploadDocument(file, uploadedBy);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
            
        } catch (Exception e) {
            log.error("Erreur lors de l'upload du document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/sign")
    @Operation(summary = "Signer un document", description = "Applique une signature électronique sur un document")
    public ResponseEntity<DocumentResponse> signDocument(
            @Valid @RequestBody SignDocumentRequest request,
            HttpServletRequest httpRequest
    ) {
        try {
            String ipAddress = httpRequest.getRemoteAddr();
            String userAgent = httpRequest.getHeader("User-Agent");
            
            DocumentResponse response = documentService.signDocument(request, ipAddress, userAgent);
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Erreur de validation lors de la signature", e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erreur lors de la signature du document", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{documentId}")
    @Operation(summary = "Récupérer un document", description = "Obtient les informations d'un document")
    public ResponseEntity<DocumentResponse> getDocument(@PathVariable Long documentId) {
        try {
            DocumentResponse response = documentService.getDocument(documentId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    @GetMapping("/{documentId}/download")
    @Operation(summary = "Télécharger le document signé", description = "Télécharge le PDF signé")
    public ResponseEntity<byte[]> downloadSignedDocument(@PathVariable Long documentId) {
        try {
            byte[] pdfBytes = documentService.downloadSignedDocument(documentId);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "signed_document.pdf");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfBytes);
                    
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED).build();
        } catch (Exception e) {
            log.error("Erreur lors du téléchargement", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/workflow")
    @Operation(summary = "Créer un workflow de signature", 
               description = "Définit un processus de signature avec plusieurs signataires")
    public ResponseEntity<?> createWorkflow(@Valid @RequestBody CreateWorkflowRequest request) {
        try {
            var workflows = workflowService.createWorkflow(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(workflows);
        } catch (IllegalArgumentException e) {
            log.error("Erreur de validation", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Erreur lors de la création du workflow", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{documentId}/workflow")
    @Operation(summary = "Obtenir le workflow d'un document", 
               description = "Liste les étapes de signature d'un document")
    public ResponseEntity<?> getWorkflow(@PathVariable Long documentId) {
        try {
            var workflows = workflowService.getDocumentWorkflows(documentId);
            return ResponseEntity.ok(workflows);
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du workflow", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PostMapping("/workflow/{token}/reject")
    @Operation(summary = "Rejeter une signature", description = "Permet à un signataire de rejeter sa signature")
    public ResponseEntity<?> rejectSignature(
            @PathVariable String token,
            @RequestParam String reason
    ) {
        try {
            var workflow = workflowService.rejectSignature(token, reason);
            return ResponseEntity.ok(workflow);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("Erreur lors du rejet", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{documentId}/verify")
    @Operation(summary = "Vérifier les signatures", 
               description = "Valide l'authenticité de toutes les signatures d'un document")
    public ResponseEntity<VerificationResponse> verifyDocument(@PathVariable Long documentId) {
        try {
            VerificationResponse response = verificationService.verifyDocument(documentId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erreur lors de la vérification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
