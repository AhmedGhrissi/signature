# API de Signature Électronique

API REST complète développée avec **Spring Boot 3** et **Java 17** pour gérer des signatures électroniques multi-types sur des documents PDF.

## 🎯 Fonctionnalités

### Types de signatures supportés
- ✅ **Signature Simple** : Application d'une image de signature sur le PDF
- ✅ **Signature Avancée** : Signature cryptographique avec certificat numérique (PKI/X.509)
- ✅ **Signature Qualifiée** : Conforme eIDAS avec certificat qualifié

### Capacités principales
- 📤 Upload de documents PDF
- ✍️ Signature de documents avec plusieurs types
- 👥 Workflow multi-signataires avec ordre de signature
- ✅ Vérification d'authenticité des signatures
- 📥 Téléchargement de documents signés
- 🔐 Gestion des certificats numériques
- 📊 Suivi du statut des signatures

## 🏗️ Architecture

```
esignature-api/
├── src/main/java/com/esignature/
│   ├── config/              # Configuration Spring
│   ├── controller/          # Endpoints REST
│   ├── service/             # Logique métier
│   ├── repository/          # Accès données JPA
│   ├── model/
│   │   ├── entity/          # Entités JPA
│   │   ├── dto/             # DTOs pour API
│   │   └── enums/           # Énumérations
│   └── exception/           # Gestion des erreurs
└── src/main/resources/
    └── application.properties
```

## 🚀 Démarrage rapide

### Prérequis
- Java 17 ou supérieur
- Maven 3.6+
- (Optionnel) PostgreSQL pour production

### Installation

1. **Cloner et compiler**
```bash
mvn clean install
```

2. **Lancer l'application**
```bash
mvn spring-boot:run
```

L'API sera accessible sur `http://localhost:8080/api/v1`

### Documentation Swagger
Interface interactive disponible sur :
- Swagger UI : `http://localhost:8080/api/v1/swagger-ui.html`
- API Docs : `http://localhost:8080/api/v1/api-docs`

## 📚 Utilisation de l'API

### 1. Upload d'un document

```bash
curl -X POST http://localhost:8080/api/v1/documents/upload \
  -F "file=@document.pdf" \
  -F "uploadedBy=john.doe"
```

Réponse :
```json
{
  "id": 1,
  "name": "document.pdf",
  "status": "PENDING",
  "createdAt": "2026-02-15T10:00:00"
}
```

### 2. Signature simple (avec image)

```bash
curl -X POST http://localhost:8080/api/v1/documents/sign \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": 1,
    "signerName": "Jean Dupont",
    "signerEmail": "jean@example.com",
    "signatureType": "SIMPLE",
    "signatureImageBase64": "iVBORw0KGgoAAAANS...",
    "pageNumber": 0,
    "xPosition": 100,
    "yPosition": 100,
    "width": 150,
    "height": 50
  }'
```

### 3. Signature avancée (avec certificat)

```bash
curl -X POST http://localhost:8080/api/v1/documents/sign \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": 1,
    "signerName": "Marie Martin",
    "signerEmail": "marie@example.com",
    "signatureType": "ADVANCED",
    "certificateBase64": "MIIEvQIBADANBgkq...",
    "certificatePassword": "password123",
    "pageNumber": 0,
    "xPosition": 100,
    "yPosition": 200
  }'
```

### 4. Créer un workflow multi-signataires

```bash
curl -X POST http://localhost:8080/api/v1/documents/workflow \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": 1,
    "expirationDays": 7,
    "signers": [
      {
        "name": "Directeur",
        "email": "directeur@company.com",
        "signOrder": 1,
        "requiredSignatureType": "ADVANCED"
      },
      {
        "name": "RH Manager",
        "email": "rh@company.com",
        "signOrder": 2,
        "requiredSignatureType": "SIMPLE"
      }
    ]
  }'
```

### 5. Vérifier les signatures

