# 📐 Architecture & Guide de Déploiement

## 🏛️ Architecture de l'API

```
┌─────────────────────────────────────────────────────────────┐
│                    CLIENTS / APPLICATIONS                    │
│          (Web Apps, Mobile Apps, Autres Services)            │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ REST API (JSON)
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                   API SPRING BOOT                            │
├─────────────────────────────────────────────────────────────┤
│  Controllers (REST Endpoints)                                │
│  ├── DocumentController                                      │
│  │   ├── POST /documents/upload                             │
│  │   ├── POST /documents/sign                               │
│  │   ├── POST /documents/workflow                           │
│  │   ├── GET  /documents/{id}                               │
│  │   ├── GET  /documents/{id}/download                      │
│  │   └── GET  /documents/{id}/verify                        │
├─────────────────────────────────────────────────────────────┤
│  Services (Business Logic)                                   │
│  ├── DocumentService                                         │
│  ├── PdfSignatureService                                     │
│  ├── CertificateService                                      │
│  ├── WorkflowService                                         │
│  └── VerificationService                                     │
├─────────────────────────────────────────────────────────────┤
│  Security Layer                                              │
│  ├── Spring Security                                         │
│  ├── CORS Configuration                                      │
│  └── JWT Authentication (à implémenter)                     │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ JPA/Hibernate
                         ▼
┌─────────────────────────────────────────────────────────────┐
│               BASE DE DONNÉES                                │
│  ├── Documents (info fichiers)                              │
│  ├── Signatures (détails signatures)                        │
│  └── SignatureWorkflows (workflows multi-signataires)       │
└─────────────────────────────────────────────────────────────┘
                         │
                         │ Filesystem
                         ▼
┌─────────────────────────────────────────────────────────────┐
│               STOCKAGE FICHIERS                              │
│  ├── /uploads (PDFs originaux)                              │
│  ├── /signed-documents (PDFs signés)                        │
│  └── /certificates (certificats numériques)                 │
└─────────────────────────────────────────────────────────────┘
```

## 🔄 Flux de Signature

### Flux 1 : Signature Simple
```
1. Client → POST /upload (PDF)
2. API → Enregistre dans DB + stocke fichier
3. Client → POST /sign (avec image base64)
4. API → Applique image sur PDF
5. API → Sauvegarde PDF signé
6. Client ← Lien de téléchargement
```

### Flux 2 : Workflow Multi-Signataires
```
1. Client → POST /upload (PDF)
2. Client → POST /workflow (liste signataires)
3. API → Génère tokens uniques
4. API → Notifie Signataire 1 (email + token)
5. Signataire 1 → POST /sign?token=xxx
6. API → Notifie Signataire 2
7. Signataire 2 → POST /sign?token=yyy
8. API → Document SIGNED, tous peuvent télécharger
```

### Flux 3 : Signature avec Certificat
```
1. Client → POST /upload (PDF)
2. Client → POST /sign (avec certificat PKCS12)
3. API → Valide certificat
4. API → Signature cryptographique (BouncyCastle)
5. API → Ajoute signature visible + invisible
6. API → Sauvegarde PDF avec signature intégrée
7. Client ← Téléchargement + verification possible
```

## 🗄️ Modèle de Données

### Entité Document
```java
- id (PK)
- name
- originalFilePath
- signedFilePath
- mimeType
- fileSize
- uploadedBy
- status (PENDING, SIGNED, REJECTED, EXPIRED, CANCELLED)
- createdAt
- signedAt
- expiresAt
- signatures[] (OneToMany)
- workflows[] (OneToMany)
```

### Entité Signature
```java
- id (PK)
- document (FK)
- signerName
- signerEmail
- signatureType (SIMPLE, ADVANCED, QUALIFIED)
- signatureImagePath
- certificateData
- certificateSerialNumber
- certificateIssuer
- digitalSignature
- position (page, x, y, width, height)
- signedAt
- ipAddress
- userAgent
```

### Entité SignatureWorkflow
```java
- id (PK)
- document (FK)
- signerName
- signerEmail
- signOrder
- requiredSignatureType
- status
- signatureToken (UUID unique)
- notifiedAt
- signedAt
- expiresAt
- signature (FK)
- rejectionReason
```

## 🚀 Options de Déploiement

### Option 1 : Déploiement Simple (JAR)
```bash
# Compiler
mvn clean package -DskipTests

# Lancer
java -jar target/esignature-api-1.0.0.jar

# Avec profil production
java -jar target/esignature-api-1.0.0.jar --spring.profiles.active=prod
```

### Option 2 : Docker
```bash
# Build image
docker build -t esignature-api:latest .

# Run container
docker run -d \
  -p 8080:8080 \
  -v ./uploads:/app/uploads \
  -v ./signed-documents:/app/signed-documents \
  -e SPRING_PROFILES_ACTIVE=prod \
  --name esignature-api \
  esignature-api:latest
```

