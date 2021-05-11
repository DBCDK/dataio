/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.holdingsitems;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnector;
import dk.dbc.solrdocstore.connector.SolrDocStoreConnectorException;
import dk.dbc.solrdocstore.connector.model.HoldingsItems;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HoldingsItemsUnmarshallerTest {
    private final SolrDocStoreConnector solrDocStoreConnector = mock(SolrDocStoreConnector.class);
    private final JSONBContext jsonbContext = new JSONBContext();
    private final HoldingsItemsUnmarshaller holdingsItemsUnmarshaller = newHoldingsItemsUnmarshaller();

    @Test
    void failOnNullValuedBibliographicRecordId() throws JSONBException, SolrDocStoreConnectorException {
        final HoldingsItems holdingsItems = new HoldingsItems();
        holdingsItems.setAgencyId(123456);

        final String json = jsonbContext.marshall(Collections.singletonList(holdingsItems));
        try {
            holdingsItemsUnmarshaller.unmarshall(json.getBytes(StandardCharsets.UTF_8), "test");
            fail("No IllegalArgumentException thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("bibliographicRecordId can not be null or empty"));
        }
    }

    @Test
    void failOnEmptyBibliographicRecordId() throws JSONBException, SolrDocStoreConnectorException {
        final HoldingsItems holdingsItems = new HoldingsItems();
        holdingsItems.setAgencyId(123456);
        holdingsItems.setBibliographicRecordId(" ");

        final String json = jsonbContext.marshall(Collections.singletonList(holdingsItems));
        try {
            holdingsItemsUnmarshaller.unmarshall(json.getBytes(StandardCharsets.UTF_8), "test");
            fail("No IllegalArgumentException thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("bibliographicRecordId can not be null or empty"));
        }
    }

    @Test
    void failOnNonPositivePostfix() throws JSONBException, SolrDocStoreConnectorException {
        final HoldingsItems holdingsItems = new HoldingsItems();
        holdingsItems.setAgencyId(123456);
        holdingsItems.setBibliographicRecordId("id__0");

        final String json = jsonbContext.marshall(Collections.singletonList(holdingsItems));
        try {
            holdingsItemsUnmarshaller.unmarshall(json.getBytes(StandardCharsets.UTF_8), "test");
            fail("No IllegalArgumentException thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), is("__POSTFIX must be larger than or equal to 1, was 0"));
        }
    }

    @Test
    void failOnMultipleAgencyIds() throws JSONBException, SolrDocStoreConnectorException {
        final HoldingsItems holdingsItems1 = new HoldingsItems();
        holdingsItems1.setAgencyId(123456);
        holdingsItems1.setBibliographicRecordId("id__1");
        final HoldingsItems holdingsItems2 = new HoldingsItems();
        holdingsItems2.setAgencyId(654321);
        holdingsItems2.setBibliographicRecordId("id__2");

        final String json = jsonbContext.marshall(Arrays.asList(holdingsItems1, holdingsItems2));
        try {
            holdingsItemsUnmarshaller.unmarshall(json.getBytes(StandardCharsets.UTF_8), "test");
            fail("No IllegalArgumentException thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), startsWith("Multiple agency IDs given"));
        }
    }

    @Test
    void failOnMultipleBibliographicRecordIds() throws JSONBException, SolrDocStoreConnectorException {
        final HoldingsItems holdingsItems1 = new HoldingsItems();
        holdingsItems1.setAgencyId(123456);
        holdingsItems1.setBibliographicRecordId("ida__1");
        final HoldingsItems holdingsItems2 = new HoldingsItems();
        holdingsItems2.setAgencyId(123456);
        holdingsItems2.setBibliographicRecordId("idb__2");

        final String json = jsonbContext.marshall(Arrays.asList(holdingsItems1, holdingsItems2));
        try {
            holdingsItemsUnmarshaller.unmarshall(json.getBytes(StandardCharsets.UTF_8), "test");
            fail("No IllegalArgumentException thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), startsWith("Multiple bibliographic record IDs given"));
        }
    }

    @Test
    void failOnOutOfOrderPostfixes() throws JSONBException, SolrDocStoreConnectorException {
        final HoldingsItems holdingsItems1 = new HoldingsItems();
        holdingsItems1.setAgencyId(123456);
        holdingsItems1.setBibliographicRecordId("id__1");
        final HoldingsItems holdingsItems2 = new HoldingsItems();
        holdingsItems2.setAgencyId(123456);
        holdingsItems2.setBibliographicRecordId("id__2");

        final String json = jsonbContext.marshall(Arrays.asList(holdingsItems2, holdingsItems1));
        try {
            holdingsItemsUnmarshaller.unmarshall(json.getBytes(StandardCharsets.UTF_8), "test");
            fail("No IllegalArgumentException thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), startsWith("Out of order postfix 2 when 1 was expected"));
        }
    }

    @Test
    void failOnPostfixGaps() throws JSONBException, SolrDocStoreConnectorException {
        final HoldingsItems holdingsItems1 = new HoldingsItems();
        holdingsItems1.setAgencyId(123456);
        holdingsItems1.setBibliographicRecordId("id__1");
        final HoldingsItems holdingsItems2 = new HoldingsItems();
        holdingsItems2.setAgencyId(123456);
        holdingsItems2.setBibliographicRecordId("id__3");

        final String json = jsonbContext.marshall(Arrays.asList(holdingsItems1, holdingsItems2));
        try {
            holdingsItemsUnmarshaller.unmarshall(json.getBytes(StandardCharsets.UTF_8), "test");
            fail("No IllegalArgumentException thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), startsWith("Out of order postfix 3 when 2 was expected"));
        }
    }

    @Test
    void failOnMixOfPostfixAndNonPostfixIds() throws JSONBException, SolrDocStoreConnectorException {
        final HoldingsItems holdingsItems1 = new HoldingsItems();
        holdingsItems1.setAgencyId(123456);
        holdingsItems1.setBibliographicRecordId("id");
        final HoldingsItems holdingsItems2 = new HoldingsItems();
        holdingsItems2.setAgencyId(123456);
        holdingsItems2.setBibliographicRecordId("id__1");

        final String json = jsonbContext.marshall(Arrays.asList(holdingsItems1, holdingsItems2));
        try {
            holdingsItemsUnmarshaller.unmarshall(json.getBytes(StandardCharsets.UTF_8), "test");
            fail("No IllegalArgumentException thrown");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage(), startsWith("Mixing postfix IDs with non-postfix IDs"));
        }
    }

    @Test
    void nonPostfixId() throws JSONBException, SolrDocStoreConnectorException {
        final HoldingsItems holdingsItems = new HoldingsItems();
        holdingsItems.setAgencyId(123456);
        holdingsItems.setBibliographicRecordId("nonPostfixId");
        final List<HoldingsItems> expectedList = Collections.singletonList(holdingsItems);

        when(solrDocStoreConnector.holdingExists(
                holdingsItems.getAgencyId(), withPostfix(holdingsItems.getBibliographicRecordId(), 1)))
                .thenReturn(false);

        final String json = jsonbContext.marshall(expectedList);
        final List<HoldingsItems> holdingsItemsList = holdingsItemsUnmarshaller.unmarshall(
                json.getBytes(StandardCharsets.UTF_8), "test");
        assertThat(holdingsItemsList, is(expectedList));

        verify(solrDocStoreConnector).holdingExists(holdingsItems.getAgencyId(),
                withPostfix(holdingsItems.getBibliographicRecordId(), 1));
        verify(solrDocStoreConnector, times(0)).holdingExists(holdingsItems.getAgencyId(),
                withPostfix(holdingsItems.getBibliographicRecordId(), 2));
    }

    @Test
    void nonPostfixIdWithPreviousWith248Fields() throws JSONBException, SolrDocStoreConnectorException {
        final HoldingsItems holdingsItems = new HoldingsItems();
        holdingsItems.setAgencyId(123456);
        holdingsItems.setBibliographicRecordId("nonPostfixIdWithPreviousWith248Fields");
        final List<HoldingsItems> expectedList = Arrays.asList(holdingsItems,
                createDeletion(holdingsItems.getAgencyId(), withPostfix(holdingsItems.getBibliographicRecordId(), 1), "test"),
                createDeletion(holdingsItems.getAgencyId(), withPostfix(holdingsItems.getBibliographicRecordId(), 2), "test"));

        when(solrDocStoreConnector.holdingExists(
                holdingsItems.getAgencyId(), withPostfix(holdingsItems.getBibliographicRecordId(), 1)))
                .thenReturn(true);
        when(solrDocStoreConnector.holdingExists(
                holdingsItems.getAgencyId(), withPostfix(holdingsItems.getBibliographicRecordId(), 2)))
                .thenReturn(true);
        when(solrDocStoreConnector.holdingExists(
                holdingsItems.getAgencyId(), withPostfix(holdingsItems.getBibliographicRecordId(), 3)))
                .thenReturn(false);

        final String json = jsonbContext.marshall(Collections.singletonList(holdingsItems));
        final List<HoldingsItems> holdingsItemsList = holdingsItemsUnmarshaller.unmarshall(
                json.getBytes(StandardCharsets.UTF_8), "test");
        assertThat(holdingsItemsList, is(expectedList));
    }

    @Test
    void postfixId() throws JSONBException, SolrDocStoreConnectorException {
        final HoldingsItems holdingsItems1 = new HoldingsItems();
        holdingsItems1.setAgencyId(123456);
        holdingsItems1.setBibliographicRecordId("postfixId__1");
        final HoldingsItems holdingsItems2 = new HoldingsItems();
        holdingsItems2.setAgencyId(123456);
        holdingsItems2.setBibliographicRecordId("postfixId__2");

        final List<HoldingsItems> expectedList = Arrays.asList(holdingsItems1, holdingsItems2);

        when(solrDocStoreConnector.holdingExists(
                holdingsItems1.getAgencyId(), "postfixId"))
                .thenReturn(false);
        when(solrDocStoreConnector.holdingExists(
                holdingsItems1.getAgencyId(), withPostfix("postfixId", 3)))
                .thenReturn(false);

        final String json = jsonbContext.marshall(expectedList);
        final List<HoldingsItems> holdingsItemsList = holdingsItemsUnmarshaller.unmarshall(
                json.getBytes(StandardCharsets.UTF_8), "test");
        assertThat(holdingsItemsList, is(expectedList));
    }

    @Test
    void postfixIdWithPreviousWithout248Fields() throws JSONBException, SolrDocStoreConnectorException {
        final HoldingsItems holdingsItems1 = new HoldingsItems();
        holdingsItems1.setAgencyId(123456);
        holdingsItems1.setBibliographicRecordId("postfixIdWithPreviousWithout248Fields__1");
        final HoldingsItems holdingsItems2 = new HoldingsItems();
        holdingsItems2.setAgencyId(123456);
        holdingsItems2.setBibliographicRecordId("postfixIdWithPreviousWithout248Fields__2");

        final List<HoldingsItems> expectedList = Arrays.asList(holdingsItems1, holdingsItems2,
                createDeletion(holdingsItems1.getAgencyId(), "postfixIdWithPreviousWithout248Fields", "test"));

        when(solrDocStoreConnector.holdingExists(
                holdingsItems1.getAgencyId(), "postfixIdWithPreviousWithout248Fields"))
                .thenReturn(true);
        when(solrDocStoreConnector.holdingExists(
                holdingsItems1.getAgencyId(), withPostfix("postfixIdWithPreviousWithout248Fields", 3)))
                .thenReturn(false);

        final String json = jsonbContext.marshall(Arrays.asList(holdingsItems1, holdingsItems2));
        final List<HoldingsItems> holdingsItemsList = holdingsItemsUnmarshaller.unmarshall(
                json.getBytes(StandardCharsets.UTF_8), "test");
        assertThat(holdingsItemsList, is(expectedList));
    }

    @Test
    void postfixIdWithPreviousWith248Fields() throws JSONBException, SolrDocStoreConnectorException {
        final HoldingsItems holdingsItems1 = new HoldingsItems();
        holdingsItems1.setAgencyId(123456);
        holdingsItems1.setBibliographicRecordId("postfixIdWithPreviousWith248Fields__1");
        final HoldingsItems holdingsItems2 = new HoldingsItems();
        holdingsItems2.setAgencyId(123456);
        holdingsItems2.setBibliographicRecordId("postfixIdWithPreviousWith248Fields__2");

        final List<HoldingsItems> expectedList = Arrays.asList(holdingsItems1, holdingsItems2,
                createDeletion(holdingsItems1.getAgencyId(), withPostfix("postfixIdWithPreviousWith248Fields", 3), "test"),
                createDeletion(holdingsItems1.getAgencyId(), withPostfix("postfixIdWithPreviousWith248Fields", 4), "test"));

        when(solrDocStoreConnector.holdingExists(
                holdingsItems1.getAgencyId(), withPostfix("postfixIdWithPreviousWith248Fields", 3)))
                .thenReturn(true);
        when(solrDocStoreConnector.holdingExists(
                holdingsItems1.getAgencyId(), withPostfix("postfixIdWithPreviousWith248Fields", 4)))
                .thenReturn(true);
        when(solrDocStoreConnector.holdingExists(
                holdingsItems1.getAgencyId(), withPostfix("postfixIdWithPreviousWith248Fields", 5)))
                .thenReturn(false);

        final String json = jsonbContext.marshall(Arrays.asList(holdingsItems1, holdingsItems2));
        final List<HoldingsItems> holdingsItemsList = holdingsItemsUnmarshaller.unmarshall(
                json.getBytes(StandardCharsets.UTF_8), "test");
        assertThat(holdingsItemsList, is(expectedList));
    }

    private HoldingsItemsUnmarshaller newHoldingsItemsUnmarshaller() {
        final HoldingsItemsUnmarshaller holdingsItemsUnmarshaller = new HoldingsItemsUnmarshaller();
        holdingsItemsUnmarshaller.solrDocStoreConnector = solrDocStoreConnector;
        return holdingsItemsUnmarshaller;
    }

    private String withPostfix(String bibliographicRecordId, int postfix) {
        return String.format("%s__%d", bibliographicRecordId, postfix);
    }

    private HoldingsItems createDeletion(int agencyId, String bibliographicId, String trackingId) {
        final HoldingsItems holdingsItems = new HoldingsItems();
        holdingsItems.setAgencyId(agencyId);
        holdingsItems.setBibliographicRecordId(bibliographicId);
        holdingsItems.setTrackingId(trackingId);
        holdingsItems.setIndexKeys(Collections.emptyList());
        return holdingsItems;
    }
}