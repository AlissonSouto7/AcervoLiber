package com.liber.service;

/**
 * Controle de tentativas de login para bloqueio temporario.
 *
 * <p>O bloqueio e por <b>(e-mail + IP)</b> — nao apenas por e-mail. Isso impede
 * o ataque de "lockout poisoning", em que um atacante de um IP X bloqueia a
 * conta da vitima fazendo 5 tentativas com senha errada, deixando a vitima
 * fora do ar mesmo de outro IP. Com a chave composta, o atacante so bloqueia
 * a propria combinacao (X + email), enquanto a vitima continua logando do
 * seu IP.
 *
 * <p>Abstracao deliberada: a implementacao atual e in-memory ({@link InMemoryLoginAttemptService}).
 * Para rodar em multiplas instancias, basta uma implementacao baseada em Redis — sem tocar
 * no resto do codigo.
 */
public interface LoginAttemptService {

    /** Lanca {@link com.liber.exception.ContaBloqueadaException} se a combinacao (email, ip) estiver bloqueada. */
    void verificarBloqueio(String email, String ip);

    /** Registra uma tentativa de login falha; pode disparar o bloqueio para (email, ip). */
    void registrarFalha(String email, String ip);

    /** Limpa o contador da combinacao (email, ip) apos um login bem-sucedido. */
    void registrarSucesso(String email, String ip);
}