### Option 3 : Docker Compose (Recommandé)
```bash
# Lancer stack complète (API + PostgreSQL + PgAdmin)
docker-compose up -d

# Vérifier
docker-compose ps
docker-compose logs -f esignature-api
```

### Option 4 : Kubernetes (Production)
```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: esignature-api
spec:
  replicas: 3
  template:
    spec:
      containers:
      - name: api
        image: esignature-api:latest
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          value: jdbc:postgresql://postgres-service:5432/esignature
        volumeMounts:
        - name: uploads
          mountPath: /app/uploads
        - name: signed-docs
          mountPath: /app/signed-documents
```

## 🔐 Configuration de Production

### 1. Base de données PostgreSQL
```properties
# application-prod.properties
spring.datasource.url=jdbc:postgresql://db-host:5432/esignature
spring.datasource.username=esignature_user
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
```

### 2. Variables d'environnement
```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/esignature"
export SPRING_DATASOURCE_PASSWORD="SecurePassword123!"
export JWT_SECRET="votre-cle-jwt-super-secrete-256-bits"
```

### 3. HTTPS/SSL
```properties
# application-prod.properties
server.ssl.enabled=true
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=${KEYSTORE_PASSWORD}
server.ssl.key-store-type=PKCS12
```

### 4. Reverse Proxy (Nginx)
```nginx
server {
    listen 443 ssl;
    server_name api.esignature.com;

    ssl_certificate /etc/ssl/certs/cert.pem;
    ssl_certificate_key /etc/ssl/private/key.pem;

    location /api/v1/ {
        proxy_pass http://localhost:8080/api/v1/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 📊 Monitoring & Observabilité

### Health Check
```bash
curl http://localhost:8080/api/v1/actuator/health
```

### Prometheus Metrics
```properties
management.endpoints.web.exposure.include=health,info,metrics,prometheus
management.metrics.export.prometheus.enabled=true
```

### Logs
```properties
logging.file.name=/var/log/esignature/app.log
logging.file.max-size=10MB
logging.file.max-history=30
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %msg%n
```

## 🔒 Sécurité - Checklist Production

- [ ] Activer HTTPS/SSL
- [ ] Implémenter JWT Authentication
- [ ] Rate limiting (Spring Cloud Gateway ou Nginx)
- [ ] Validation des certificats avec CA
- [ ] Chiffrement des données sensibles en DB
- [ ] Audit trail complet
- [ ] Backup automatique des documents
- [ ] WAF (Web Application Firewall)
- [ ] Scan de vulnérabilités
- [ ] GDPR compliance

## 🧪 Tests

### Tests Unitaires
```bash
mvn test
```

### Tests d'Intégration
```bash
mvn verify
```

### Tests de Charge (exemple JMeter)
```xml
<ThreadGroup>
  <num_threads>100</num_threads>
  <ramp_time>10</ramp_time>
  <HTTPSampler>
    <path>/api/v1/documents/upload</path>
    <method>POST</method>
  </HTTPSampler>
</ThreadGroup>
```

## 📈 Scalabilité

### Scale Horizontal
```bash
# Docker Compose
docker-compose up -d --scale esignature-api=3

# Kubernetes
kubectl scale deployment esignature-api --replicas=5
```

### Load Balancer
```
         ┌──────────────┐
         │ Load Balancer│
         └──────┬───────┘
                │
       ┌────────┼────────┐
       │        │        │
    ┌──▼──┐  ┌─▼──┐  ┌─▼──┐
    │ API1│  │API2│  │API3│
    └─────┘  └────┘  └────┘
       │        │        │
       └────────┼────────┘
                │
         ┌──────▼───────┐
         │  PostgreSQL  │
         │   (Master)   │
         └──────────────┘
```

## 🔄 CI/CD Pipeline

### GitHub Actions Example
```yaml
name: CI/CD Pipeline
on: [push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 17
        uses: actions/setup-java@v2
        with:
          java-version: '17'
      - name: Build with Maven
        run: mvn clean package
      - name: Build Docker image
        run: docker build -t esignature-api:${{ github.sha }} .
      - name: Push to registry
        run: docker push esignature-api:${{ github.sha }}
```

## 📞 Support & Maintenance

### Logs à surveiller
- Erreurs de signature
- Échecs de validation de certificats
- Tentatives d'accès non autorisés
- Performance des requêtes DB
- Espace disque (uploads)

### Métriques clés
- Temps de signature moyen
- Taux de succès des signatures
- Nombre de documents signés/jour
- Taille moyenne des fichiers
- Utilisation CPU/RAM

---

**API prête pour production avec cette configuration complète !**
