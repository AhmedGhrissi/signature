package com.esignature;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.security.Security;

@SpringBootApplication
@EnableConfigurationProperties
public class ESignatureApiApplication {

    public static void main(String[] args) {
        // Ajouter BouncyCastle comme provider de sécurité
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        
        SpringApplication.run(ESignatureApiApplication.class, args);
    }
}
