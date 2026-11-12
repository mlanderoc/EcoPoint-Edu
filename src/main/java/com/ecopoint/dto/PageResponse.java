package com.ecopoint.dto;

import java.util.List;
import java.util.function.Function;

/**
 * One page of a long list (spec 003, R-103 and decision T10).
 *
 * @param items      elements of the page
 * @param page       page number, starting at 0
 * @param size       maximum number of elements per page
 * @param totalItems number of elements of the whole list
 * @param <T>        element type
 * @author craulgomez
 * @version 1.0
 */
public record PageResponse<T>(List<T> items, int page, int size, long totalItems) {

    /** Page size used when the request does not send one. */
    public static final int DEFAULT_SIZE = 50;
    /** Largest page size accepted. */
    public static final int MAX_SIZE = 200;

    /**
     * Creates the page with an immutable copy of its items.
     */
    public PageResponse {
        items = List.copyOf(items);
    }

    /**
     * Cuts one page out of a complete list and converts its elements.
     *
     * @param all       complete list
     * @param page      requested page, starting at 0; negative values become 0
     * @param size      requested size; out-of-range values become the default or the maximum
     * @param converter conversion of each element to its DTO
     * @param <S>       source element type
     * @param <T>       DTO type
     * @return the requested page
     */
    public static <S, T> PageResponse<T> of(List<S> all, int page, int size, Function<S, T> converter) {
        int safeSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        int safePage = Math.max(page, 0);
        int from = (int) Math.min((long) safePage * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());
        return new PageResponse<>(all.subList(from, to).stream().map(converter).toList(), safePage, safeSize, all.size());
    }
}
