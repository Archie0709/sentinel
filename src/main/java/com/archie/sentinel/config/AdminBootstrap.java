package com.archie.sentinel.config;

import com.archie.sentinel.model.User;
import com.archie.sentinel.repository.RoleRepository;
import com.archie.sentinel.repository.UserRepository;
import com.archie.sentinel.security.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminBootstrap(UserRepository userRepository,
                          RoleRepository roleRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${sentinel.bootstrap.admin-username:admin}") String username,
                          @Value("${sentinel.bootstrap.admin-password:}") String password) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (password.isBlank() || userRepository.count() > 0) {
            return;
        }
        var adminRole = roleRepository.findByName(Roles.ADMIN).orElseThrow();
        userRepository.save(new User(username, passwordEncoder.encode(password), adminRole));
        log.info("Created initial administrator '{}'", username);
    }
}