package dk.dbc.dataio.harvester.utils.holdingsitems;

import dk.dbc.invariant.InvariantUtil;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.GroupParams;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class provides an API to an holdings-items Solr server.
 * This class is thread safe.
 */
public class HoldingsItemsConnector {
    public final static String AGENCY_ID_FIELD = "holdingsitem.agencyId";
    public final static String BIBLIOGRAPHIC_RECORD_ID_FIELD = "holdingsitem.bibliographicRecordId";

    private final HttpSolrClient client;
    private final String appId;

    public HoldingsItemsConnector(String solrServerEndpoint) throws NullPointerException, IllegalArgumentException {
        client = new HttpSolrClient.Builder(InvariantUtil.checkNotNullNotEmptyOrThrow(solrServerEndpoint, "solrServerEndpoint")).build();
        appId = System.getenv("SOLR_APPID");
    }

    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Tests for holdings for a given bibliographic record ID
     *
     * @param bibliographicRecordId ID of record for which to query
     * @param agencyIds             Set of agency IDs for which to limit the query, if empty all agencies are taken into account
     * @return Set of IDs for agencies with holdings
     * @throws NullPointerException            if given null valued bibliographicRecordId or agencyIds arguments
     * @throws IllegalArgumentException        if given empty valued bibliographicRecordId argument
     * @throws HoldingsItemsConnectorException on failure to communicate with holdings items server
     */
    public Set<Integer> hasHoldings(String bibliographicRecordId, Set<Integer> agencyIds)
            throws NullPointerException, IllegalArgumentException, HoldingsItemsConnectorException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(bibliographicRecordId, "bibliographicRecordId");
        InvariantUtil.checkNotNullOrThrow(agencyIds, "agencyIds");

        final SolrQuery query = getHasHoldingsQuery(bibliographicRecordId, agencyIds);
        return executeQuery(query).getResults().stream()
                .map(doc -> (Integer) doc.getFieldValue(AGENCY_ID_FIELD))
                .filter(agencyId -> agencyId != null)
                .filter(agencyId -> agencyId > 0)
                .collect(Collectors.toSet());
    }

    private SolrQuery getHasHoldingsQuery(String bibliographicRecordId, Set<Integer> agencyIds) {
        final SolrQuery query = new SolrQuery();
        query.setQuery(getHasHoldingsQueryString(bibliographicRecordId, agencyIds));
        query.set(GroupParams.GROUP, true);
        query.set(GroupParams.GROUP_MAIN, true);
        query.set(GroupParams.GROUP_FIELD, AGENCY_ID_FIELD);
        query.setFields(AGENCY_ID_FIELD);
        query.setRows(agencyIds.isEmpty() ? 9999 : agencyIds.size());
        query.setParam("appId", appId);
        return query;
    }

    private String getHasHoldingsQueryString(String bibliographicRecordId, Set<Integer> agencyIds) {
        return String.format("%s:%s%s", BIBLIOGRAPHIC_RECORD_ID_FIELD, bibliographicRecordId, getAgencyIdQueryStringFilter(agencyIds));
    }

    private String getAgencyIdQueryStringFilter(Set<Integer> agencyIds) {
        if (!agencyIds.isEmpty()) {
            return String.format(" AND %s:(%s)", AGENCY_ID_FIELD, agencyIds.stream()
                    .map(Object::toString).collect(Collectors.joining(" OR ")));
        }
        return "";
    }

    private QueryResponse executeQuery(SolrQuery query) throws HoldingsItemsConnectorException {
        try {
            return client.query(query);
        } catch (SolrServerException | IOException e) {
            throw new HoldingsItemsConnectorException(e);
        }
    }
}