```bash
curl -X GET http://localhost:8080/api/v1/documents/1/verify
```

Réponse :
```json
{
  "isValid": true,
  "message": "Toutes les signatures sont valides",
  "signatures": [
    {
      "signerName": "Jean Dupont",
      "signerEmail": "jean@example.com",
      "isValid": true,
      "signedAt": "2026-02-15T10:30:00",
      "certificateIssuer": "CN=CA Authority",
      "validationErrors": []
    }
  ]
}
```

### 6. Télécharger le document signé

```bash
curl -X GET http://localhost:8080/api/v1/documents/1/download \
  -o signed_document.pdf
```

## 🔐 Sécurité

### Certificats numériques

Pour les signatures avancées et qualifiées, utilisez des certificats au format PKCS#12 (.p12 ou .pfx).

**Générer un certificat de test :**
```bash
# Créer une paire de clés
keytool -genkeypair -alias testcert -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore certificate.p12 \
  -validity 365 -storepass password123
```

**Convertir en Base64 pour l'API :**
```bash
base64 -i certificate.p12 -o certificate_base64.txt
```

### Protection de l'API

Pour la production, ajoutez :
- Authentification JWT
- Rate limiting
- HTTPS obligatoire
- Validation des certificats avec une CA

## 🗄️ Configuration base de données

### H2 (Développement)
Configuration par défaut. Console H2 : `http://localhost:8080/api/v1/h2-console`

### PostgreSQL (Production)

Modifier `application.properties` :
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/esignature
spring.datasource.username=postgres
spring.datasource.password=yourpassword
spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect
```

## 📊 Modèle de données

### Entités principales

**Document**
- Informations du fichier
- Statut de signature
- Chemins fichiers (original/signé)

**Signature**
- Détails du signataire
- Type de signature
- Données cryptographiques
- Position sur le PDF

**SignatureWorkflow**
- Gestion multi-signataires
- Ordre de signature
- Tokens d'accès
- États et notifications

## 🔄 Workflow de signature

```
1. Upload document → Document créé (PENDING)
2. Créer workflow → Signataires notifiés
3. Signataire 1 signe → Notification signataire 2
4. Signataire 2 signe → Document SIGNED
5. Téléchargement disponible
```

## 🧪 Tests

```bash
# Tests unitaires
mvn test

# Tests d'intégration
mvn verify
```

## 📦 Dépendances principales

- **Spring Boot 3.2** : Framework
- **Apache PDFBox 3.0** : Manipulation PDF
- **BouncyCastle 1.77** : Cryptographie
- **Springdoc OpenAPI** : Documentation Swagger
- **H2/PostgreSQL** : Base de données

## 🚧 Améliorations futures

- [ ] Intégration avec des services de TSA (TimeStamp Authority)
- [ ] Support de formats additionnels (DOCX, images)
- [ ] Notifications email automatiques
- [ ] Interface web de gestion
- [ ] API de génération de certificats
- [ ] Support des signatures biométriques
- [ ] Audit trail complet
- [ ] Archivage long terme (LTV)

## 📝 Conformité

Cette API supporte les standards :
- **eIDAS** (Règlement UE 910/2014)
- **PAdES** (PDF Advanced Electronic Signatures)
- **PKCS#7/CMS** (Cryptographic Message Syntax)
- **X.509** (Certificats numériques)

## 🤝 Contribution

Les contributions sont bienvenues ! Pour contribuer :
1. Fork le projet
2. Créer une branche feature (`git checkout -b feature/AmazingFeature`)
3. Commit les changements (`git commit -m 'Add AmazingFeature'`)
4. Push vers la branche (`git push origin feature/AmazingFeature`)
5. Ouvrir une Pull Request

## 📄 Licence

Projet sous licence Apache 2.0

## 📧 Support

Pour toute question ou support :
- Email : support@esignature.com
- Documentation : `/swagger-ui.html`

---

Développé avec ❤️ pour la transformation numérique
