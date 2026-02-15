package com.esignature.service;

import com.esignature.model.enums.SignatureType;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.SignatureInterface;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.springframework.stereotype.Service;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

@Service
@Slf4j
public class PdfSignatureService {
    
    /**
     * Signer un PDF avec une signature simple (image)
     */
    public byte[] signPdfWithImage(
            byte[] pdfBytes,
            byte[] signatureImage,
            int pageNumber,
            float x,
            float y,
            float width,
            float height
    ) throws IOException {
        try (PDDocument document = Loader.loadPDF(pdfBytes);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            // Vérifier que la page existe
            if (pageNumber >= document.getNumberOfPages()) {
                pageNumber = document.getNumberOfPages() - 1;
            }
            
            PDPage page = document.getPage(pageNumber);
            
            // Créer l'image de signature
            PDImageXObject pdImage = PDImageXObject.createFromByteArray(
                    document, signatureImage, "signature");
            
            // Ajouter l'image sur la page
            try (PDPageContentStream contentStream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                
                contentStream.drawImage(pdImage, x, y, width, height);
            }
            
            document.save(outputStream);
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            log.error("Erreur lors de la signature simple du PDF", e);
            throw new IOException("Échec de la signature du PDF", e);
        }
    }
    
    /**
     * Signer un PDF avec un certificat numérique (signature avancée/qualifiée)
     */
    public byte[] signPdfWithCertificate(
            byte[] pdfBytes,
            KeyStore keyStore,
            String keyAlias,
            char[] keyPassword,
            SignatureType signatureType,
            String signerName,
            int pageNumber,
            float x,
            float y,
            float width,
            float height
    ) throws Exception {
        
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            
            // Charger le document
            PDDocument document = Loader.loadPDF(pdfBytes);
            
            // Créer la signature PDF
            PDSignature signature = new PDSignature();
            signature.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
            signature.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
            signature.setName(signerName);
            signature.setSignDate(Calendar.getInstance());
            
            // Définir le type de signature
            if (signatureType == SignatureType.QUALIFIED) {
                signature.setReason("Signature Électronique Qualifiée");
            } else {
                signature.setReason("Signature Électronique Avancée");
            }
            
            // Ajouter la signature au document
            document.addSignature(signature);
            
            // Créer le gestionnaire de signature
            SignatureInterface signatureInterface = new SignatureInterface() {
                @Override
                public byte[] sign(InputStream content) throws IOException {
                    try {
                        // Lire le contenu à signer
                        byte[] contentBytes = content.readAllBytes();
                        
                        // Récupérer la clé privée et le certificat
                        PrivateKey privateKey = (PrivateKey) keyStore.getKey(keyAlias, keyPassword);
                        Certificate[] certChain = keyStore.getCertificateChain(keyAlias);
                        
                        // Créer le générateur de signature CMS
                        List<Certificate> certList = new ArrayList<>();
                        for (Certificate cert : certChain) {
                            certList.add(cert);
                        }
                        
                        JcaCertStore certs = new JcaCertStore(certList);
                        
                        CMSSignedDataGenerator gen = new CMSSignedDataGenerator();
                        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
                                .setProvider("BC")
                                .build(privateKey);
                        
                        gen.addSignerInfoGenerator(
                                new JcaSignerInfoGeneratorBuilder(
                                        new JcaDigestCalculatorProviderBuilder()
                                                .setProvider("BC")
                                                .build())
                                        .build(signer, (X509Certificate) certChain[0]));
                        
                        gen.addCertificates(certs);
                        
                        // Générer la signature
                        CMSProcessableByteArray msg = new CMSProcessableByteArray(contentBytes);
                        CMSSignedData signedData = gen.generate(msg, false);
                        
                        return signedData.getEncoded();
                        
                    } catch (Exception e) {
                        throw new IOException("Erreur lors de la signature", e);
                    }
                }
            };
            
            // Signer le document
            document.saveIncremental(outputStream);
            
            // Ajouter un champ visuel de signature si des coordonnées sont fournies
            if (x >= 0 && y >= 0) {
                addVisualSignature(document, pageNumber, x, y, width, height, signerName);
            }
            
            document.close();
            
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            log.error("Erreur lors de la signature du PDF avec certificat", e);
            throw new Exception("Échec de la signature cryptographique du PDF", e);
        }
    }
    
    /**
     * Ajouter un champ visuel de signature
     */
    private void addVisualSignature(
            PDDocument document,
            int pageNumber,
            float x,
            float y,
            float width,
            float height,
            String signerName
    ) throws IOException {
        
        if (pageNumber >= document.getNumberOfPages()) {
            pageNumber = document.getNumberOfPages() - 1;
        }
        
        PDPage page = document.getPage(pageNumber);
        
        try (PDPageContentStream contentStream = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            
            // Dessiner un rectangle pour la signature
            contentStream.setNonStrokingColor(0.9f, 0.9f, 1.0f);
            contentStream.addRect(x, y, width, height);
            contentStream.fill();
            
            // Ajouter le texte
            contentStream.beginText();
            contentStream.setNonStrokingColor(0, 0, 0);
            contentStream.newLineAtOffset(x + 5, y + height - 15);
            contentStream.showText("Signé par: " + signerName);
            contentStream.endText();
        }
    }
    
    /**
     * Vérifier les signatures d'un PDF
     */
    public List<SignatureValidation> verifyPdfSignatures(byte[] pdfBytes) throws IOException {
        List<SignatureValidation> validations = new ArrayList<>();
        
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            List<PDSignature> signatures = document.getSignatureDictionaries();
            
            for (PDSignature signature : signatures) {
                SignatureValidation validation = new SignatureValidation();
                validation.setSignerName(signature.getName());
                validation.setSignDate(signature.getSignDate());
                validation.setReason(signature.getReason());
                
                // Vérification basique - à améliorer avec validation complète du certificat
                validation.setValid(signature.getContents() != null);
                
                validations.add(validation);
            }
            
        } catch (Exception e) {
            log.error("Erreur lors de la vérification des signatures", e);
            throw new IOException("Échec de la vérification des signatures", e);
        }
        
        return validations;
    }
    
    /**
     * Classe interne pour les résultats de validation
     */
    @lombok.Data
    public static class SignatureValidation {
        private String signerName;
        private Calendar signDate;
        private String reason;
        private boolean valid;
        private String errorMessage;
    }
}
