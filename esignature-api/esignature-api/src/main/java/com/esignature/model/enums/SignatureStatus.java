package com.esignature.model.enums;

/**
 * États d'un workflow de signature
 */
public enum SignatureStatus {
    /**
     * En attente de signature
     */
    PENDING,
    
    /**
     * Signé avec succès
     */
    SIGNED,
    
    /**
     * Rejeté par le signataire
     */
    REJECTED,
    
    /**
     * Expiré
     */
    EXPIRED,
    
    /**
     * Annulé
     */
    CANCELLED
}
