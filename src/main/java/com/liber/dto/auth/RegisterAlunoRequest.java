package com.liber.dto.auth;

import com.liber.validation.SenhaForte;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Auto-cadastro de aluno na tela publica de login.
 *
 * <p>O aluno DEVE ter sido pre-cadastrado pelo bibliotecario (com cpf+nome+turma)
 * antes. O service valida:
 * <ul>
 *   <li>CPF existe no cadastro</li>
 *   <li>nome bate (normalizado: case-insensitive, sem acentos)</li>
 *   <li>aluno ainda nao tem Usuario vinculado</li>
 * </ul>
 *
 * <p>Sem essas tres garantias, o auto-cadastro vira vetor de sequestro de conta —
 * qualquer um que saiba o CPF (que circula em listas escolares) poderia
 * cadastrar antes do aluno legitimo.
 */
public record RegisterAlunoRequest(

    @NotBlank(message = "CPF obrigatorio.")
    @Pattern(regexp = "^[0-9.\\-]+$", message = "CPF deve conter apenas digitos, pontos e hifen")
    String cpf,

    @NotBlank(message = "Nome obrigatorio.")
    @Size(max = 150)
    // Mesma regex anti-XSS dos outros DTOs de nome (Fase 7.X.alta.1).
    @Pattern(
        regexp = "^[\\p{L}\\p{N} .,'\\-]+$",
        message = "Nome contem caracteres invalidos."
    )
    String nome,

    @NotBlank
    @SenhaForte
    String senha
) {}
