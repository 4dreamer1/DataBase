package dev.usr.database.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @Size(max = 50)
    private String name;
    
    @Size(max = 50)
    @Email
    private String email;
    
    @Size(max = 20)
    private String phone;
    
    @Size(max = 100)
    private String department;
    
    @Size(max = 255)
    private String avatar;
    
    @Size(min = 6, max = 40)
    private String password;
    
    private String oldPassword;
} 