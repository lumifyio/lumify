package io.lumify.core.util;

import com.google.common.collect.ImmutableList;
import org.securegraph.Edge;
import org.securegraph.FetchHint;
import org.securegraph.Vertex;
import org.securegraph.query.Predicate;
import org.securegraph.query.Query;
import org.securegraph.util.IterableUtils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;

public class CompositeGraphQuery implements Query {
    private final List<Query> queries;

    public CompositeGraphQuery(List<Query> queries) {
        this.queries = ImmutableList.copyOf(queries);
    }

    @Override
    public Iterable<Vertex> vertices() {
        Collection<Vertex> all = new LinkedHashSet<>();
        for (Query query : queries) {
            all.addAll(IterableUtils.toList(query.vertices()));
        }
        return all;
    }

    @Override
    public Iterable<Vertex> vertices(EnumSet<FetchHint> fetchHints) {
        Collection<Vertex> all = new LinkedHashSet<>();
        for (Query query : queries) {
            all.addAll(IterableUtils.toList(query.vertices(fetchHints)));
        }
        return all;
    }

    @Override
    public Iterable<Edge> edges() {
        Collection<Edge> all = new LinkedHashSet<>();
        for (Query query : queries) {
            all.addAll(IterableUtils.toList(query.edges()));
        }
        return all;
    }

    @Override
    public Iterable<Edge> edges(EnumSet<FetchHint> fetchHints) {
        Collection<Edge> all = new LinkedHashSet<>();
        for (Query query : queries) {
            all.addAll(IterableUtils.toList(query.edges(fetchHints)));
        }
        return all;
    }

    @Override
    public Iterable<Edge> edges(String label) {
        Collection<Edge> all = new LinkedHashSet<>();
        for (Query query : queries) {
            all.addAll(IterableUtils.toList(query.edges(label)));
        }
        return all;
    }

    @Override
    public Iterable<Edge> edges(String label, EnumSet<FetchHint> fetchHints) {
        Collection<Edge> all = new LinkedHashSet<>();
        for (Query query : queries) {
            all.addAll(IterableUtils.toList(query.edges(label, fetchHints)));
        }
        return all;
    }

    @Override
    public <T> Query range(String propertyName, T startValue, T endValue) {
        for (Query query : queries) {
            query.range(propertyName, startValue, endValue);
        }
        return this;
    }

    @Override
    public <T> Query has(String propertyName, T value) {
        for (Query query : queries) {
            query.has(propertyName, value);
        }
        return this;
    }

    @Override
    public <T> Query has(String propertyName, Predicate predicate, T value) {
        for (Query query : queries) {
            query.has(propertyName, predicate, value);
        }
        return this;
    }

    @Override
    public Query skip(int count) {
        for (Query query : queries) {
            query.skip(count);
        }
        return this;
    }

    @Override
    public Query limit(int count) {
        for (Query query : queries) {
            query.limit(count);
        }
        return this;
    }
}
