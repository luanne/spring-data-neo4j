/*
 * Copyright (c)  [2011-2015] "Pivotal Software, Inc." / "Neo Technology" / "Graph Aware Ltd."
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product may include a number of subcomponents with
 * separate copyright notices and license terms. Your use of the source
 * code for these subcomponents is subject to the terms and
 * conditions of the subcomponent's license, as noted in the LICENSE file.
 */

package org.springframework.data.neo4j.repository.query;

import java.lang.reflect.Method;

import org.neo4j.ogm.session.Session;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * @author Mark Angrish
 * @author Luanne Misquitta
 */
public class GraphQueryLookupStrategy implements QueryLookupStrategy {

    private final Session session;

    public GraphQueryLookupStrategy(Session session) {
        this.session = session;
    }

    @Override
    public RepositoryQuery resolveQuery(Method method, RepositoryMetadata repositoryMetadata, NamedQueries namedQueries) {
        return new GraphQueryMethod(method, repositoryMetadata, session).createQuery();
    }

}
