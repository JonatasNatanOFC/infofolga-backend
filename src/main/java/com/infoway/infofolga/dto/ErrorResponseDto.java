package com.infoway.infofolga.dto;

/**
 * {@code erro} e {@code message} carregam o mesmo texto: o app lê um ou outro dependendo da tela.
 */
public record ErrorResponseDto(String erro, String message) {
    public ErrorResponseDto(String erro) {
        this(erro, erro);
    }
}
