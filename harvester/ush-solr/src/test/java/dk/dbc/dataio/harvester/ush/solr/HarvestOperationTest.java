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

package dk.dbc.dataio.harvester.ush.solr;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.bfs.api.BinaryFileStore;
import dk.dbc.dataio.bfs.api.BinaryFileStoreFsImpl;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.UshHarvesterProperties;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
import dk.dbc.dataio.harvester.utils.ush.UshSolrConnector;
import dk.dbc.dataio.harvester.utils.ush.UshSolrDocument;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class HarvestOperationTest {
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final FileStoreServiceConnector fileStoreServiceConnector = mock(FileStoreServiceConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final UshSolrConnector ushSolrConnector = mock(UshSolrConnector.class);
    private final UshSolrConnector.ResultSet resultSet = mock(UshSolrConnector.ResultSet.class);

    private BinaryFileStore binaryFileStore;
    private final int ushHarvesterJobId = 42;
    private final Date solrTimeOfLastHarvest = new Date(0);
    private final Date ushTimeOfLastHarvest = new Date(1);

    private final JSONBContext jsonbContext = new JSONBContext();

    private final byte[] marcXchangeWrappedInOai =
            ("<record xmlns=\"http://www.openarchives.org/OAI/2.0/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
             "  <header>\n" +
             "    <identifier>urn:repox.ist.utl.pt:marcX810010:oai:rex.kb.dk:KGL01-000574078</identifier>\n" +
             "    <datestamp>2016-03-03</datestamp>\n" +
             "    <setSpec>marcX810010</setSpec>\n" +
             "  </header>\n" +
             "  <metadata>\n" +
             "    <marcx:record xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\" format=\"danMARC2\" type=\"Bibliographic\" xsi:schemaLocation=\"info:lc/xmlns/marcxchange-v1 http://www.loc.gov/standards/iso25577/marcxchange-1-1.xsd\">\n" +
             "      <marcx:leader>-----nam--22------a-4500</marcx:leader>\n" +
             "      <marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"001\">\n" +
             "        <marcx:subfield code=\"a\">000574078</marcx:subfield>\n" +
             "        <marcx:subfield code=\"b\">810010</marcx:subfield>\n" +
             "        <marcx:subfield code=\"f\">a</marcx:subfield>\n" +
             "      </marcx:datafield>\n" +
             "    </marcx:record>\n" +
             "  </metadata>\n" +
             "</record>").getBytes(StandardCharsets.UTF_8);

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Before
    public void setup() {
        binaryFileStore = new BinaryFileStoreFsImpl(folder.getRoot().toPath());
    }

    @Test(expected = NullPointerException.class)
    public void constructor_configArgIsNull_throws() throws HarvesterException {
        new HarvestOperation(null, flowStoreServiceConnector, binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_flowStoreServiceConnectorArgIsNull_throws() throws HarvesterException {
        new HarvestOperation(newUshSolrHarvesterConfig(), null, binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_binaryFileStoreArgIsNull_throws() throws HarvesterException {
        new HarvestOperation(newUshSolrHarvesterConfig(), flowStoreServiceConnector, null, fileStoreServiceConnector, jobStoreServiceConnector);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_fileStoreServiceConnectorArgIsNull_throws() throws HarvesterException {
        new HarvestOperation(newUshSolrHarvesterConfig(), flowStoreServiceConnector, binaryFileStore, null, jobStoreServiceConnector);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_jobStoreServiceConnectorArgIsNull_throws() throws HarvesterException {
        new HarvestOperation(newUshSolrHarvesterConfig(), flowStoreServiceConnector, binaryFileStore, fileStoreServiceConnector, null);
    }

    @Test
    public void execute_returnsNumberOfRecordsAdded() throws HarvesterException, FlowStoreServiceConnectorException {
        final HarvestOperation harvestOperation = Mockito.spy(newHarvestOperation(newUshSolrHarvesterConfig()));
        when(resultSet.iterator()).thenReturn(Arrays.asList(
                createUshSolrDocument(marcXchangeWrappedInOai),
                createUshSolrDocument(marcXchangeWrappedInOai)).iterator());

        assertThat(harvestOperation.execute(), is(2));
    }

    @Test
    public void executeTest_returnsOptional() throws HarvesterException, FlowStoreServiceConnectorException {
        final HarvestOperation harvestOperation = Mockito.spy(newHarvestOperation(newUshSolrHarvesterConfig()));
        when(resultSet.iterator()).thenReturn(Arrays.asList(
                createUshSolrDocument(marcXchangeWrappedInOai),
                createUshSolrDocument(marcXchangeWrappedInOai)).iterator());

        assertThat(harvestOperation.executeTest(), is(Optional.empty()));
    }

    @Test
    public void execute_updatesHarvesterConfigInFlowStore() throws HarvesterException, FlowStoreServiceConnectorException, JobStoreServiceConnectorException {
        final HarvestOperation harvestOperation = Mockito.spy(newHarvestOperation(newUshSolrHarvesterConfig()));
        when(resultSet.iterator()).thenReturn(Collections.singletonList(createUshSolrDocument(marcXchangeWrappedInOai)).iterator());

        harvestOperation.execute();

        verify(flowStoreServiceConnector, times(1)).updateHarvesterConfig(any(HarvesterConfig.class));
    }

    @Test
    public void execute_solrDocumentContainsNoMarcXchange_addsAddiRecordToDatafile()
            throws HarvesterException, FlowStoreServiceConnectorException, FileStoreServiceConnectorException {
        final HarvestOperation harvestOperation = Mockito.spy(newHarvestOperation(newUshSolrHarvesterConfig()));
        when(resultSet.iterator()).thenReturn(Collections.singletonList(createUshSolrDocument("<record/>".getBytes())).iterator());

        harvestOperation.execute();

        verify(fileStoreServiceConnector, times(1)).addFile(any(InputStream.class));
        verify(flowStoreServiceConnector, times(1)).updateHarvesterConfig(any(HarvesterConfig.class));
    }

    @Test
    public void toAddiRecord_trimsOaiTags() throws HarvesterException {
        final UshSolrDocument ushSolrDocument = createUshSolrDocument(marcXchangeWrappedInOai);
        final HarvestOperation harvestOperation = newHarvestOperation(newUshSolrHarvesterConfig());
        final AddiRecord addiRecord = harvestOperation.toAddiRecord(ushSolrDocument);
        assertThat(new String(addiRecord.getContentData()).startsWith("<record xmlns='info:lc/xmlns/marcxchange-v1'"), is(true));
    }

    @Test
    public void toAddiRecord_solrDocumentContainsNoMarcXchange_returnsAddiRecordWithDiagnostic() throws HarvesterException, JSONBException {
        final byte[] originalContent = "<record/>".getBytes();
        final UshSolrDocument ushSolrDocument = createUshSolrDocument(originalContent);
        final HarvestOperation harvestOperation = newHarvestOperation(newUshSolrHarvesterConfig());

        final AddiRecord addiRecord = harvestOperation.toAddiRecord(ushSolrDocument);
        assertThat("Addi record content data", addiRecord.getContentData(), is(originalContent));

        final AddiMetaData addiMetaData = jsonbContext.unmarshall(new String(addiRecord.getMetaData()), AddiMetaData.class);
        assertThat("Addi record metadata has diagnostic", addiMetaData.diagnostic(), is(notNullValue()));
    }

    @Test
    public void toAddiRecord_solrDocumentHasNonXmlRecordContent_returnsAddiRecordWithDiagnostic() throws HarvesterException, JSONBException {
        final byte[] originalContent = "not xml".getBytes();
        final UshSolrDocument ushSolrDocument = createUshSolrDocument(originalContent);
        final HarvestOperation harvestOperation = newHarvestOperation(newUshSolrHarvesterConfig());

        final AddiRecord addiRecord = harvestOperation.toAddiRecord(ushSolrDocument);
        assertThat("Addi record content data", addiRecord.getContentData(), is(originalContent));

        final AddiMetaData addiMetaData = jsonbContext.unmarshall(new String(addiRecord.getMetaData()), AddiMetaData.class);
        assertThat("Addi record metadata has diagnostic", addiMetaData.diagnostic(), is(notNullValue()));
    }

    @Test
    public void getJobSpecificationTemplate_interpolatesConfigValues() throws HarvesterException {
        final JobSpecification expectedJobSpecificationTemplate = new JobSpecificationBuilder()
                .setSubmitterId(424242)
                .setDestination("testbase")
                .setPackaging("addi-xml")
                .setFormat("marc2")
                .setCharset("utf8")
                .setMailForNotificationAboutVerification("placeholder")
                .setMailForNotificationAboutProcessing("placeholder")
                .setResultmailInitials("placeholder")
                .setDataFile("placeholder")
                .setType(JobSpecification.Type.TRANSIENT)
                .setAncestry(new JobSpecification.Ancestry()
                    .withHarvesterToken("ush-solr:1:1:0:1"))
                .build();

        final UshSolrHarvesterConfig ushSolrHarvesterConfig = newUshSolrHarvesterConfig();
        ushSolrHarvesterConfig.getContent()
                .withSubmitterNumber((int) expectedJobSpecificationTemplate.getSubmitterId())
                .withDestination(expectedJobSpecificationTemplate.getDestination())
                .withFormat(expectedJobSpecificationTemplate.getFormat());

        final HarvestOperation harvestOperation = newHarvestOperation(ushSolrHarvesterConfig);
        assertThat(harvestOperation.getJobSpecificationTemplate(JobSpecification.Type.TRANSIENT), is(expectedJobSpecificationTemplate));
    }

    private HarvestOperation newHarvestOperation(UshSolrHarvesterConfig config) throws HarvesterException {
        HarvestOperation harvestOperation = new HarvestOperation(config, flowStoreServiceConnector, binaryFileStore, fileStoreServiceConnector, jobStoreServiceConnector);
        harvestOperation.ushSolrConnector = ushSolrConnector;
        when(ushSolrConnector.findDatabaseDocumentsHarvestedInInterval(anyString(), any(Date.class), any(Date.class))).thenReturn(resultSet);

        return harvestOperation;
    }

    private UshSolrHarvesterConfig newUshSolrHarvesterConfig() {
        return new UshSolrHarvesterConfig(1, 1, new UshSolrHarvesterConfig.Content()
            .withUshHarvesterJobId(ushHarvesterJobId)
            .withTimeOfLastHarvest(solrTimeOfLastHarvest)
                .withFormat("format")
                .withDestination("destination")
                .withSubmitterNumber(42)
                .withUshHarvesterProperties(
                    new UshHarvesterProperties()
                        .withLastHarvestFinishedDate(ushTimeOfLastHarvest)
                .withStorageUrl("url")));
    }

    private UshSolrDocument createUshSolrDocument(byte[] content) {
        final UshSolrDocument document = new UshSolrDocument();
        document.contentBinary = new ArrayList<>();
        document.contentBinary.add(content);
        return document;
    }
}