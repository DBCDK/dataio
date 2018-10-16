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

import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.marcxmerge.MarcXMerger;
import dk.dbc.marcxmerge.MarcXMergerException;
import dk.dbc.rawrepo.MockedRecord;
import dk.dbc.rawrepo.RawRepoDAO;
import dk.dbc.rawrepo.RawRepoException;
import dk.dbc.rawrepo.Record;
import dk.dbc.rawrepo.RecordId;
import dk.dbc.rawrepo.RelationHintsOpenAgency;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.naming.Context;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RawRepoConnectorTest {
    private static final String DATA_SOURCE_RESOURCE_NAME = "resourceName";

    private final DataSource dataSource = mock(DataSource.class);
    private final RawRepoDAO rawRepoDAO = mock(RawRepoDAO.class);
    private final RelationHintsOpenAgency relationHints = mock(RelationHintsOpenAgency.class);

    @BeforeClass
    public static void setupClass() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @Before
    public void setupTest() {
        InMemoryInitialContextFactory.bind(DATA_SOURCE_RESOURCE_NAME, dataSource);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_dataSourceResourceNameIsNull_throws() {
        new RawRepoConnector((String) null, relationHints);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_dataSourceIsNull_throws() {
        new RawRepoConnector((DataSource) null, relationHints);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_dataSourceResourceNameIsEmpty_throws() {
        new RawRepoConnector("", relationHints);
    }

    @Test(expected = IllegalStateException.class)
    public void constructor_dataSourceResourceNameLookupThrowsNamingException_throws() {
        new RawRepoConnector("noSuchResource", relationHints);
    }

    @Test(expected = IllegalStateException.class)
    public void constructor_dataSourceResourceNameLookupReturnsNonDataSourceObject_throws() {
        InMemoryInitialContextFactory.bind(DATA_SOURCE_RESOURCE_NAME, "notDataSource");
        new RawRepoConnector(DATA_SOURCE_RESOURCE_NAME, relationHints);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_relationalHintsIsNull_throws() {
        new RawRepoConnector(DATA_SOURCE_RESOURCE_NAME, null);
    }

    @Test
    public void constructor_dataSourceResourceNameLookupReturnsDataSourceObject_returnsNewInstance() {
        final RawRepoConnector connector = getRawRepoConnector();
        assertThat("connector", connector, is(notNullValue()));
        assertThat("connector.dataSource", connector.getDataSource(), is(dataSource));
    }

    @Test
    public void dequeue_consumerIdArgIsNull_throws() throws SQLException, RawRepoException {
        final RawRepoConnector connector = getRawRepoConnector();
        try {
            connector.dequeue(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void dequeue_consumerIdArgIsEmpty_throws() throws SQLException, RawRepoException {
        final RawRepoConnector connector = getRawRepoConnector();
        try {
            connector.dequeue("");
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void queueFail_queueJobsArgIsNull_throws() throws SQLException, RawRepoException {
        final RawRepoConnector connector = getRawRepoConnector();
        try {
            connector.queueFail(null, "error");
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void getStringRecordMap_recordIsDeleted_returnsRecordMapDerivedFromFetchMergedRecord() throws RawRepoException, MarcXMergerException {
        final RecordId recordId = new RecordId("id", 42);
        final MockedRecord mockedRecord = new MockedRecord(recordId, true);

        when(rawRepoDAO.recordExistsMaybeDeleted(recordId.getBibliographicRecordId(), recordId.getAgencyId()))
                .thenReturn(true);
        when(rawRepoDAO.recordExists(recordId.getBibliographicRecordId(), recordId.getAgencyId()))
                .thenReturn(false);
        when(rawRepoDAO.fetchMergedRecord(eq(recordId.getBibliographicRecordId()), eq(recordId.getAgencyId()), any(MarcXMerger.class), eq(true)))
                .thenReturn(mockedRecord);

        final RawRepoConnector connector = getRawRepoConnector();
        final Map<String, Record> recordMap = connector.getStringRecordMap(recordId, rawRepoDAO, true);

        assertThat("recordMap", recordMap, is(notNullValue()));
        assertThat("recordMap.size", recordMap.size(), is(1));
        assertThat("recordMap.key",  recordMap.containsKey(recordId.getBibliographicRecordId()), is(true));
        assertThat("recordMap.value", (MockedRecord) recordMap.get(recordId.getBibliographicRecordId()), is(mockedRecord));
    }

    @Test
    public void getStringRecordMap_recordIsNotDeleted_returnsRecordMapFromFetchRecordCollection() throws RawRepoException, MarcXMergerException {
        final RecordId recordId = new RecordId("id", 42);
        final MockedRecord mockedRecord = new MockedRecord(recordId, true);
        final Map<String, Record> expectedRecordMap = new HashMap<>(1);
        expectedRecordMap.put("anotherid", mockedRecord);

        when(rawRepoDAO.recordExistsMaybeDeleted(recordId.getBibliographicRecordId(), recordId.getAgencyId()))
                .thenReturn(true);
        when(rawRepoDAO.recordExists(recordId.getBibliographicRecordId(), recordId.getAgencyId()))
                .thenReturn(true);
        when(rawRepoDAO.fetchRecordCollectionExpanded(eq(recordId.getBibliographicRecordId()), eq(recordId.getAgencyId()), any(MarcXMerger.class)))
                .thenReturn(expectedRecordMap);

        final RawRepoConnector connector = getRawRepoConnector();
        final Map<String, Record> recordMap = connector.getStringRecordMap(recordId, rawRepoDAO, true);

        assertThat("recordMap", recordMap, is(expectedRecordMap));
    }

    private RawRepoConnector getRawRepoConnector() {
        return new RawRepoConnector(DATA_SOURCE_RESOURCE_NAME, relationHints);
    }
}
