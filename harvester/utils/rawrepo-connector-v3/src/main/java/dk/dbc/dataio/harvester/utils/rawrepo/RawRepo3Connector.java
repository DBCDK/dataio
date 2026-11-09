package dk.dbc.dataio.harvester.utils.rawrepo;

import dk.dbc.invariant.InvariantUtil;
import dk.dbc.pgqueue.consumer.BasicHarvester;
import dk.dbc.pgqueue.consumer.Settings;
import dk.dbc.rawrepo.dto.RecordIdDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * This class facilitates access to the RawRepo through data source
 * resolved via JNDI lookup of provided resource name
 */
public class RawRepo3Connector {
    private static final Logger LOGGER = LoggerFactory.getLogger(RawRepo3Connector.class);
    private final DataSource dataSource;
    private final BatchFetcher<RecordIdDTO> fetcher;

    public RawRepo3Connector(DataSource dataSource, String consumerId) {
        this.dataSource = dataSource;
        if(Objects.requireNonNull(consumerId).isBlank()) throw new IllegalArgumentException("Consumer ID cannot be empty");
        fetcher = initFetcher(dataSource, consumerId);
    }

    public RawRepo3Connector(String dataSourceResourceName, String consumerId) throws NullPointerException, IllegalArgumentException, IllegalStateException {
        this(lookupDataSource(dataSourceResourceName), consumerId);
    }

    public int dequeue(int limit, Consumer<List<RecordIdDTO>> consumer) throws SQLException {
        return fetcher.batch(limit, consumer);
    }

    public String getRecordServiceUrl() {
        return RawRepoService.RECORD_SERVICE.getUrl(dataSource);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    protected BatchFetcher<RecordIdDTO> initFetcher(DataSource dataSource, String consumerId) {
        BasicHarvester<RecordIdDTO> harvester = new BasicHarvester<>(Settings.defaults(List.of(consumerId), new RRDM3ItemStorage()), dataSource) {
            @Override
            protected Connection getConnection() throws SQLException {
                return dataSource.getConnection();
            }
        };
        return makeFetcher(consumerId, harvester);
    }

    protected BatchFetcher<RecordIdDTO> makeFetcher(String consumerId, BasicHarvester<RecordIdDTO> harvester) {
        return new BatchFetcherImpl<>(consumerId, harvester);
    }

    private static DataSource lookupDataSource(String dataSourceResourceName) {
        InvariantUtil.checkNotNullNotEmptyOrThrow(dataSourceResourceName, "dataSourceResourceName");
        InitialContext initialContext = null;
        try {
            initialContext = new InitialContext();
            Object object = initialContext.lookup(dataSourceResourceName);
            if (object instanceof DataSource ds) return ds;
            throw new IllegalStateException(String.format("Unexpected resource type '%s' returned from lookup", object.getClass().getName()));
        } catch (NamingException e) {
            throw new IllegalStateException(e);
        } finally {
            if (initialContext != null) {
                try {
                    initialContext.close();
                } catch (NamingException e) {
                    LOGGER.warn("Unable to close initial context", e);
                }
            }
        }
    }

    public enum RawRepoService {
        RECORD_SERVICE("RECORD_SERVICE_URL"),
        OPENAGENCY("OPENAGENCY_URL"),
        VIPCORE("VIPCORE_ENDPOINT"),
        SOLR_ZK("SOLR_ZK_HOST");

        private final String key;
        private static final Map<DataSource, Map<RawRepoService, String>> CACHE = new ConcurrentHashMap<>();
        private static final String ERROR_MSG = "Unable to lookup RawRepoService: ";

        RawRepoService(String key) {
            this.key = key;
        }

        /**
         * Returns the value configured for this service in the configurations table of the given data source.
         * Values are cached per data source and service for the lifetime of the JVM.
         *
         * @param dataSource raw-repo data source to look up the value in
         * @return configured value
         * @throws IllegalStateException if the value is missing or the lookup fails
         */
        public String getUrl(DataSource dataSource) {
            return CACHE.computeIfAbsent(dataSource, ds -> new ConcurrentHashMap<>())
                    .computeIfAbsent(this, service -> lookup(key, dataSource));
        }

        private String lookup(String key, DataSource dataSource) {
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement ps = connection.prepareStatement("select value from configurations where key = ?")) {
                ps.setString(1, key);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString(1);
                    }
                }
            } catch (SQLException e) {
                throw new IllegalStateException(ERROR_MSG + key, e);
            }
            throw new IllegalStateException(ERROR_MSG + key);
        }
    }
}
