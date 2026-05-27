package com.ocean.service;

import com.ocean.dto.CredentialSaveDTO;

import java.util.List;
import java.util.Map;

public interface UserCredentialService {

    List<Map<String, Object>> listCredentials(Long userId);

    void saveCredential(Long userId, CredentialSaveDTO dto);

    void deleteCredential(Long userId, Long credentialId);

    String getCredentialValue(Long userId, String credentialKey);
}
