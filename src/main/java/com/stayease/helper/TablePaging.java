package com.stayease.helper;

import org.springframework.data.domain.*;
import java.util.List;
import java.util.Map;

public final class TablePaging
{
    private static final List<Integer> SIZES = List.of(5, 10, 25, 50, 100);
    private TablePaging() {}

    public static Pageable of(int page, int size, String sort, String dir, Map<String, String> sortable, String defaultSort)
    {
        size = SIZES.contains(size) ? size : 10;
        page = Math.max(page, 0);

        String property = (sort == null) ? defaultSort : sortable.getOrDefault(sort, defaultSort);

        Sort.Direction d = "asc".equalsIgnoreCase(dir) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Sort s = Sort.by(d, property);

        if (!"id".equals(property))
        {
            s = s.and(Sort.by(Sort.Direction.ASC, "id")); // stable pagination when values tie
        }

        return PageRequest.of(page, size, s);
    }
}