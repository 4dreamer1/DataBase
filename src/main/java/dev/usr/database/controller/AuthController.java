package dev.usr.database.controller;

import dev.usr.database.entity.ERole;
import dev.usr.database.entity.Role;
import dev.usr.database.entity.User;
import dev.usr.database.payload.request.LoginRequest;
import dev.usr.database.payload.request.SignupRequest;
import dev.usr.database.payload.response.JwtResponse;
import dev.usr.database.payload.response.MessageResponse;
import dev.usr.database.repository.RoleRepository;
import dev.usr.database.repository.UserRepository;
import dev.usr.database.security.jwt.JwtUtils;
import dev.usr.database.security.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;

@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private JwtUtils jwtUtils;

    private ResponseEntity<?> doAuthenticate(LoginRequest loginRequest) {
        log.info("用户登录: {}", loginRequest.getUsername());
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(), 
                            loginRequest.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            String jwt = jwtUtils.generateJwtToken(authentication);
            UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
            
            List<String> roles = userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());

            log.info("用户 {} 登录成功", userDetails.getUsername());
            
            return ResponseEntity.ok(new JwtResponse(
                    jwt, userDetails.getId(), userDetails.getUsername(),
                    userDetails.getEmail(), userDetails.getName(), roles));
        } catch (Exception e) {
            log.error("登录失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new MessageResponse("用户名或密码错误"));
        }
    }

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        return doAuthenticate(loginRequest);
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest) {
        return doAuthenticate(loginRequest);
    }

    private ResponseEntity<?> doRegister(SignupRequest signUpRequest) {
        log.info("注册请求: username={}, email={}", signUpRequest.getUsername(), signUpRequest.getEmail());
        
        if (userRepository.existsByUsername(signUpRequest.getUsername())) {
            log.warn("注册失败: 用户名 {} 已被使用", signUpRequest.getUsername());
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("错误: 用户名已被使用!"));
        }

        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            log.warn("注册失败: 邮箱 {} 已被使用", signUpRequest.getEmail());
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("错误: 邮箱已被使用!"));
        }
        
        if (!signUpRequest.getPassword().equals(signUpRequest.getConfirmPassword())) {
            log.warn("注册失败: 两次密码输入不一致");
            return ResponseEntity
                    .badRequest()
                    .body(new MessageResponse("错误: 两次密码输入不一致!"));
        }

        User user = new User(signUpRequest.getUsername(),
                signUpRequest.getEmail(),
                encoder.encode(signUpRequest.getPassword()),
                signUpRequest.getName());

        Set<String> strRoles = signUpRequest.getRoles();
        Set<Role> roles = new HashSet<>();

        if (strRoles == null) {
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("错误: 未找到用户角色"));
            roles.add(userRole);
        } else {
            strRoles.forEach(role -> {
                if ("admin".equals(role)) {
                        Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                                .orElseThrow(() -> new RuntimeException("错误: 未找到管理员角色"));
                        roles.add(adminRole);
                } else {
                        Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                                .orElseThrow(() -> new RuntimeException("错误: 未找到用户角色"));
                        roles.add(userRole);
                }
            });
        }

        user.setRoles(roles);
        userRepository.save(user);

        log.info("用户 {} 注册成功", user.getUsername());
        return ResponseEntity.ok(new MessageResponse("用户注册成功!"));
    }
    
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signUpRequest) {
        return doRegister(signUpRequest);
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> signupUser(@Valid @RequestBody SignupRequest signUpRequest) {
        return doRegister(signUpRequest);
    }
} 