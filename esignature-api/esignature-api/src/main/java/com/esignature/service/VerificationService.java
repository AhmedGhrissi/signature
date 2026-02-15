package com.esignature.service;

import com.esignature.model.dto.VerificationResponse;
import com.esignature.model.entity.Document;
import com.esignature.model.entity.Signature;
import com.esignature.repository.DocumentRepository;
import com.esignature.repository.SignatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {
    
    private final DocumentRepository documentRepository;
    private final SignatureRepository signatureRepository;
    private final PdfSignatureService pdfSignatureService;
    
    /**
     * Vérifier l'authenticité des signatures d'un document
     */
    public VerificationResponse verifyDocument(Long documentId) throws IOException {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Document non trouvé"));
        
        if (document.getSignedFilePath() == null) {
            return VerificationResponse.builder()
                    .isValid(false)
                    .message("Document non signé")
                    .signatures(new ArrayList<>())
                    .build();
        }
        
        // Lire le document signé
        byte[] pdfBytes = Files.readAllBytes(Paths.get(document.getSignedFilePath()));
        
        // Vérifier les signatures dans le PDF
        List<PdfSignatureService.SignatureValidation> pdfValidations = 
                pdfSignatureService.verifyPdfSignatures(pdfBytes);
        
        // Récupérer les signatures de la base de données
        List<Signature> dbSignatures = signatureRepository.findByDocumentId(documentId);
        
        // Créer les résultats de vérification
        List<VerificationResponse.SignatureVerification> verifications = new ArrayList<>();
        
        for (Signature signature : dbSignatures) {
            VerificationResponse.SignatureVerification verification = 
                    VerificationResponse.SignatureVerification.builder()
                    .signerName(signature.getSignerName())
                    .signerEmail(signature.getSignerEmail())
                    .signedAt(signature.getSignedAt())
                    .certificateIssuer(signature.getCertificateIssuer())
                    .certificateSerialNumber(signature.getCertificateSerialNumber())
                    .validationErrors(new ArrayList<>())
                    .build();
            
            // Vérification basique - à améliorer avec validation cryptographique complète
            boolean isValid = true;
            List<String> errors = new ArrayList<>();
            
            // Vérifier si la signature existe dans le PDF
            boolean foundInPdf = pdfValidations.stream()
                    .anyMatch(v -> v.getSignerName() != null && 
                                  v.getSignerName().equals(signature.getSignerName()));
            
            if (!foundInPdf) {
                isValid = false;
                errors.add("Signature non trouvée dans le PDF");
            }
            
            // Vérifier la validité du certificat (si applicable)
            if (signature.getCertificateSerialNumber() != null) {
                // TODO: Vérifier la validité du certificat avec une autorité de certification
                verification.setCertificateValid(true);
            }
            
            verification.setIsValid(isValid);
            verification.setValidationErrors(errors);
            
            verifications.add(verification);
        }
        
        // Résultat global
        boolean allValid = verifications.stream().allMatch(VerificationResponse.SignatureVerification::getIsValid);
        
        return VerificationResponse.builder()
                .isValid(allValid)
                .message(allValid ? "Toutes les signatures sont valides" : 
                        "Certaines signatures sont invalides ou suspectes")
                .signatures(verifications)
                .build();
    }
}
