# SRE Coding Assignment

## Project Structure
```
.
├── user-metadata-service/          # Part 1 — Backend + Reliability
│   ├── src/main/java/com/sre/userservice/
│   │   ├── controller/             UserController.java
│   │   ├── service/                UserService.java
│   │   ├── repository/             UserRepository.java
│   │   ├── model/                  User.java, UserDto.java
│   │   └── metrics/                UserServiceMetrics.java
│   ├── src/main/resources/         application.yml
│   ├── Dockerfile
│   └── pom.xml
│
├── deployment-portal/              # Part 2 — IDP + CI/CD Backend
│   └── src/main/java/com/sre/portal/
│       ├── controller/             DeploymentPortalController.java
│       ├── service/                DeploymentPortalService.java
│       └── model/                  PortalDto.java
│
├── terraform/
│   ├── environments/dev/           main.tf, variables.tf, outputs.tf
│   └── modules/
│       ├── ecr/                    ECR repository
│       ├── iam/                    IAM role with ECR pull policy
│       └── k8s/                    K8s Namespace, Deployment, Service
│
├── cicd/
│   ├── .gitlab-ci.yml
│   └── Jenkinsfile
│
└── api-spec.yaml                   OpenAPI 3.0
```

---

## Part 1 — User Metadata Service

### APIs

**POST /user**
```bash
curl -X POST http://localhost:8080/user \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: key-001" \
  -d '{"userId":"u-1","name":"Alice","email":"alice@test.com","phone":"9999999999"}'
```

**GET /user/{id}**
```bash
curl http://localhost:8080/user/u-1
```

### Reliability implemented
| Requirement | Implementation |
|-------------|---------------|
| Idempotency | `Idempotency-Key` header — same key returns existing user, no duplicate created |
| Retry + exponential backoff + jitter | Resilience4j `@Retry` — 3 attempts, 200ms base, 2x multiplier, 50% jitter |
| Circuit breaker for DB writes | Resilience4j `@CircuitBreaker` — opens after 50% failure rate over 10 calls |
| Metrics: total_requests | Micrometer counter |
| Metrics: success_count | Micrometer counter |
| Metrics: failure_count | Micrometer counter |
| Metrics: request_latency_ms | Micrometer timer |
| Log: Request ID | UUID logged on every request |
| Log: Latency | Logged on every request |
| Log: Error summary | Logged on every failure |
| Docker | Multi-stage Dockerfile |

### Build and run
```bash
cd user-metadata-service
docker build -t user-metadata-service .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host:5432/userdb \
  -e DB_USER=postgres \
  -e DB_PASS=postgres \
  user-metadata-service
```

---

## Part 2 — Deployment Portal

### API

**POST /api/v1/services/register**
```bash
curl -X POST http://localhost:8081/api/v1/services/register \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "payment-service",
    "teamName": "payments-team",
    "repoUrl": "https://gitlab.example.com/payments-team/payment-service"
  }'
```

**What it automatically creates:**
- ECR repository via `terraform apply`
- IAM role via `terraform apply`
- K8s deployment manifest file
- GitLab CI pipeline YAML file

### Terraform

```bash
cd terraform/environments/dev
terraform init
terraform apply \
  -var="service_name=payment-service" \
  -var="team_name=payments-team"
```

### CI/CD Pipeline stages
```
Build → Docker Push → Terraform Apply → Deploy to K8s
```
Both `.gitlab-ci.yml` and `Jenkinsfile` are provided.