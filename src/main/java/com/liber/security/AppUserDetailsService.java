package com.liber.security;

import com.liber.entity.Usuario;
import com.liber.repository.UsuarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Usuario nao encontrado: " + email));

        return new AppUserDetails(
            usuario.getEmail(),
            usuario.getSenhaHash(),
            usuario.getAtivo(),
            List.of(new SimpleGrantedAuthority(usuario.getRole().authority())),
            usuario.getId(),
            usuario.getPasswordChangedAt(),
            Boolean.TRUE.equals(usuario.getDeveTrocarSenha())
        );
    }
}
