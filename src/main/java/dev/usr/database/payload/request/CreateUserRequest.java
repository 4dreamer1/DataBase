package dev.usr.database.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

/**
 * 创建用户请求数据模型
 */
@Data
public class CreateUserRequest {
    @NotBlank
    @Size(min = 3, max = 20)
    private String username;
    
    @NotBlank
    @Size(max = 50)
    @Email
    private String email;
    
    @NotBlank
    @Size(min = 6, max = 40)
    private String password;
    
    @NotBlank
    @Size(max = 50)
    private String name;
    
    @Size(max = 20)
    private String phone;
    
    @Size(max = 100)
    private String department;
    
    private Set<String> roles;
} 