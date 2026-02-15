package com.esignature.model.enums;

/**
 * Types de signature électronique supportés
 */
public enum SignatureType {
    /**
     * Signature simple - image de signature appliquée sur le document
     */
    SIMPLE,
    
    /**
     * Signature avancée - avec certificat numérique (PKI/X.509)
     */
    ADVANCED,
    
    /**
     * Signature qualifiée - conforme eIDAS avec certificat qualifié
     */
    QUALIFIED
}
