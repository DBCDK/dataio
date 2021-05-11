/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.holdingsitems;

import com.fasterxml.jackson.databind.type.CollectionType;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnector;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnectorException;
import dk.dbc.solrdocstore.connector.model.HoldingsItems;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is responsible for unmarshalling byte[] representation of JSON array of holdings items
 * into List of {@link HoldingsItems}.
 *
 * This class also tests the existence of already created holdings items against the input in order
 * to delete any deprecated entries in accordance with the "248 datafield" model.
 */
@ApplicationScoped
public class HoldingsItemsUnmarshaller {
    private final JSONBContext jsonbContext = new JSONBContext();
    private final CollectionType holdingsItemsListType = jsonbContext.getTypeFactory()
            .constructCollectionType(List.class, HoldingsItems.class);

    @Inject SolrDocStoreConnector solrDocStoreConnector;

    public List<HoldingsItems> unmarshall(byte[] bytes, String trackingId)
            throws JSONBException, SolrDocStoreConnectorException {
        final List<HoldingsItems> holdingsItemsList = jsonbContext.unmarshall(
                StringUtil.asString(bytes), holdingsItemsListType);

        final List<RecordId> recordIds = getRecordIds(holdingsItemsList);
        holdingsItemsList.addAll(getDeletions(recordIds, trackingId));

        return holdingsItemsList;
    }

    private List<RecordId> getRecordIds(List<HoldingsItems> holdingsItemsList) {
        final List<RecordId> recordIds = new ArrayList<>();
        for (HoldingsItems holdingsItems : holdingsItemsList) {
            recordIds.add(new RecordId(holdingsItems));
        }
        validateRecordIds(recordIds);
        return recordIds;
    }

    private void validateRecordIds(List<RecordId> recordIds) {
        final HashSet<Integer> agencyIds = new HashSet<>(1);
        final HashSet<String> bibliographicRecordIds = new HashSet<>(1);
        int nextPostfix = 0;
        for (RecordId recordId : recordIds) {
            agencyIds.add(recordId.getAgencyId());
            bibliographicRecordIds.add(recordId.getBibliographicRecordId());
            final Integer postfix = recordId.getPostfix();
            if (postfix != null && postfix != ++nextPostfix) {
                throw new IllegalArgumentException("Out of order postfix " + postfix + " when " + nextPostfix + " was expected");
            }
        }
        if (agencyIds.size() > 1) {
            throw new IllegalArgumentException("Multiple agency IDs given: " + agencyIds);
        }
        if (bibliographicRecordIds.size() > 1) {
            throw new IllegalArgumentException("Multiple bibliographic record IDs given: " + bibliographicRecordIds);
        }
        if (nextPostfix > 0 && recordIds.size() != nextPostfix) {
            throw new IllegalArgumentException("Mixing postfix IDs with non-postfix IDs");
        }
    }

    private List<HoldingsItems> getDeletions(List<RecordId> recordIds, String trackingId) throws SolrDocStoreConnectorException {
        final List<HoldingsItems> deletions = new ArrayList<>();
        if (!recordIds.isEmpty()) {
            // Start at postfix __1 since record might previously have had 248 fields
            int testPostfix = 1;
            final RecordId firstRecordId = recordIds.get(0);
            if (firstRecordId.getPostfix() != null) {
                // Test for existence of non-postfix holding since record might
                // previously not have had any 248 fields.
                if (callHoldingExists(firstRecordId.getAgencyId(), firstRecordId.getBibliographicRecordId())) {
                    deletions.add(createDeletion(
                            firstRecordId.getAgencyId(), firstRecordId.getBibliographicRecordId(), trackingId));
                }
                // Instead of starting at postfix __1 start at the largest value
                // not seen in the input. This ensures that 248 fields no longer present
                // will be deleted.
                final RecordId lastRecordid = recordIds.get(recordIds.size() - 1);
                testPostfix = lastRecordid.getPostfix() + 1;
            }

            while (true) {
                // Keep testing increasing postfixes and create delete records for each existing holding.
                // Terminate loop on first non-existing holding.
                final String bibliographicRecordId = String.format("%s__%d", firstRecordId.getBibliographicRecordId(), testPostfix);
                if (!callHoldingExists(firstRecordId.getAgencyId(), bibliographicRecordId)) {
                    break;
                }
                deletions.add(createDeletion(firstRecordId.getAgencyId(), bibliographicRecordId, trackingId));
                testPostfix++;
            }
        }
        return deletions;
    }

    private boolean callHoldingExists(int agencyId, String bibliographicRecordId) throws SolrDocStoreConnectorException {
        return solrDocStoreConnector.holdingExists(agencyId, bibliographicRecordId);
    }

    private HoldingsItems createDeletion(int agencyId, String bibliographicId, String trackingId) {
        // A HoldingsItems object with an empty indexKeys list is
        // effectively a delete record.
        final HoldingsItems holdingsItems = new HoldingsItems();
        holdingsItems.setAgencyId(agencyId);
        holdingsItems.setBibliographicRecordId(bibliographicId);
        holdingsItems.setTrackingId(trackingId);
        holdingsItems.setIndexKeys(Collections.emptyList());
        return holdingsItems;
    }

    /* In the '248' model a bibliographic record ID can consist of both a string value and
       a positive numeric postfix separated by double underscores '__'
     */
    private static class RecordId {
        private final static Pattern POSTFIX_PATTERN = Pattern.compile("(.*?)__(\\d+)");

        private final int agencyId;
        private String bibliographicRecordId;
        private Integer postfix = null;

        RecordId(HoldingsItems holdingsItems) {
            agencyId = holdingsItems.getAgencyId();
            bibliographicRecordId = holdingsItems.getBibliographicRecordId();
            if (bibliographicRecordId == null || bibliographicRecordId.trim().isEmpty()) {
                throw new IllegalArgumentException("bibliographicRecordId can not be null or empty");
            }
            final Matcher matcher = POSTFIX_PATTERN.matcher(holdingsItems.getBibliographicRecordId());
            if (matcher.find()) {
                bibliographicRecordId = matcher.group(1);
                postfix = Integer.parseInt(matcher.group(2));
                if (postfix <= 0) {
                    throw new IllegalArgumentException("__POSTFIX must be larger than or equal to 1, was " + postfix);
                }
            }
        }

        public int getAgencyId() {
            return agencyId;
        }

        public String getBibliographicRecordId() {
            return bibliographicRecordId;
        }

        public Integer getPostfix() {
            return postfix;
        }

        @Override
        public String toString() {
            return "RecordId{" +
                    "agencyId=" + agencyId +
                    ", bibliographicId='" + bibliographicRecordId + '\'' +
                    ", postfix=" + postfix +
                    '}';
        }
    }
}
