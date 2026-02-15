# 🚀 Guide de Démarrage Rapide - API Signature Électronique

## ⚡ Installation en 3 étapes

### 1️⃣ Prérequis
```bash
# Vérifier Java
java -version  # Doit être >= 17

# Vérifier Maven
mvn -version
```

### 2️⃣ Lancer l'application
```bash
cd esignature-api
mvn spring-boot:run
```

L'API sera disponible sur : **http://localhost:8080/api/v1**

### 3️⃣ Tester l'API
Ouvrir dans votre navigateur : **http://localhost:8080/api/v1/swagger-ui.html**

---

## 📝 Exemples d'utilisation rapide

### Exemple 1 : Signature Simple
```bash
# 1. Upload un PDF
curl -X POST http://localhost:8080/api/v1/documents/upload \
  -F "file=@contract.pdf" \
  -F "uploadedBy=jean.dupont"

# Réponse : {"id": 1, "status": "PENDING", ...}

# 2. Créer une image de signature (exemple base64)
# Convertir votre signature PNG en base64 :
base64 -i ma_signature.png

# 3. Signer le document
curl -X POST http://localhost:8080/api/v1/documents/sign \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": 1,
    "signerName": "Jean Dupont",
    "signerEmail": "jean@example.com",
    "signatureType": "SIMPLE",
    "signatureImageBase64": "iVBORw0KGgo...[VOTRE_IMAGE_BASE64]",
    "pageNumber": 0,
    "xPosition": 400,
    "yPosition": 50,
    "width": 150,
    "height": 50
  }'

# 4. Télécharger le PDF signé
curl http://localhost:8080/api/v1/documents/1/download -o contrat_signe.pdf
```

### Exemple 2 : Workflow Multi-Signataires
```bash
# 1. Upload document
curl -X POST http://localhost:8080/api/v1/documents/upload \
  -F "file=@contrat_emploi.pdf" \
  -F "uploadedBy=rh"

# 2. Créer workflow avec 2 signataires
curl -X POST http://localhost:8080/api/v1/documents/workflow \
  -H "Content-Type: application/json" \
  -d '{
    "documentId": 1,
    "expirationDays": 7,
    "signers": [
      {
        "name": "Manager",
        "email": "manager@company.com",
        "signOrder": 1,
        "requiredSignatureType": "ADVANCED"
      },
      {
        "name": "Employé",
        "email": "employe@company.com",
        "signOrder": 2,
        "requiredSignatureType": "SIMPLE"
      }
    ]
  }'

# Chaque signataire reçoit un token unique
# Le premier signe avec son token, puis le second est notifié
```

### Exemple 3 : Signature avec Certificat
```bash
# 1. Générer un certificat de test
keytool -genkeypair -alias testcert -keyalg RSA -keysize 2048 \
  -storetype PKCS12 -keystore certificate.p12 \
  -validity 365 -storepass password123

# 2. Convertir en base64
base64 -i certificate.p12 > cert_base64.txt

# 3. Signer avec le certificat
curl -X POST http://localhost:8080/api/v1/documents/sign \
  -H "Content-Type: application/json" \
  -d "{
    \"documentId\": 1,
    \"signerName\": \"Marie Martin\",
    \"signerEmail\": \"marie@example.com\",
    \"signatureType\": \"ADVANCED\",
    \"certificateBase64\": \"$(cat cert_base64.txt)\",
    \"certificatePassword\": \"password123\",
    \"pageNumber\": 0,
    \"xPosition\": 100,
    \"yPosition\": 200
  }"
```

---

## 🐳 Déploiement Docker (Production)

```bash
# 1. Construire et lancer avec Docker Compose
docker-compose up -d

# L'API sera sur http://localhost:8080
# PostgreSQL sur localhost:5432
# PgAdmin sur http://localhost:5050

# 2. Vérifier les logs
docker logs esignature-api -f

# 3. Arrêter
docker-compose down
```

---

## 🔧 Configuration de base

Modifier `application.properties` pour :

```properties
# Port de l'API
server.port=8080

# Taille max des fichiers
spring.servlet.multipart.max-file-size=50MB

# Base de données (changer pour PostgreSQL en prod)
spring.datasource.url=jdbc:postgresql://localhost:5432/esignature
spring.datasource.username=esignature
spring.datasource.password=VOTRE_MOT_DE_PASSE
```

---

## 📊 Endpoints principaux

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/documents/upload` | Upload un document |
| POST | `/documents/sign` | Signer un document |
| POST | `/documents/workflow` | Créer workflow multi-signataires |
| GET | `/documents/{id}` | Récupérer infos document |
| GET | `/documents/{id}/download` | Télécharger PDF signé |
| GET | `/documents/{id}/verify` | Vérifier signatures |
| GET | `/documents/{id}/workflow` | Voir workflow |

Documentation complète : **http://localhost:8080/api/v1/swagger-ui.html**

---

## ❓ Troubleshooting

### Problème : Port 8080 déjà utilisé
```bash
# Changer le port
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=9090
```

### Problème : Erreur de mémoire Java
```bash
# Augmenter la mémoire
export MAVEN_OPTS="-Xmx1024m"
mvn spring-boot:run
```

### Problème : Fichier trop volumineux
Modifier dans `application.properties`:
```properties
spring.servlet.multipart.max-file-size=100MB
spring.servlet.multipart.max-request-size=100MB
```

---

## 📚 Ressources

- **Documentation Swagger** : http://localhost:8080/api/v1/swagger-ui.html
- **Console H2** (dev) : http://localhost:8080/api/v1/h2-console
- **README complet** : Voir README.md
- **Collection Postman** : Importer postman_collection.json

---

## 🎯 Prochaines étapes

1. ✅ Tester les endpoints avec Swagger UI
2. ✅ Créer votre premier document signé
3. ✅ Tester un workflow multi-signataires
4. ⚙️ Configurer PostgreSQL pour production
5. 🔐 Ajouter l'authentification JWT
6. 📧 Configurer les notifications email

---

**Besoin d'aide ?** Consultez le README.md pour la documentation complète !
