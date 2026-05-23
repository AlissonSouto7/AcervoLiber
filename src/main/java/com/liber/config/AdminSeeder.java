package com.liber.config;

import com.liber.entity.Role;
import com.liber.entity.Usuario;
import com.liber.repository.UsuarioRepository;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements ApplicationRunner {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AdminProperties props;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!props.seedEnabled()) {
            log.debug("AdminSeeder desabilitado (app.admin.seed-enabled=false). Pulando.");
            return;
        }

        if (usuarioRepository.existsByRole(Role.ADMIN)) {
            log.debug("Ja existe um ADMIN cadastrado. Seeder nao executara.");
            return;
        }

        boolean senhaGerada = props.password() == null || props.password().isBlank();
        String senhaTextoPuro = senhaGerada ? gerarSenhaAleatoria() : props.password();

        Usuario admin = Usuario.builder()
            .email(props.email().trim().toLowerCase())
            .nome(props.nome())
            .senhaHash(passwordEncoder.encode(senhaTextoPuro))
            .role(Role.ADMIN)
            .ativo(true)
            .passwordChangedAt(Instant.now())
            .build();

        usuarioRepository.save(admin);

        if (senhaGerada) {
            logSenhaGerada(admin.getEmail(), senhaTextoPuro);
        } else {
            log.info("AdminSeeder: usuario ADMIN criado com email={} (senha fornecida via configuracao).",
                admin.getEmail());
        }
    }

    private static String gerarSenhaAleatoria() {
        byte[] bytes = new byte[15]; // 15 bytes -> 20 chars base64 sem padding
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static void logSenhaGerada(String email, String senha) {
        String separador = "=".repeat(72);
        log.warn("");
        log.warn(separador);
        log.warn("  ADMIN SEEDER: usuario ADMIN criado automaticamente");
        log.warn("  email : {}", email);
        log.warn("  senha : {}", senha);
        log.warn("");
        log.warn("  Esta senha foi gerada porque ADMIN_PASSWORD nao estava definida.");
        log.warn("  ACAO REQUERIDA: faca login e troque a senha imediatamente,");
        log.warn("  ou defina ADMIN_PASSWORD via variavel de ambiente em producao.");
        log.warn(separador);
        log.warn("");
    }
}
