package dev.usr.database.payload.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Set;

@Data
public class UpdateUserRequest {
    @Size(min = 3, max = 20)
    private String username;
    
    @Size(max = 50)
    @Email
    private String email;
    
    @Size(min = 6, max = 40)
    private String password;
    
    @Size(max = 50)
    private String name;
    
    @Size(max = 20)
    private String phone;
    
    @Size(max = 100)
    private String department;
    
    private Set<String> roles;
} 