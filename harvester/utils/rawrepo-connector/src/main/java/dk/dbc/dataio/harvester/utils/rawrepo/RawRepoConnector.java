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
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.marcxmerge.MarcXMerger;
import dk.dbc.marcxmerge.MarcXMergerException;
import dk.dbc.rawrepo.QueueJob;
import dk.dbc.rawrepo.RawRepoDAO;
import dk.dbc.rawrepo.RawRepoException;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;
import dk.dbc.rawrepo.RelationHints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class facilitates access to the RawRepo through data source
 * resolved via JNDI lookup of provided resource name
 */
public class RawRepoConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(RawRepoConnector.class);

    private DataSource dataSource;
    private final RelationHints relationHints;

    // This class is NOT thread safe.
    private final MarcXMerger marcXMerger;

    public RawRepoConnector(DataSource dataSource, RelationHints relationHints)
            throws NullPointerException, IllegalArgumentException,
            IllegalStateException {
        this(relationHints);
        InvariantUtil.checkNotNullOrThrow(dataSource, "dataSource");
        this.dataSource = dataSource;
    }

    public RawRepoConnector(String dataSourceResourceName, RelationHints relationHints)
            throws NullPointerException, IllegalArgumentException, IllegalStateException {
        this(relationHints);
        dataSource = lookupDataSource(dataSourceResourceName);
    }

    // this constructor is private to enable sharing its code with the other
    // constructors without exposing a constructor which doesn't take a datasource.
    private RawRepoConnector(RelationHints relationHints) throws NullPointerException,
            IllegalArgumentException, IllegalStateException {
        this.relationHints = InvariantUtil.checkNotNullOrThrow(relationHints, "relationHints");
        try {
            this.marcXMerger = new MarcXMerger();
        } catch (MarcXMergerException e) {
            throw new IllegalStateException(e);
        }
    }

    public Record fetchRecord(RecordId id) throws NullPointerException, SQLException, RawRepoException {
        InvariantUtil.checkNotNullOrThrow(id, "id");
        try (final Connection connection = dataSource.getConnection()) {
            final StopWatch stopWatch = new StopWatch();
            try {
                return getRawRepoDAO(connection).fetchRecord(id.getBibliographicRecordId(), id.getAgencyId());
            } finally {
                LOGGER.info("RawRepo fetchRecord operation took {} milliseconds", stopWatch.getElapsedTime());
            }
        }
    }

    public Map<String, Record> fetchRecordCollection(RecordId id)
            throws NullPointerException, SQLException, RawRepoException, MarcXMergerException {
        InvariantUtil.checkNotNullOrThrow(id, "id");
        try (final Connection connection = dataSource.getConnection()) {
            final StopWatch stopWatch = new StopWatch();
            try {
                return getStringRecordMap(id, getRawRepoDAO(connection));
            } finally {
                LOGGER.info("RawRepo fetchRecordCollection operation took {} milliseconds", stopWatch.getElapsedTime());
            }
        }
    }

    Map<String, Record> getStringRecordMap(RecordId id, RawRepoDAO rawRepoDAO) throws RawRepoException, MarcXMergerException {
        final String bibliographicRecordId = id.getBibliographicRecordId();
        final int agencyId = id.getAgencyId();

        // We handle delete records as suggested by the RawRepoDAO author (MB).
        if (rawRepoDAO.recordExistsMaybeDeleted(bibliographicRecordId, agencyId)
                && !rawRepoDAO.recordExists(bibliographicRecordId, agencyId)) {
            final Map<String, Record> deletedRecordMap = new HashMap<>(1);
            deletedRecordMap.put(bibliographicRecordId,
                    rawRepoDAO.fetchMergedRecord(bibliographicRecordId, agencyId, marcXMerger, true));
            return deletedRecordMap;
        } else {
            return rawRepoDAO.fetchRecordCollectionExpanded(bibliographicRecordId, agencyId, marcXMerger);
        }
    }

    public QueueJob dequeue(String consumerId)
            throws NullPointerException, SQLException, RawRepoException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(consumerId, "consumerId");
        try (final Connection connection = dataSource.getConnection()) {
            final StopWatch stopWatch = new StopWatch();
            try {
                return getRawRepoDAO(connection).dequeue(consumerId);
            } finally {
                LOGGER.info("RawRepo dequeue operation took {} milliseconds", stopWatch.getElapsedTime());
            }
        }
    }

    public void queueFail(QueueJob queueJob, String errorMessage)
            throws NullPointerException, SQLException, RawRepoException {
        InvariantUtil.checkNotNullOrThrow(queueJob, "queueJob");
        InvariantUtil.checkNotNullNotEmptyOrThrow(errorMessage, "errorMessage");
        try (final Connection connection = dataSource.getConnection()) {
            final StopWatch stopWatch = new StopWatch();
            try {
                getRawRepoDAO(connection).queueFail(queueJob, errorMessage);
            } finally {
                LOGGER.info("RawRepo queueFail operation took {} milliseconds", stopWatch.getElapsedTime());
            }
        }
    }

    public boolean recordExists(String bibliographicRecordId, int agencyId) throws SQLException, RawRepoException {
        try (final Connection connection = dataSource.getConnection()) {
            final StopWatch stopWatch = new StopWatch();
            try {
                return getRawRepoDAO(connection).recordExistsMaybeDeleted(bibliographicRecordId, agencyId);
            } finally {
                LOGGER.info("RawRepo recordExistsMabyDeleted operation took {} milliseconds", stopWatch.getElapsedTime());
            }
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public RelationHints getRelationHints() {
        return relationHints;
    }

    private RawRepoDAO getRawRepoDAO(Connection connection) throws RawRepoException {
        return RawRepoDAO.builder(connection)
                .relationHints(relationHints)
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
}
