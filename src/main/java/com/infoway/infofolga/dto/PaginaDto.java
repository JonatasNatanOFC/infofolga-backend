package com.infoway.infofolga.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public record PaginaDto<T>(List<T> conteudo, int pagina, int tamanho, long totalElementos, boolean ultima) {

    public static <E, T> PaginaDto<T> de(Page<E> page, Function<E, T> mapper) {
        return new PaginaDto<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.isLast());
    }
}
