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

package dk.dbc.dataio.harvester.corepo;

import dk.dbc.corepo.access.CORepoDAO;
import dk.dbc.corepo.access.CORepoProvider;
import dk.dbc.dataio.commons.types.Pid;
import dk.dbc.opensearch.commons.repository.IRepositoryIdentifier;
import dk.dbc.opensearch.commons.repository.RepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * This class facilitates access to the CORepo
 */
public class CORepoConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(CORepoConnector.class);

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZone(ZoneOffset.UTC);

    private final CORepoProvider coRepoProvider;

    public CORepoConnector(String repositoryUrl, String harvesterId) throws SQLException, ClassNotFoundException {
       this(new CORepoProvider(String.format("CORepoHarvester.%s", harvesterId), String.format("jdbc:%s", repositoryUrl)));
    }

    CORepoConnector(CORepoProvider provider) {
        coRepoProvider = provider;
    }

    /**
     * Identifies records modified in time interval [from, to[
     * @param from time interval begin
     * @param to time interval end
     * @param acceptedPids predicate for returned PIDs
     * @return list of PIDs for modified records
     * @throws RepositoryException on failure to query modified records
     */
    public List<Pid> getChangesInRepository(Instant from, Instant to, Predicate<Pid> acceptedPids) throws RepositoryException {
        try (CORepoDAO repository = (CORepoDAO) coRepoProvider.getRepository()) {
            final String query = getIntervalQuery(from, to);
            LOGGER.info("finding changes in repository where {}", query);
            return Arrays.stream(repository.searchRepository(query))
                    .map(this::toPid)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .filter(acceptedPids)
                    .collect(Collectors.toList());
        }
    }

    private String getIntervalQuery(Instant from, Instant to) {
        return "modified >= " + DATE_TIME_FORMATTER.format(from) + " AND modified < " + DATE_TIME_FORMATTER.format(to);
    }

    private Optional<Pid> toPid(IRepositoryIdentifier identifier) {
        try {
            return Optional.of(Pid.of(identifier.toString()));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
