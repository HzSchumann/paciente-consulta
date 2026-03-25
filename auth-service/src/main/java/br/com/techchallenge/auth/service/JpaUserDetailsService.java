package br.com.techchallenge.auth.service;

import br.com.techchallenge.auth.entity.AppUser;
import br.com.techchallenge.auth.repository.AppUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class JpaUserDetailsService implements UserDetailsService {

    private final AppUserRepository appUserRepository;

    public JpaUserDetailsService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));

        return User.withUsername(appUser.getUsername())
                .password(appUser.getPasswordHash())
                .authorities(appUser.roleList().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList())
                .build();
    }
}
