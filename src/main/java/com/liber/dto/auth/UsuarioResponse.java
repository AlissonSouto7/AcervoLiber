package com.liber.dto.auth;

import com.liber.entity.Role;
import com.liber.entity.Usuario;

public record UsuarioResponse(
    Long id,
    String email,
    String nome,
    Role role,
    Boolean ativo,
    Boolean deveTrocarSenha
) {
    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(
            u.getId(), u.getEmail(), u.getNome(), u.getRole(), u.getAtivo(), u.getDeveTrocarSenha());
    }
}
