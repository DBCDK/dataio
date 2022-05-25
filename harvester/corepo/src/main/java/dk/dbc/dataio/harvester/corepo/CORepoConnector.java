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
