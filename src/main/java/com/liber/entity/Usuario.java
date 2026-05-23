package com.liber.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(
    name = "usuarios",
    indexes = {
        @Index(name = "idx_usuarios_email", columnList = "email", unique = true)
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Usuario extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @ToString.Include
    private Long id;

    @NotBlank
    @Email
    @Size(max = 150)
    @Column(nullable = false, unique = true, length = 150)
    @ToString.Include
    private String email;

    @NotBlank
    @Size(max = 150)
    @Column(nullable = false, length = 150)
    private String nome;

    /** Hash BCrypt — nunca o texto puro. */
    @NotBlank
    @Column(nullable = false, length = 100)
    private String senhaHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @ToString.Include
    private Role role;

    @NotNull
    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    /** Marcado a cada troca de senha; tokens emitidos antes disso sao invalidados. */
    @NotNull
    @Column(name = "password_changed_at", nullable = false)
    private Instant passwordChangedAt;

    /** True quando a conta foi criada com senha provisoria — exige troca no 1o acesso. */
    @NotNull
    @Column(name = "deve_trocar_senha", nullable = false)
    @Builder.Default
    private Boolean deveTrocarSenha = false;

    /**
     * Aluno vinculado — preenchido apenas para usuarios com role ALUNO.
     * Usuarios da equipe (ADMIN/BIBLIOTECARIO) tem este campo nulo.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aluno_id", unique = true)
    private Aluno aluno;
}
