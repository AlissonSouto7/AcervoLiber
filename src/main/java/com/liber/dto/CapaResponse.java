package com.liber.dto;

/**
 * Resposta do preview de capa: a URL resolvida a partir do ISBN, ou null se a
 * Google Books nao tiver capa para aquele ISBN.
 */
public record CapaResponse(String capaUrl) {}
