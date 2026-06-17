package com.easyfamily.feedback.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

@Service
public class GithubIssueService {

    private static final Logger log = LoggerFactory.getLogger(GithubIssueService.class);
    private static final String GITHUB_API = "https://api.github.com";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;

    @Value("${easyfamily.github.token:}")
    private String githubToken;

    @Value("${easyfamily.github.repo:MikeDeng29/Easyfamily}")
    private String githubRepo;

    public GithubIssueService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void createIssue(String title, String body) {
        if (githubToken == null || githubToken.isBlank()) {
            log.warn("GitHub token not configured, skipping issue creation");
            return;
        }
        try {
            Map<String, Object> payload = Map.of(
                    "title", title,
                    "body", body,
                    "labels", List.of("user-feedback")
            );
            String json = objectMapper.writeValueAsString(payload);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GITHUB_API + "/repos/" + githubRepo + "/issues"))
                    .header("Authorization", "Bearer " + githubToken)
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", "2022-11-28")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 201) {
                log.error("GitHub issue creation failed: {} {}", response.statusCode(), response.body());
            } else {
                log.info("GitHub issue created for repo {}", githubRepo);
            }
        } catch (Exception e) {
            log.error("Failed to create GitHub issue", e);
        }
    }
}
