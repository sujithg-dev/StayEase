package com.stayease.helper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.support.PageableExecutionUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public final class MongoTable
{
    private MongoTable() {}

    public static Criteria search(String q, String... fields)
    {
        if (q == null || q.isBlank()) return null;

        String regex = Pattern.quote(q.trim());

        Criteria[] ors = java.util.Arrays.stream(fields)
                .map(f -> Criteria.where(f).regex(regex, "i"))
                .toArray(Criteria[]::new);

        return new Criteria().orOperator(ors);
    }

    public static Criteria equal(String field, Object value)
    {
        if (value == null || (value instanceof String s && s.isBlank())) return null;

        return Criteria.where(field).is(value);
    }

    public static <E extends Enum<E>> Criteria enumEqual(String field, Class<E> type, String value)
    {
        if (value == null || value.isBlank()) return null;

        try
        {
            return Criteria.where(field).is(Enum.valueOf(type, value));
        }
        catch (IllegalArgumentException ex)
        {
            return null;
        }
    }

    public static Criteria dateFrom(String field, LocalDate from)
    {
        return from == null ? null : Criteria.where(field).gte(from);
    }

    public static Criteria dateTo(String field, LocalDate to)
    {
        return to == null ? null : Criteria.where(field).lte(to);
    }

    public static <T> Page<T> find(MongoTemplate template, Class<T> type, List<Criteria> criteria, Pageable pageable)
    {
        Query query = new Query();
        List<Criteria> active = criteria.stream().filter(Objects::nonNull).toList();

        if (!active.isEmpty())
        {
            query.addCriteria(new Criteria().andOperator(active));
        }

        query.collation(Collation.of("en").strength(2));

        Query countQuery = Query.of(query).limit(-1).skip(-1);
        List<T> content = template.find(query.with(pageable), type);
        return PageableExecutionUtils.getPage(content, pageable, () -> template.count(countQuery, type));
    }
}