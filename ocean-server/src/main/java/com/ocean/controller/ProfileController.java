package com.ocean.controller;

import com.ocean.common.Result;
import com.ocean.dto.*;
import com.ocean.service.SysUserService;
import com.ocean.service.UserSettingService;
import com.ocean.service.UserCredentialService;
import com.ocean.vo.UserVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private UserSettingService userSettingService;

    @Autowired
    private UserCredentialService userCredentialService;

    @Value("${upload.avatar.dir:/data/ocean/uploads/avatars/}")
    private String avatarDir;

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif");
    private static final long MAX_SIZE = 2 * 1024 * 1024;

    private Long getUserId(HttpServletRequest request) {
        return (Long) request.getAttribute("userId");
    }

    @GetMapping
    public Result<UserVO> getProfile(HttpServletRequest request) {
        return Result.success(sysUserService.getUserById(getUserId(request)));
    }

    @PutMapping
    public Result<?> updateProfile(HttpServletRequest request,
                                   @Validated @RequestBody ProfileUpdateDTO dto) {
        UserSaveDTO saveDto = new UserSaveDTO();
        saveDto.setId(getUserId(request));
        saveDto.setUsername(dto.getUsername());
        saveDto.setRealName(dto.getRealName());
        saveDto.setPhone(dto.getPhone());
        sysUserService.updateUser(saveDto);
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
        File dir = new File(avatarDir);
        if (!dir.exists()) dir.mkdirs();

        String fileName = userId + "_" + System.currentTimeMillis() + "." + ext;
        try {
            file.transferTo(new File(dir, fileName));
        } catch (IOException e) {
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
