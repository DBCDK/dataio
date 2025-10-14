package dk.dbc.dataio.harvester.rr_dm3;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.common.SolrDocumentList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class SolrVersionTestIT {
    @Test
    @Disabled("Test to verify that the included solr4j version is working (run manually)")
    public void testVersion() throws SolrServerException, IOException {
        testServer(new Http2SolrClient.Builder("http://fbstest.solr.dbc.dk:8983/solr/fbstest-ims-searcher").useHttp1_1(true).build());
        testServer(new Http2SolrClient.Builder("http://cisterne.solr.dbc.dk:8983/solr/cisterne-ims-searcher").useHttp1_1(true).build());
    }

    private void testServer(SolrClient client) throws SolrServerException, IOException {
        String field = "holdingsitem.bibliographicRecordId";
        SolrQuery query = new SolrQuery(field + ":*").setFields(field).setRows(1);
        SolrDocumentList results = client.query(query).getResults();
        Assertions.assertTrue(results.getNumFound() > 0);
        Assertions.assertNotNull(results.get(0).getFieldValue(field));
        client.close();
    }
}
