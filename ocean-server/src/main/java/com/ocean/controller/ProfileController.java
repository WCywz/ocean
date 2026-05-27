package com.ocean.controller;

import com.ocean.common.Result;
import com.ocean.dto.*;
import com.ocean.service.SysUserService;
import com.ocean.service.UserCredentialService;
import com.ocean.service.UserSettingService;
import com.ocean.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private UserSettingService userSettingService;

    @Autowired
    private UserCredentialService userCredentialService;

    @Value("${upload.avatar.dir:/data/ocean/uploads/avatars/}")
    private String avatarDir;

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif");
    private static final long MAX_SIZE = 5 * 1024 * 1024;

    private Long getUserId(HttpServletRequest request) {
        return ((Number) request.getAttribute("userId")).longValue();
    }

    @GetMapping
    public Result<UserVO> getProfile(HttpServletRequest request) {
        return Result.success(sysUserService.getUserById(getUserId(request)));
    }

    @PutMapping
    public Result<?> updateProfile(HttpServletRequest request,
                                   @Validated @RequestBody ProfileUpdateDTO dto) {
        sysUserService.updateProfile(getUserId(request), dto);
        return Result.success("更新成功");
    }

    @PutMapping("/password")
    public Result<?> changePassword(HttpServletRequest request,
                                    @Validated @RequestBody PasswordChangeDTO dto) {
        sysUserService.changePassword(getUserId(request), dto);
        return Result.success("密码修改成功");
    }

    @PostMapping("/avatar")
    public Result<Map<String, String>> uploadAvatar(HttpServletRequest request,
                                                    @RequestParam("file") MultipartFile file) {
        if (file.getSize() > MAX_SIZE) {
            return Result.error("文件大小不能超过2MB");
        }
        String original = file.getOriginalFilename();
        if (original == null || !original.contains(".")) {
            return Result.error("无效的文件");
        }
        String ext = original.substring(original.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXT.contains(ext)) {
            return Result.error("只支持 jpg、png、gif 格式");
        }

        Long userId = getUserId(request);

        // Resolve upload directory
        File dir = new File(avatarDir);
        if (!dir.isAbsolute()) {
            dir = new File(System.getProperty("user.dir"), avatarDir);
        }
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (!created) {
                log.error("无法创建上传目录: {}", dir.getAbsolutePath());
                return Result.error("上传目录创建失败");
            }
        }

        // Delete old avatar file
        UserVO currentUser = sysUserService.getUserById(userId);
        if (currentUser.getAvatarUrl() != null && !currentUser.getAvatarUrl().isEmpty()) {
            String oldPath = currentUser.getAvatarUrl().replace("/uploads/avatars/", "");
            File oldFile = new File(dir, oldPath);
            if (oldFile.isFile()) oldFile.delete();
        }

        String fileName = userId + "_" + System.currentTimeMillis() + "." + ext;
        Path dest = dir.toPath().resolve(fileName);
        try {
            Files.copy(file.getInputStream(), dest, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("头像写入失败: {}", dest.toAbsolutePath(), e);
            return Result.error("头像上传失败");
        }

        String avatarUrl = "/uploads/avatars/" + fileName;
        sysUserService.updateAvatar(userId, avatarUrl);

        Map<String, String> data = new HashMap<>();
        data.put("avatarUrl", avatarUrl);
        return Result.success("上传成功", data);
    }

    @GetMapping("/settings")
    public Result<Map<String, String>> getSettings(HttpServletRequest request) {
        return Result.success(userSettingService.getUserSettings(getUserId(request)));
    }

    @PutMapping("/settings")
    public Result<?> updateSettings(HttpServletRequest request,
                                    @Validated @RequestBody SettingsUpdateDTO dto) {
        userSettingService.updateSettings(getUserId(request), dto.getSettings());
        return Result.success("设置已更新");
    }

    @GetMapping("/credentials")
    public Result<List<Map<String, Object>>> getCredentials(HttpServletRequest request) {
        return Result.success(userCredentialService.listCredentials(getUserId(request)));
    }

    @PostMapping("/credentials")
    public Result<?> saveCredential(HttpServletRequest request,
                                    @Validated @RequestBody CredentialSaveDTO dto) {
        userCredentialService.saveCredential(getUserId(request), dto);
        return Result.success("密钥保存成功");
    }

    @DeleteMapping("/credentials/{id}")
    public Result<?> deleteCredential(HttpServletRequest request, @PathVariable Long id) {
        userCredentialService.deleteCredential(getUserId(request), id);
        return Result.success("密钥已删除");
    }
}
