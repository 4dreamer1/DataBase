package dev.usr.database.config;

import dev.usr.database.entity.ERole;
import dev.usr.database.entity.Role;
import dev.usr.database.entity.User;
import dev.usr.database.repository.RoleRepository;
import dev.usr.database.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

//数据库初始化器
@Component
public class DatabaseSeeder implements CommandLineRunner {
    //注入角色和用户仓库
    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder encoder;

    //运行方法
    @Override
    public void run(String... args) throws Exception {
        System.out.println("====== 开始初始化数据库 ======");
        
        // 初始化角色
        initRoles();
        
        // 初始化管理员账户
        initAdminUser();
        
        System.out.println("====== 数据库初始化完成 ======");
    }

    //初始化角色
    private void initRoles() {
        if (roleRepository.count() == 0) {
            System.out.println("创建角色...");
            
            Role userRole = new Role();
            userRole.setName(ERole.ROLE_USER);
            roleRepository.save(userRole);
            System.out.println("创建了USER角色: " + userRole.getName());

            Role adminRole = new Role();
            adminRole.setName(ERole.ROLE_ADMIN);
            roleRepository.save(adminRole);
            System.out.println("创建了ADMIN角色: " + adminRole.getName());

            System.out.println("角色初始化完成");
        } else {
            System.out.println("角色已存在，跳过初始化");
        }
    }

    //初始化管理员账户
        private void initAdminUser() {
        String adminUsername = "admin";
        String adminPassword = "admin123";
        
        if (!userRepository.existsByUsername(adminUsername)) {
            System.out.println("创建管理员账户...");
            
            // 使用BCrypt加密密码
            String encodedPassword = encoder.encode(adminPassword);
            System.out.println("原始密码: " + adminPassword);
            System.out.println("加密后密码: " + encodedPassword);
            
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setEmail("admin@example.com");
            admin.setPassword(encodedPassword);
            admin.setName("系统管理员");
            
            Set<Role> roles = new HashSet<>();
            Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("错误: 未找到管理员角色"));
            roles.add(adminRole);
            admin.setRoles(roles);
            
            userRepository.save(admin);
            System.out.println("管理员账户创建成功: " + admin.getUsername());
        } else {
            // 如果管理员已存在，则更新密码
            System.out.println("管理员账户已存在，更新密码...");
            User admin = userRepository.findByUsername(adminUsername)
                    .orElseThrow(() -> new RuntimeException("管理员账户查询失败"));
            
            String encodedPassword = encoder.encode(adminPassword);
            admin.setPassword(encodedPassword);
            userRepository.save(admin);
            
            System.out.println("管理员密码已更新。原始密码: " + adminPassword);
            System.out.println("加密后密码: " + encodedPassword);
        }
    }
} 
 