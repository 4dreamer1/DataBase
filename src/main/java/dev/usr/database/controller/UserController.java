package dev.usr.database.controller;

import dev.usr.database.entity.User;
import dev.usr.database.payload.request.ChangePasswordRequest;
import dev.usr.database.payload.request.CreateUserRequest;
import dev.usr.database.payload.request.UpdateProfileRequest;
import dev.usr.database.payload.request.UpdateUserRequest;
import dev.usr.database.payload.response.MessageResponse;
import dev.usr.database.security.services.UserDetailsImpl;
import dev.usr.database.service.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/users")
@Slf4j
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        
        log.info("获取用户列表 - 关键词: {}, 角色: {}, 状态: {}, 页码: {}, 每页数量: {}", 
                keyword, role, status, pageNum, pageSize);
        
        try {
            List<User> users = userService.findAll();
            
            // 应用筛选条件
            if (keyword != null && !keyword.isEmpty()) {
                String lowerKeyword = keyword.toLowerCase();
                users = users.stream()
                        .filter(user -> 
                            (user.getUsername() != null && user.getUsername().toLowerCase().contains(lowerKeyword)) ||
                            (user.getEmail() != null && user.getEmail().toLowerCase().contains(lowerKeyword)) ||
                            (user.getDepartment() != null && user.getDepartment().toLowerCase().contains(lowerKeyword)))
                        .collect(Collectors.toList());
            }
            
            if (role != null && !role.isEmpty()) {
                users = users.stream()
                        .filter(user -> user.getRoles().stream()
                                .anyMatch(r -> r.getName().toString().equals(role)))
                        .collect(Collectors.toList());
            }
            
            if (status != null) {
                // 假设User实体中有status字段，目前代码中并没有显示
                // users = users.stream().filter(user -> user.getStatus() == status).collect(Collectors.toList());
                users = users.stream().filter(user -> {
                    Integer userStatus = user.getStatus();
                    return userStatus != null && userStatus.equals(status);
                }).collect(Collectors.toList());
            }
            
            // 分页处理
            int startIndex = (pageNum - 1) * pageSize;
            int endIndex = Math.min(startIndex + pageSize, users.size());
            
            if (startIndex >= users.size()) {
                startIndex = 0;
                endIndex = Math.min(pageSize, users.size());
            }
            
            List<User> pagedUsers = startIndex < endIndex ? 
                    users.subList(startIndex, endIndex) : new ArrayList<>();
            
            // 返回标准的分页格式
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            data.put("list", pagedUsers);
            data.put("total", users.size());
            response.put("data", data);
            
            // 添加缓存控制头，确保前端不缓存响应
            HttpHeaders headers = new HttpHeaders();
            headers.setCacheControl("no-cache, no-store, must-revalidate");
            headers.setPragma("no-cache");
            headers.setExpires(0);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(response);
        } catch (Exception e) {
            log.error("获取用户列表出错", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("获取用户列表失败: " + e.getMessage()));
        }
    }
    
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest createUserRequest) {
        log.info("创建用户请求: username={}, email={}", createUserRequest.getUsername(), createUserRequest.getEmail());
        try {
            User createdUser = userService.createUser(createUserRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (IllegalArgumentException e) {
            log.warn("创建用户失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("创建用户时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("创建用户失败: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/profile")
    public ResponseEntity<User> getCurrentUserProfile(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        User user = userService.findById(userDetails.getId());
        return ResponseEntity.ok(user);
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest updateProfileRequest,
                                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
        User updatedUser = userService.updateProfile(userDetails.getId(), updateProfileRequest);
        return ResponseEntity.ok(updatedUser);
    }
    
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadAvatar(@RequestParam("avatar") MultipartFile file,
                                        @AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            User updatedUser = userService.uploadAvatar(userDetails.getId(), file);
            return ResponseEntity.ok(updatedUser);
        } catch (IOException e) {
            log.error("上传头像失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("上传头像失败: " + e.getMessage()));
        }
    }
    
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest,
                                         @AuthenticationPrincipal UserDetailsImpl userDetails) {
        userService.changePassword(userDetails.getId(), 
                                  changePasswordRequest.getOldPassword(), 
                                  changePasswordRequest.getPassword());
        return ResponseEntity.ok(new MessageResponse("密码已成功修改"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest updateUserRequest) {
        User updatedUser = userService.updateUser(id, updateUserRequest);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(new MessageResponse("用户已成功删除"));
    }
    
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> payload) {
        log.info("切换用户 {} 状态为 {}", id, payload.get("status"));
        
        try {
            Integer status = payload.get("status");
            User updatedUser = userService.toggleUserStatus(id, status);
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            log.warn("切换用户状态失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("切换用户状态时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("切换用户状态失败: " + e.getMessage()));
        }
    }
    
    @PutMapping("/{id}/password/reset")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> resetUserPassword(@PathVariable Long id) {
        log.info("重置用户 {} 密码", id);
        
        try {
            User updatedUser = userService.resetPassword(id);
            return ResponseEntity.ok(new MessageResponse("密码已重置为默认密码"));
        } catch (Exception e) {
            log.error("重置用户密码时发生异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new MessageResponse("重置密码失败: " + e.getMessage()));
        }
    }
} 