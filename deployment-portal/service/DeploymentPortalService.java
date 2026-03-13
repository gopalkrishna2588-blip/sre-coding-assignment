package com.sre.portal.service;

import com.sre.portal.model.PortalDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DeploymentPortalService {

    @Value("${terraform.dir:./terraform/environments/dev}")
    private String terraformDir;

    /**
     * Registers a new microservice by automatically creating:
     * - ECR repository
     * - IAM role
     * - K8s deployment manifest
     * - Jenkins/GitLab pipeline YAML
     */
    public PortalDto.RegisterServiceResponse registerService(PortalDto.RegisterServiceRequest req) {
        log.info("Registering service={} team={}", req.getServiceName(), req.getTeamName());

        List<String> created = new ArrayList<>();

        // Step 1: Run terraform init + apply to create ECR repo and IAM role
        runTerraform(req.getServiceName(), req.getTeamName());
        created.add("ECR repository: " + req.getTeamName() + "-" + req.getServiceName());
        created.add("IAM role: " + req.getTeamName() + "-" + req.getServiceName() + "-role");

        // Step 2: Generate K8s deployment manifest file
        generateK8sManifest(req.getServiceName(), req.getTeamName());
        created.add("K8s manifest: k8s/" + req.getServiceName() + "/deployment.yaml");

        // Step 3: Generate pipeline YAML file
        generatePipelineYaml(req.getServiceName(), req.getTeamName(), req.getRepoUrl());
        created.add("Pipeline config: pipelines/" + req.getServiceName() + "/.gitlab-ci.yml");

        return PortalDto.RegisterServiceResponse.builder()
                .serviceName(req.getServiceName())
                .teamName(req.getTeamName())
                .repoUrl(req.getRepoUrl())
                .createdResources(created)
                .registeredAt(Instant.now())
                .build();
    }

    // ── Terraform (ECR + IAM) ─────────────────────────────────────────────────

    private void runTerraform(String serviceName, String teamName) {
        try {
            log.info("Running terraform init...");
            runCommand(new String[]{
                    "terraform", "init", "-input=false"
            }, terraformDir);

            log.info("Running terraform apply...");
            runCommand(new String[]{
                    "terraform", "apply", "-input=false", "-auto-approve",
                    "-var=service_name=" + serviceName,
                    "-var=team_name=" + teamName
            }, terraformDir);

            log.info("Terraform apply completed for service={}", serviceName);
        } catch (Exception e) {
            log.error("Terraform failed: {}", e.getMessage());
            throw new RuntimeException("Failed to provision infrastructure: " + e.getMessage());
        }
    }

    private void runCommand(String[] command, String workingDir) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new java.io.File(workingDir));
        pb.redirectErrorStream(true);

        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("[terraform] {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed with exit code: " + exitCode);
        }
    }

    // ── K8s Manifest ──────────────────────────────────────────────────────────

    private void generateK8sManifest(String serviceName, String teamName) {
        String manifest = """
apiVersion: apps/v1
kind: Deployment
metadata:
  name: %s
  namespace: %s
spec:
  replicas: 1
  selector:
    matchLabels:
      app: %s
  template:
    metadata:
      labels:
        app: %s
    spec:
      containers:
        - name: %s
          image: %s-ACCOUNT.dkr.ecr.ap-south-1.amazonaws.com/%s:latest
          ports:
            - containerPort: 8080
---
apiVersion: v1
kind: Service
metadata:
  name: %s
  namespace: %s
spec:
  selector:
    app: %s
  ports:
    - port: 80
      targetPort: 8080
  type: ClusterIP
""".formatted(
                serviceName, teamName,
                serviceName, serviceName,
                serviceName, teamName, serviceName,
                serviceName, teamName, serviceName);

        writeFile("k8s/" + serviceName + "/deployment.yaml", manifest);
        log.info("K8s manifest generated for service={}", serviceName);
    }

    // ── Pipeline YAML ─────────────────────────────────────────────────────────

    private void generatePipelineYaml(String serviceName, String teamName, String repoUrl) {
        String pipeline = """
# Auto-generated pipeline for %s
# Repo: %s

stages:
  - build
  - push
  - deploy

variables:
  SERVICE_NAME: "%s"
  TEAM_NAME: "%s"
  ECR_URI: "$AWS_ACCOUNT_ID.dkr.ecr.ap-south-1.amazonaws.com/%s"

build:
  stage: build
  script:
    - mvn clean package -DskipTests
  artifacts:
    paths:
      - target/*.jar

docker-push:
  stage: push
  script:
    - aws ecr get-login-password | docker login --username AWS --password-stdin $ECR_URI
    - docker build -t $ECR_URI:$CI_COMMIT_SHORT_SHA .
    - docker push $ECR_URI:$CI_COMMIT_SHORT_SHA
  only:
    - main

deploy:
  stage: deploy
  script:
    - kubectl set image deployment/%s %s=$ECR_URI:$CI_COMMIT_SHORT_SHA -n %s
    - kubectl rollout status deployment/%s -n %s
  only:
    - main
""".formatted(
                serviceName, repoUrl,
                serviceName, teamName, serviceName,
                serviceName, serviceName, teamName,
                serviceName, teamName);

        writeFile("pipelines/" + serviceName + "/.gitlab-ci.yml", pipeline);
        log.info("Pipeline YAML generated for service={}", serviceName);
    }

    // ── File Writer ───────────────────────────────────────────────────────────

    private void writeFile(String relativePath, String content) {
        try {
            java.io.File file = new java.io.File(relativePath);
            file.getParentFile().mkdirs();
            java.nio.file.Files.writeString(file.toPath(), content);
        } catch (Exception e) {
            throw new RuntimeException("Failed to write file " + relativePath + ": " + e.getMessage());
        }
    }
}