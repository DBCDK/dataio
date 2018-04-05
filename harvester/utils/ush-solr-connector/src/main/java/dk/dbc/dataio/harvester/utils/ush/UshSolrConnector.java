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

package dk.dbc.dataio.harvester.utils.ush;

import dk.dbc.invariant.InvariantUtil;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.CursorMarkParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This class provides an API to an USH Solr server.
 * This class is thread safe.
 */
public class UshSolrConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(UshSolrConnector.class);

    private static final int DEFAULT_DOCUMENT_BUFFER_MAX_SIZE = 500;

    private final SolrClient client;
    private final int documentBufferMaxSize;
    private final DateTimeFormatter utcDateTimeFormatter = DateTimeFormatter.ISO_INSTANT;

    public UshSolrConnector(String solrServerEndpoint) throws NullPointerException, IllegalArgumentException {
        this(solrServerEndpoint, DEFAULT_DOCUMENT_BUFFER_MAX_SIZE);
    }

    public UshSolrConnector(String solrServerEndpoint, int documentBufferMaxSize) throws NullPointerException, IllegalArgumentException {
        this(new HttpSolrClient.Builder(
                InvariantUtil.checkNotNullNotEmptyOrThrow(
                        solrServerEndpoint, "solrServerEndpoint")).build(),
                documentBufferMaxSize);
    }

    private UshSolrConnector(SolrClient solrClient, int documentBufferMaxSize) throws NullPointerException, IllegalArgumentException {
        this.client = InvariantUtil.checkNotNullOrThrow(solrClient, "solrClient");
        if (documentBufferMaxSize <= 0) {
            throw new IllegalArgumentException("Value of parameter 'documentBufferMaxSize' must be larger than 0");
        }
        this.documentBufferMaxSize = documentBufferMaxSize;
    }

    public void close() {
        try {
            client.close();
        } catch(IOException e){
            throw new IllegalStateException(e);
        }
    }

    /**
     * Finds all documents in specified database harvested in interval given by from and until timestamps
     * @param database name of database for which to retrieve documents
     * @param from all documents harvested after this timestamp (non-inclusive), can be null for open-ended
     * @param until all documents harvested before this timestamp (inclusive), can be null for open-ended
     * @return iterable result set
     * @throws NullPointerException if given null-valued database
     * @throws IllegalArgumentException if given empty-valued database
     * @throws UshSolrConnectorException on error during Solr server communication
     */
    public ResultSet findDatabaseDocumentsHarvestedInInterval(String database, Date from, Date until)
            throws NullPointerException, IllegalArgumentException, UshSolrConnectorException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(database, "database");
        final SolrQuery query = new SolrQuery()
            .setQuery(String.format("database:%s AND harvest-timestamp:{%s TO %s]",
                    database, asQueryDateTime(from), asQueryDateTime(until)));
        // mandatory when using cursorMark:
        query.addSort("id", SolrQuery.ORDER.asc);
        LOGGER.info("query: {}", query.toString());
        return new ResultSet(query, documentBufferMaxSize);
    }

    private String asQueryDateTime(Date date) {
        if (date == null) {
            return "*";
        }
        return utcDateTimeFormatter.format(date.toInstant());
    }

    /**
     * This class represents a one-time iteration of Solr search result set
     */
    public class ResultSet implements Iterable<UshSolrDocument> {
        private final SolrQuery query;
        private final int documentBufferMaxSize;
        private final int size;
        private String cursorMark, nextCursorMark;
        private Iterator<UshSolrDocument> documentBuffer;

        public ResultSet(SolrQuery query, int documentBufferMaxSize) throws UshSolrConnectorException {
            this.query = query;
            this.documentBufferMaxSize = documentBufferMaxSize;
            this.size = getResultSetSize();
            this.cursorMark = CursorMarkParams.CURSOR_MARK_START;
            this.nextCursorMark = "";
            this.documentBuffer = fillDocumentBuffer();
        }

        public int getSize() {
            return size;
        }

        @Override
        public Iterator<UshSolrDocument> iterator() {
            return new Iterator<UshSolrDocument>() {
                @Override
                public boolean hasNext() throws UshSolrConnectorException {
                    if (!documentBuffer.hasNext() && !cursorMark.equals(
                            nextCursorMark)) {
                        cursorMark = nextCursorMark;
                        documentBuffer = fillDocumentBuffer();
                    }
                    return documentBuffer.hasNext();
                }

                @Override
                public UshSolrDocument next() {
                    try {
                        return documentBuffer.next();
                    } catch (NoSuchElementException e) {
                        return null;
                    }
                }
            };
        }

        private int getResultSetSize() throws UshSolrConnectorException {
            query.setRows(0);
            return (int) getQueryResponse(query).getResults().getNumFound();
        }

        private Iterator<UshSolrDocument> fillDocumentBuffer() throws UshSolrConnectorException {
            query.set(CursorMarkParams.CURSOR_MARK_PARAM, cursorMark);
            query.setRows(documentBufferMaxSize);
            QueryResponse response = getQueryResponse(query);
            nextCursorMark = response.getNextCursorMark();
            return response.getBeans(UshSolrDocument.class).iterator();
        }

        private QueryResponse getQueryResponse(SolrQuery query) throws UshSolrConnectorException {
            try {
                return client.query(query);
            } catch (SolrServerException|IOException e) {
                throw new UshSolrConnectorException(e);
            }
        }
    }
}
