package com.liber.security;

import java.time.Instant;
import java.util.Collection;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * Estende {@link User} carregando dados extras do {@code Usuario}:
 * <ul>
 *   <li>{@code passwordChangedAt} — o filtro JWT rejeita tokens emitidos antes
 *       da ultima troca de senha;</li>
 *   <li>{@code deveTrocarSenha} — quando true (senha provisoria), o
 *       {@code SenhaProvisoriaFilter} bloqueia o acesso a qualquer endpoint
 *       fora de {@code /api/v1/auth/**}.</li>
 * </ul>
 */
@Getter
public class AppUserDetails extends User {

    private final Long usuarioId;
    private final Instant passwordChangedAt;
    private final boolean deveTrocarSenha;

    public AppUserDetails(String username,
                          String password,
                          boolean enabled,
                          Collection<? extends GrantedAuthority> authorities,
                          Long usuarioId,
                          Instant passwordChangedAt,
                          boolean deveTrocarSenha) {
        super(username, password, enabled, true, true, true, authorities);
        this.usuarioId = usuarioId;
        this.passwordChangedAt = passwordChangedAt;
        this.deveTrocarSenha = deveTrocarSenha;
    }
}
