package com.example.OldSchoolTeed.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class UnlockRequest {
    private String email;
    private String code;
    private String newPassword;
}