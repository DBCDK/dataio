package dk.dbc.dataio.accessnew;

import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.Pid;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnector;
import dk.dbc.dataio.harvester.task.connector.HarvesterTaskServiceConnectorException;
import dk.dbc.dataio.harvester.types.HarvestRecordsRequest;
import jakarta.ws.rs.client.ClientBuilder;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.common.SolrDocumentList;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Rerun {
    private static final Logger LOGGER = LoggerFactory.getLogger(Rerun.class);

    public static void main(String[] args) throws SolrServerException, IOException, HarvesterTaskServiceConnectorException {
        Env env = Env.BOBLEBAD;
        try (Http2SolrClient client = new Http2SolrClient.Builder(env.solr).useHttp1_1(true).build()) {
            SolrQuery query = new SolrQuery("rec.collectionIdentifier:710100\\-inaktive").setFields("rec.manifestationId").setRows(500_000);
            SolrDocumentList results = client.query(query).getResults();
            LOGGER.info("Got {} results from Solr", results.getNumFound());
            HarvesterTaskServiceConnector taskServiceConnector = new HarvesterTaskServiceConnector(ClientBuilder.newClient().register(new JacksonFeature()), env.harvester);
            List<AddiMetaData> records = results.stream()
                    .map(d -> d.getFieldValue("rec.manifestationId"))
                    .map(Rerun::toAddiMetaData)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            String harvestTask = taskServiceConnector.createHarvestTask(env.harvesterId, new HarvestRecordsRequest(records));
            LOGGER.info(harvestTask);
        }
    }

    private static AddiMetaData toAddiMetaData(Object o) {
        List<String> list = (List<String>)o;
        if(list == null || list.isEmpty()) return null;
        Pid pid = Pid.of(list.get(0));
        return new AddiMetaData().withSubmitterNumber(pid.getAgencyId()).withBibliographicRecordId(pid.getBibliographicRecordId()).withFormat(pid.getFormat()).withPid(pid.toString());
    }

    public enum Env {
        BOBLEBAD("http://cisterne.solr.dbc.dk:8983/solr/cisterne-corepo-searcher", 14705, "http://dataio-rr-harvester-service.metascrum-staging.svc.cloud.dbc.dk/dataio/harvester/rr"),//http://boblebad.solr.dbc.dk:8983/solr/boblebad-corepo-searcher
        FBSTEST("http://fbstest.solr.dbc.dk:8983/solr/fbstest-corepo-searcher", 19401, "http://dataio-rr-harvester-service.metascrum-staging.svc.cloud.dbc.dk/dataio/harvester/rr"),
        CISTERNE("http://cisterne.solr.dbc.dk:8983/solr/cisterne-corepo-searcher", 7154, "http://dataio-rr-harvester-service.metascrum-prod.svc.cloud.dbc.dk/dataio/harvester/rr");

        public final String solr;
        public final int harvesterId;
        public final String harvester;

        Env(String solr, int harvesterId, String harvester) {
            this.harvester = harvester;
            this.harvesterId = harvesterId;
            this.solr = solr;
        }
    }
}
