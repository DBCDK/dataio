/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobstore.service.dependencytracking;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Simple dependency tracking key generator adding submitter postfix to each token in given list
 * while ensuring that duplicates are removed.
 */
public class DefaultKeyGenerator implements KeyGenerator {
    private final String submitter;

    public DefaultKeyGenerator(long submitterNumber) {
        submitter = String.valueOf(submitterNumber);
    }

    @Override
    public Set<String> getKeys(List<String> tokens) {
        if (tokens != null) {
            return Collections.unmodifiableSet(
                tokens.stream()
                        .map(this::postfixSubmitter)
                        .collect(Collectors.toSet()));
        }
        return Collections.emptySet();
    }

    private String postfixSubmitter(String key) {
        return key + ":" + submitter;
    }
}
