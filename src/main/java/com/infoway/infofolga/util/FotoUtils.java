package com.infoway.infofolga.util;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * As fotos ficam salvas no banco como data URI (base64). Para não trafegar o base64 em toda
 * listagem, as respostas da API levam apenas uma URL curta que aponta para o endpoint da imagem.
 */
public final class FotoUtils {

    private static final Pattern DATA_URI = Pattern.compile("^data:(image/[a-zA-Z0-9.+-]+);base64,(.+)$", Pattern.DOTALL);
    private static final int TAMANHO_MAXIMO = 3 * 1024 * 1024;

    private FotoUtils() {
    }

    public record Imagem(String contentType, byte[] bytes) {
    }

    /**
     * Converte a foto armazenada em uma URL para o app. A versão (v) muda quando a foto muda,
     * o que permite ao app manter a imagem em cache com segurança.
     */
    public static String url(String fotoArmazenada, String caminho) {
        if (fotoArmazenada == null || fotoArmazenada.isBlank()) {
            return null;
        }
        if (!fotoArmazenada.startsWith("data:")) {
            return fotoArmazenada;
        }
        return caminho + "?v=" + Integer.toHexString(fotoArmazenada.hashCode());
    }

    public static Optional<Imagem> decodificar(String fotoArmazenada) {
        if (fotoArmazenada == null) {
            return Optional.empty();
        }
        Matcher m = DATA_URI.matcher(fotoArmazenada);
        if (!m.matches()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Imagem(m.group(1), Base64.getMimeDecoder().decode(m.group(2))));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /**
     * Resposta HTTP com os bytes da imagem. A URL já carrega a versão, então o cache pode ser longo.
     */
    public static ResponseEntity<byte[]> resposta(Optional<Imagem> imagem) {
        return imagem
                .map(img -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(img.contentType()))
                        .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePrivate())
                        .body(img.bytes()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Só aceita fotos novas enviadas como data URI de imagem. Qualquer outro valor (por exemplo a URL
     * que o próprio app recebeu) significa "manter a foto atual" e devolve null.
     */
    public static String validarNovaFoto(String foto) {
        if (foto == null || !foto.startsWith("data:")) {
            return null;
        }
        if (!DATA_URI.matcher(foto).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de foto inválido.");
        }
        if (foto.length() > TAMANHO_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A foto é muito grande (máximo 2 MB).");
        }
        return foto;
    }
}
