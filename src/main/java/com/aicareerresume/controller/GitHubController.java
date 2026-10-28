package com.aicareerresume.controller;

import com.aicareerresume.dto.ApiResponse;
import com.aicareerresume.exception.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/github")
public class GitHubController {

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/analyze")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeGitHub(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        if (username == null || username.trim().isEmpty()) {
            throw new BadRequestException("Please provide a valid GitHub username.");
        }

        try {
            // Live GitHub API Call
            String url = "https://api.github.com/users/" + username.trim() + "/repos?sort=updated&per_page=15";
            List<Map<String, Object>> repos = restTemplate.getForObject(url, List.class);

            if (repos == null || repos.isEmpty()) {
                throw new BadRequestException("No public repositories found for user: " + username);
            }

            Map<String, Integer> langDist = new HashMap<>();
            List<Map<String, Object>> realRepos = new ArrayList<>();

            for (Map<String, Object> r : repos) {
                Boolean isFork = (Boolean) r.getOrDefault("fork", false);
                if (Boolean.TRUE.equals(isFork)) continue; // Filter out forks to show genuine work

                String lang = (String) r.get("language");
                if (lang != null) {
                    langDist.put(lang, langDist.getOrDefault(lang, 0) + 1);
                }

                int stars = (Integer) r.getOrDefault("stargazers_count", 0);
                realRepos.add(Map.of(
                        "name", r.get("name"),
                        "description", r.getOrDefault("description", "Public open-source repository"),
                        "language", lang != null ? lang : "Code",
                        "stars", stars,
                        "url", r.get("html_url")
                ));
            }

            // Sort by stars & activity
            realRepos.sort((a, b) -> Integer.compare((Integer) b.get("stars"), (Integer) a.get("stars")));

            Map<String, Object> result = Map.of(
                    "username", username,
                    "topLanguages", langDist,
                    "recommendedProjectsForResume", realRepos.stream().limit(6).toList(),
                    "recommendations", List.of(
                            "Pin your highest-quality repositories to the top of your GitHub profile.",
                            "Ensure each repository features a complete README with installation and architecture details."
                    )
            );

            return ResponseEntity.ok(ApiResponse.ok("GitHub analysis completed successfully", result));

        } catch (HttpClientErrorException.NotFound ex) {
            throw new BadRequestException("GitHub username '" + username + "' does not exist. Please check spelling.");
        } catch (Exception e) {
            throw new BadRequestException("Could not query GitHub API: " + e.getMessage());
        }
    }
}
