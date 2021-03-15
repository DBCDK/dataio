/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.periodicjobs;

import dk.dbc.dataio.bfs.api.BinaryFile;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.solr.SolrSearch;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.common.SolrDocument;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * Class for retrieving record IDs based on a Solr search
 */
public class RecordSearcher implements AutoCloseable {
    private static String UNIQUE_KEY_FIELD = "id";
    private static int FETCH_SIZE = 5000;

    private final CloudSolrClient solrClient;

    /**
     * @param solrZkHost Solr cloud zookeeper connect string
     */
    public RecordSearcher(String solrZkHost) {
        solrClient = new CloudSolrClient.Builder()
                .withZkHost(solrZkHost)
                .build();
        solrClient.connect();
    }

    /**
     * Executes given Solr query on the specified collection
     * and writes the resulting record IDs to the out file
     * @param solrCollection name of Solr Collection
     * @param query Solr query
     * @param out output file
     * @return number of record IDs written to output file
     * @throws HarvesterException on failure to complete this search operation
     */
    public long search(String solrCollection, String query, BinaryFile out) throws HarvesterException {
        try (OutputStream outputStream = out.openOutputStream(true);
             OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
             PrintWriter writer = new PrintWriter(outputStreamWriter)) {
            final SolrSearch.ResultSet resultSet = new SolrSearch(solrClient, solrCollection)
                    .withQuery(query)
                    .withRows(FETCH_SIZE)
                    .withFields(UNIQUE_KEY_FIELD)
                    .withSortClauses(new SolrQuery.SortClause(UNIQUE_KEY_FIELD, SolrQuery.ORDER.asc))
                    .executeForCursorBasedIteration();
            for (SolrDocument doc : resultSet) {
                writer.println(doc.getFirstValue(UNIQUE_KEY_FIELD));
            }
            return resultSet.getSize();
        } catch (IOException e) {
            throw new HarvesterException("Unable to write search result to file", e);
        } catch (SolrServerException e) {
            throw new HarvesterException("Unable to complete search", e);
        }
    }

    /**
     * Executes given Solr query on the specified collection
     * and return the number of found record IDs
     * @param solrCollection name of Solr Collection
     * @param query Solr query
     * @return number of record IDs found
     * @throws HarvesterException on failure to complete this search operation
     */
    public long validate(String solrCollection, String query) throws HarvesterException {
        try {
            final SolrSearch.ResultSet resultSet = new SolrSearch(solrClient, solrCollection)
                    .withQuery(query)
                    .withRows(0) // Don't return any docs - we just want the size of the result
                    .withFields(UNIQUE_KEY_FIELD)
                    .withSortClauses(new SolrQuery.SortClause(UNIQUE_KEY_FIELD, SolrQuery.ORDER.asc))
                    .executeForCursorBasedIteration();

            return resultSet.getSize();
        } catch (SolrServerException e) {
            throw new HarvesterException("Unable to complete search", e);
        }
    }

    @Override
    public void close() {
        if (solrClient != null) {
            try {
                solrClient.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
