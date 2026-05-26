package com.ocean.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ocean.common.BusinessException;
import com.ocean.dto.CredentialSaveDTO;
import com.ocean.entity.UserCredential;
import com.ocean.mapper.UserCredentialMapper;
import com.ocean.service.UserCredentialService;
import com.ocean.util.AesUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserCredentialServiceImpl implements UserCredentialService {

    @Autowired
    private UserCredentialMapper userCredentialMapper;

    @Value("${credential.encrypt.secret}")
    private String encryptSecret;

    @Override
    public List<Map<String, Object>> listCredentials(Long userId) {
        List<UserCredential> records = userCredentialMapper.selectList(
                new LambdaQueryWrapper<UserCredential>()
                        .eq(UserCredential::getUserId, userId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (UserCredential r : records) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", r.getId());
            item.put("credentialKey", r.getCredentialKey());
            String decrypted = AesUtil.decrypt(r.getCredentialValue(), encryptSecret);
            item.put("credentialValue", AesUtil.mask(decrypted));
            result.add(item);
        }
        return result;
    }

    @Override
    public void saveCredential(Long userId, CredentialSaveDTO dto) {
        LambdaQueryWrapper<UserCredential> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCredential::getUserId, userId)
               .eq(UserCredential::getCredentialKey, dto.getCredentialKey());
        UserCredential existing = userCredentialMapper.selectOne(wrapper);

        String encrypted = AesUtil.encrypt(dto.getCredentialValue(), encryptSecret);

        if (existing != null) {
            existing.setCredentialValue(encrypted);
            userCredentialMapper.updateById(existing);
        } else {
            UserCredential uc = new UserCredential();
            uc.setUserId(userId);
            uc.setCredentialKey(dto.getCredentialKey());
            uc.setCredentialValue(encrypted);
            userCredentialMapper.insert(uc);
        }
    }

    @Override
    public void deleteCredential(Long userId, Long credentialId) {
        UserCredential uc = userCredentialMapper.selectById(credentialId);
        if (uc == null || !uc.getUserId().equals(userId)) {
            throw new BusinessException("密钥不存在或无权操作");
        }
        userCredentialMapper.deleteById(credentialId);
    }

    @Override
    public String getCredentialValue(Long userId, String credentialKey) {
        LambdaQueryWrapper<UserCredential> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserCredential::getUserId, userId)
               .eq(UserCredential::getCredentialKey, credentialKey);
        UserCredential uc = userCredentialMapper.selectOne(wrapper);
        if (uc == null) return null;
        return AesUtil.decrypt(uc.getCredentialValue(), encryptSecret);
    }
}
