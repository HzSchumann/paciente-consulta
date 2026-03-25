package br.com.techchallenge.auth.service;

import br.com.techchallenge.auth.dto.CreateUserRequest;
import br.com.techchallenge.auth.dto.UserResponse;
import br.com.techchallenge.auth.entity.AppUser;
import br.com.techchallenge.auth.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProvisioningService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProvisioningService(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        if (appUserRepository.findByUsername(request.username()).isPresent()) {
            throw new IllegalArgumentException("Usuário já existe");
        }

        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(request.roles());
        AppUser saved = appUserRepository.save(user);
        return new UserResponse(saved.getId(), saved.getUsername(), saved.getRoles());
    }
}
