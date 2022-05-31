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

package dk.dbc.dataio.harvester.utils.rawrepo;

import dk.dbc.dataio.commons.time.StopWatch;
import dk.dbc.invariant.InvariantUtil;
import dk.dbc.rawrepo.queue.ConfigurationException;
import dk.dbc.rawrepo.queue.QueueException;
import dk.dbc.rawrepo.queue.QueueItem;
import dk.dbc.rawrepo.queue.RawRepoQueueDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;

/**
 * This class facilitates access to the RawRepo through data source
 * resolved via JNDI lookup of provided resource name
 */
public class RawRepoConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(RawRepoConnector.class);
    private static final String RECORD_SERVICE_URL_KEY = "RECORD_SERVICE_URL";
    private static final String SOLR_ZK_HOST_KEY = "SOLR_ZK_HOST";

    private DataSource dataSource;

    public RawRepoConnector(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public RawRepoConnector(String dataSourceResourceName)
            throws NullPointerException, IllegalArgumentException, IllegalStateException {
        dataSource = lookupDataSource(dataSourceResourceName);
    }

    public QueueItem dequeue(String consumerId)
            throws NullPointerException, SQLException, QueueException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(consumerId, "consumerId");
        try (Connection connection = dataSource.getConnection()) {
            final StopWatch stopWatch = new StopWatch();
            try {
                return getRawRepoQueueDAO(connection).dequeue(consumerId);
            } finally {
                LOGGER.info("RawRepo dequeue operation took {} milliseconds", stopWatch.getElapsedTime());
            }
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    private RawRepoQueueDAO getRawRepoQueueDAO(Connection connection) throws QueueException {
        return RawRepoQueueDAO.builder(connection)
                .build();
    }

    private DataSource lookupDataSource(String dataSourceResourceName)
            throws NullPointerException, IllegalArgumentException, IllegalStateException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(dataSourceResourceName, "dataSourceResourceName");
        InitialContext initialContext = null;
        try {
            initialContext = new InitialContext();
            final Object object = initialContext.lookup(dataSourceResourceName);
            if (!(object instanceof DataSource)) {
                throw new IllegalStateException(String.format(
                        "Unexpected resource type '%s' returned from lookup", object.getClass().getName()));
            }
            return (DataSource) object;
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

    public String getRecordServiceUrl() throws SQLException, QueueException, ConfigurationException {
        try (Connection connection = dataSource.getConnection()) {
            final RawRepoQueueDAO queueDAO = getRawRepoQueueDAO(connection);
            final HashMap<String, String> configuration = queueDAO.getConfiguration();
            if (!configuration.containsKey(RECORD_SERVICE_URL_KEY)) {
                throw new ConfigurationException("Error getting records-service url - Key " +
                        RECORD_SERVICE_URL_KEY + " was not found in the configuration");
            }
            final String recordServiceUrl = configuration.get(RECORD_SERVICE_URL_KEY);
            LOGGER.info("Using record service URL from database configuration: {}", recordServiceUrl);
            return recordServiceUrl;
        }
    }

    public String getSolrZkHost() throws SQLException, QueueException, ConfigurationException {
        try (Connection connection = dataSource.getConnection()) {
            final RawRepoQueueDAO queueDAO = getRawRepoQueueDAO(connection);
            final HashMap<String, String> configuration = queueDAO.getConfiguration();
            if (!configuration.containsKey(SOLR_ZK_HOST_KEY)) {
                throw new ConfigurationException("Error getting Solr zookeeper host - Key " +
                        SOLR_ZK_HOST_KEY + " was not found in the configuration");
            }
            final String solrZkHost = configuration.get(SOLR_ZK_HOST_KEY);
            LOGGER.info("Using Solr zookeeper host from database configuration: {}", solrZkHost);
            return solrZkHost;
        }
    }
}
