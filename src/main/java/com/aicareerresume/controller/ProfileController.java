package com.aicareerresume.controller;

import com.aicareerresume.dto.ApiResponse;
import com.aicareerresume.entity.MasterProfile;
import com.aicareerresume.security.UserPrincipal;
import com.aicareerresume.service.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<MasterProfile>> getProfile(@AuthenticationPrincipal UserPrincipal currentUser) {
        MasterProfile profile = profileService.getProfileByUserId(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("Profile retrieved", profile));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<MasterProfile>> updateProfile(@AuthenticationPrincipal UserPrincipal currentUser,
                                                                   @RequestBody MasterProfile payload) {
        MasterProfile updated = profileService.updateProfile(currentUser.getId(), payload);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", updated));
    }
}
