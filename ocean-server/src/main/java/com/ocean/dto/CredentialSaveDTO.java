package com.ocean.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CredentialSaveDTO {

    @NotBlank(message = "密钥类型不能为空")
    private String credentialKey;

    @NotBlank(message = "密钥值不能为空")
    private String credentialValue;
}
