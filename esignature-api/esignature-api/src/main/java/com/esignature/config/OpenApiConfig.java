package com.esignature.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    
    @Value("${server.port}")
    private String serverPort;
    
    @Bean
    public OpenAPI customOpenAPI() {
        Server server = new Server();
        server.setUrl("http://localhost:" + serverPort + "/api/v1");
        server.setDescription("Serveur de développement");
        
        Contact contact = new Contact();
        contact.setName("Équipe E-Signature");
        contact.setEmail("support@esignature.com");
        
        License license = new License();
        license.setName("Apache 2.0");
        license.setUrl("https://www.apache.org/licenses/LICENSE-2.0");
        
        Info info = new Info()
                .title("API de Signature Électronique")
                .version("1.0.0")
                .description("API REST complète pour la gestion de signatures électroniques.\n\n" +
                        "**Fonctionnalités :**\n" +
                        "- Upload de documents PDF\n" +
                        "- Signature simple (image)\n" +
                        "- Signature avancée avec certificat numérique (PKI/X.509)\n" +
                        "- Signature qualifiée conforme eIDAS\n" +
                        "- Workflow multi-signataires\n" +
                        "- Vérification d'authenticité des signatures\n" +
                        "- Téléchargement de documents signés\n\n" +
                        "**Types de signature supportés :**\n" +
                        "- `SIMPLE` : Signature visuelle (image)\n" +
                        "- `ADVANCED` : Signature avec certificat numérique\n" +
                        "- `QUALIFIED` : Signature qualifiée conforme eIDAS")
                .contact(contact)
                .license(license);
        
        return new OpenAPI()
                .info(info)
                .servers(List.of(server));
    }
}
