package com.esignature.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class CertificateService {
    
    /**
     * Charger un KeyStore depuis des bytes
     */
    public KeyStore loadKeyStore(byte[] keystoreBytes, String password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(new ByteArrayInputStream(keystoreBytes), password.toCharArray());
        return keyStore;
    }
    
    /**
     * Obtenir le premier alias du KeyStore
     */
    public String getFirstAlias(KeyStore keyStore) throws Exception {
        Enumeration<String> aliases = keyStore.aliases();
        if (!aliases.hasMoreElements()) {
            throw new IllegalArgumentException("Aucun certificat trouvé dans le KeyStore");
        }
        return aliases.nextElement();
    }
    
    /**
     * Extraire les informations du certificat
     */
    public Map<String, String> extractCertificateInfo(KeyStore keyStore, String alias) throws Exception {
        Map<String, String> info = new HashMap<>();
        
        Certificate cert = keyStore.getCertificate(alias);
        if (cert instanceof X509Certificate x509Cert) {
            info.put("serialNumber", x509Cert.getSerialNumber().toString());
            info.put("issuer", x509Cert.getIssuerDN().getName());
            info.put("subject", x509Cert.getSubjectDN().getName());
            info.put("notBefore", x509Cert.getNotBefore().toString());
            info.put("notAfter", x509Cert.getNotAfter().toString());
        }
        
        return info;
    }
    
    /**
     * Valider un certificat
     */
    public boolean validateCertificate(X509Certificate cert) {
        try {
            cert.checkValidity();
            return true;
        } catch (Exception e) {
            log.error("Certificat invalide", e);
            return false;
        }
    }
}
