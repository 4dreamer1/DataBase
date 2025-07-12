package dev.usr.database.service.impl;

import dev.usr.database.entity.ERole;
import dev.usr.database.entity.Role;
import dev.usr.database.entity.User;
import dev.usr.database.payload.request.CreateUserRequest;
import dev.usr.database.payload.request.UpdateProfileRequest;
import dev.usr.database.payload.request.UpdateUserRequest;
import dev.usr.database.repository.RoleRepository;
import dev.usr.database.repository.UserRepository;
import dev.usr.database.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder encoder;
    
    @Value("${app.upload.dir:uploads/avatars}")
    private String uploadDir;

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("未找到用户: " + id));
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("未找到用户: " + username));
    }
    
    @Override
    @Transactional
    public User createUser(CreateUserRequest createUserRequest) {
        log.info("创建新用户: {}", createUserRequest.getUsername());
        
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(createUserRequest.getUsername())) {
            log.warn("用户名已存在: {}", createUserRequest.getUsername());
            throw new IllegalArgumentException("用户名已存在: " + createUserRequest.getUsername());
        }
        
        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(createUserRequest.getEmail())) {
            log.warn("邮箱已存在: {}", createUserRequest.getEmail());
            throw new IllegalArgumentException("邮箱已存在: " + createUserRequest.getEmail());
        }
        
        // 创建新用户
        User user = new User(
            createUserRequest.getUsername(),
            createUserRequest.getEmail(),
            encoder.encode(createUserRequest.getPassword()),
            createUserRequest.getName()
        );
        
        // 设置其他字段
        user.setPhone(createUserRequest.getPhone());
        user.setDepartment(createUserRequest.getDepartment());
        
        // 设置角色
        Set<String> strRoles = createUserRequest.getRoles();
        Set<Role> roles = new HashSet<>();
        
        log.info("处理用户角色: {}", strRoles);
        
        if (strRoles == null || strRoles.isEmpty()) {
            // 如果没有指定角色，默认设置为普通用户
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("错误: 未找到用户角色"));
            roles.add(userRole);
            log.info("使用默认角色: ROLE_USER");
        } else {
            strRoles.forEach(role -> {
                // 规范化角色名称，忽略大小写和ROLE_前缀
                String normalizedRole = role.toLowerCase().replace("role_", "");
                
                log.info("处理角色: {}, 规范化后: {}", role, normalizedRole);
                
                switch (normalizedRole) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("错误: 未找到管理员角色"));
                        roles.add(adminRole);
                        log.info("添加角色: ROLE_ADMIN");
                        break;
                    case "user":
                    default:
                        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("错误: 未找到用户角色"));
                        roles.add(userRole);
                        log.info("添加角色: ROLE_USER");
                }
            });
        }
        
        user.setRoles(roles);
        
        // 保存用户
        User savedUser = userRepository.save(user);
        log.info("用户创建成功: {}", savedUser.getUsername());
        
        return savedUser;
    }

    @Override
    @Transactional
    public User updateProfile(Long id, UpdateProfileRequest updateProfileRequest) {
        User user = findById(id);
        
        if (updateProfileRequest.getName() != null) {
            user.setName(updateProfileRequest.getName());
        }
        
        if (updateProfileRequest.getEmail() != null &&
                !updateProfileRequest.getEmail().equals(user.getEmail()) &&
                userRepository.existsByEmail(updateProfileRequest.getEmail())) {
            throw new IllegalArgumentException("邮箱已存在: " + updateProfileRequest.getEmail());
        }
        
        if (updateProfileRequest.getEmail() != null) {
            user.setEmail(updateProfileRequest.getEmail());
        }
        
        if (updateProfileRequest.getPhone() != null) {
            user.setPhone(updateProfileRequest.getPhone());
        }
        
        if (updateProfileRequest.getDepartment() != null) {
            user.setDepartment(updateProfileRequest.getDepartment());
        }
        
        if (updateProfileRequest.getAvatar() != null) {
            user.setAvatar(updateProfileRequest.getAvatar());
        }
        
        if (updateProfileRequest.getPassword() != null && updateProfileRequest.getOldPassword() != null) {
            if (!encoder.matches(updateProfileRequest.getOldPassword(), user.getPassword())) {
                throw new IllegalArgumentException("原密码不正确");
            }
            user.setPassword(encoder.encode(updateProfileRequest.getPassword()));
        }
        
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateUser(Long id, UpdateUserRequest updateUserRequest) {
        User user = findById(id);
        
        if (updateUserRequest.getUsername() != null &&
                !updateUserRequest.getUsername().equals(user.getUsername()) &&
                userRepository.existsByUsername(updateUserRequest.getUsername())) {
            throw new IllegalArgumentException("用户名已存在: " + updateUserRequest.getUsername());
        }
        
        if (updateUserRequest.getUsername() != null) {
            user.setUsername(updateUserRequest.getUsername());
        }
        
        if (updateUserRequest.getEmail() != null &&
                !updateUserRequest.getEmail().equals(user.getEmail()) &&
                userRepository.existsByEmail(updateUserRequest.getEmail())) {
            throw new IllegalArgumentException("邮箱已存在: " + updateUserRequest.getEmail());
        }
        
        if (updateUserRequest.getEmail() != null) {
            user.setEmail(updateUserRequest.getEmail());
        }
        
        if (updateUserRequest.getName() != null) {
            user.setName(updateUserRequest.getName());
        }
        
        if (updateUserRequest.getPhone() != null) {
            user.setPhone(updateUserRequest.getPhone());
        }
        
        if (updateUserRequest.getDepartment() != null) {
            user.setDepartment(updateUserRequest.getDepartment());
        }
        
        if (updateUserRequest.getPassword() != null) {
            user.setPassword(encoder.encode(updateUserRequest.getPassword()));
        }
        
        if (updateUserRequest.getRoles() != null && !updateUserRequest.getRoles().isEmpty()) {
            Set<Role> roles = new HashSet<>();
            
            updateUserRequest.getRoles().forEach(role -> {
                switch (role) {
                    case "admin":
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("错误: 未找到管理员角色"));
                        roles.add(adminRole);
                        break;
                    case "user":
                        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("错误: 未找到用户角色"));
                        roles.add(userRole);
                        break;
                    default:
                        throw new IllegalArgumentException("未知角色: " + role);
                }
            });
            
            user.setRoles(roles);
        }
        
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        User user = findById(id);
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public void changePassword(Long id, String oldPassword, String newPassword) {
        User user = findById(id);
        
        if (!encoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("原密码不正确");
        }
        
        user.setPassword(encoder.encode(newPassword));
        userRepository.save(user);
    }
    
    @Override
    @Transactional
    public User uploadAvatar(Long id, MultipartFile file) throws IOException {
        User user = findById(id);
        
        // 创建上传目录（如果不存在）
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        // 生成唯一文件名
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        if (originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = UUID.randomUUID().toString() + fileExtension;
        
        // 保存文件
        Path filePath = uploadPath.resolve(filename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // 更新用户头像路径
        String avatarUrl = "/api/uploads/avatars/" + filename;
        user.setAvatar(avatarUrl);
        
        return userRepository.save(user);
    }
    
    @Override
    @Transactional
    public User toggleUserStatus(Long id, Integer status) {
        User user = findById(id);
        
        // 验证状态值
        if (status != 0 && status != 1) {
            throw new IllegalArgumentException("无效的状态值: " + status + "，状态值必须为0(正常)或1(禁用)");
        }
        
        user.setStatus(status);
        return userRepository.save(user);
    }
    
    @Override
    @Transactional
    public User resetPassword(Long id) {
        User user = findById(id);
        
        // 重置为默认密码 "123456"
        user.setPassword(encoder.encode("123456"));
        
        return userRepository.save(user);
    }
    
    @Override
    public List<String> getDefaultAvatars() throws IOException {
        Path avatarsPath = Paths.get("ui/public/images");
        if (Files.exists(avatarsPath)) {
            return Files.list(avatarsPath)
                    .filter(path -> {
                        String fileName = path.getFileName().toString().toLowerCase();
                        return fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg");
                    })
                    .map(path -> "/images/" + path.getFileName().toString())
                    .toList();
        }
        return List.of("/images/default-avatar.png");
    }
    
    @Override
    @Transactional
    public User selectDefaultAvatar(Long id, String avatarPath) {
        User user = findById(id);
        user.setAvatar(avatarPath);
        return userRepository.save(user);
    }
} 