package dev.usr.database.service;

import dev.usr.database.entity.User;
import dev.usr.database.payload.request.CreateUserRequest;
import dev.usr.database.payload.request.UpdateProfileRequest;
import dev.usr.database.payload.request.UpdateUserRequest;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface UserService {
    List<User> findAll();
    
    User findById(Long id);
    
    User findByUsername(String username);
    
    User createUser(CreateUserRequest createUserRequest);
    
    User updateProfile(Long id, UpdateProfileRequest updateProfileRequest);
    
    User updateUser(Long id, UpdateUserRequest updateUserRequest);
    
    void delete(Long id);
    
    void changePassword(Long id, String oldPassword, String newPassword);
    
    User toggleUserStatus(Long id, Integer status);
    
    User resetPassword(Long id);
    
    User uploadAvatar(Long id, MultipartFile file) throws IOException;
    
    List<String> getDefaultAvatars() throws IOException;
    
    User selectDefaultAvatar(Long id, String avatarPath);
} 