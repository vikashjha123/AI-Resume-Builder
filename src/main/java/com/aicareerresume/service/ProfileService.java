package com.aicareerresume.service;

import com.aicareerresume.entity.MasterProfile;

public interface ProfileService {
    MasterProfile getProfileByUserId(Long userId);
    MasterProfile updateProfile(Long userId, MasterProfile updated);
    int calculateProfileCompletion(MasterProfile profile);
}