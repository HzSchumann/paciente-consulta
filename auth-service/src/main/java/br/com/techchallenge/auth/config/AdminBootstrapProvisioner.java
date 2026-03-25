package br.com.techchallenge.auth.config;

import br.com.techchallenge.auth.entity.AppUser;
import br.com.techchallenge.auth.repository.AppUserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrapProvisioner {

    @Bean
    ApplicationRunner bootstrapAdminProvisioner(
            ApplicationSecurityProperties properties,
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder
    ) {
        return args -> {
            ApplicationSecurityProperties.BootstrapAdmin bootstrapAdmin = properties.getBootstrapAdmin();
            if (bootstrapAdmin.getUsername() == null || bootstrapAdmin.getUsername().isBlank()) {
                return;
            }
            if (bootstrapAdmin.getPassword() == null || bootstrapAdmin.getPassword().isBlank()) {
                return;
            }
            if (appUserRepository.findByUsername(bootstrapAdmin.getUsername()).isPresent()) {
                return;
            }

            AppUser admin = new AppUser();
            admin.setUsername(bootstrapAdmin.getUsername());
            admin.setPasswordHash(passwordEncoder.encode(bootstrapAdmin.getPassword()));
            admin.setRoles(bootstrapAdmin.getRoles());
            appUserRepository.save(admin);
        };
    }
}
