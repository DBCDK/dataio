/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.promat;

import dk.dbc.dataio.bfs.api.BinaryFileStoreFsImpl;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.commons.faust.factory.FaustFactory;
import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.MockedFileStoreServiceConnector;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.PromatHarvesterConfig;
import dk.dbc.dataio.harvester.utils.datafileverifier.AddiFileVerifier;
import dk.dbc.dataio.harvester.utils.datafileverifier.Expectation;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jobstore.types.JobInputStream;
import dk.dbc.opennumberroll.OpennumberRollConnector;
import dk.dbc.opennumberroll.OpennumberRollConnectorException;
import dk.dbc.promat.service.connector.PromatServiceConnector;
import dk.dbc.promat.service.connector.PromatServiceConnectorException;
import dk.dbc.promat.service.dto.CaseRequestDto;
import dk.dbc.promat.service.dto.CaseSummaryList;
import dk.dbc.promat.service.dto.CriteriaOperator;
import dk.dbc.promat.service.persistence.CaseStatus;
import dk.dbc.promat.service.persistence.PromatCase;
import dk.dbc.promat.service.persistence.PromatTask;
import dk.dbc.promat.service.persistence.TaskFieldType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.IsoFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HarvestOperationTest {
    private JobStoreServiceConnector jobStoreServiceConnector;
    private MockedFileStoreServiceConnector fileStoreServiceConnector;
    private FlowStoreServiceConnector flowStoreServiceConnector;
    private OpennumberRollConnector openNumberRollConnector;
    private PromatServiceConnector promatServiceConnector;
    private Path harvesterTmpFile;

    @TempDir Path tempDir;

    @BeforeEach
    void setupMocks() throws JobStoreServiceConnectorException {
        // Intercept harvester data files with mocked FileStoreServiceConnectorBean
        harvesterTmpFile = tempDir.resolve("harvester.dat");
        fileStoreServiceConnector = new MockedFileStoreServiceConnector();
        fileStoreServiceConnector.destinations.add(harvesterTmpFile);

        jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
        when(jobStoreServiceConnector.addJob(any(JobInputStream.class)))
                .thenReturn(new JobInfoSnapshot());

        flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
        openNumberRollConnector = mock(OpennumberRollConnector.class);
        promatServiceConnector = mock(PromatServiceConnector.class);
    }

    @Test
    void harvestCases() throws HarvesterException, PromatServiceConnectorException, OpennumberRollConnectorException,
                               JobStoreServiceConnectorException, FlowStoreServiceConnectorException {
        final LocalDate creationDate = LocalDate.of(2021, 1, 20);

        final List<PromatCase> cases = new ArrayList<>();
        cases.add(new PromatCase()
                .withId(1001)
                .withRecordId("41414141")
                .withCreated(creationDate)
                .withTasks(Collections.singletonList(new PromatTask()
                        .withTaskFieldType(TaskFieldType.BKM)
                        .withData("yes"))));
         // no recordId, get one from opennumberroll
        cases.add(new PromatCase()
                .withId(1002)
                .withCreated(creationDate));
        cases.add(new PromatCase()
                .withId(1003)
                .withRecordId("43434343")
                .withCreated(creationDate)
                .withStatus(CaseStatus.PENDING_REVERT));

        // Two fetch cycles
        final CaseSummaryList firstCaseSummaryList = new CaseSummaryList();
        firstCaseSummaryList.setNumFound(2);
        firstCaseSummaryList.setCases(cases.subList(0, 2));

        final CaseSummaryList secondCaseSummaryList = new CaseSummaryList();
        secondCaseSummaryList.setNumFound(2);
        secondCaseSummaryList.setCases(cases.subList(2, 3));

        when(promatServiceConnector.listCases(new PromatServiceConnector.ListCasesParams()
                .withFormat(PromatServiceConnector.ListCasesParams.Format.EXPORT)
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withStatus(CaseStatus.PENDING_REVERT)
                .withTrimmedWeekcodeOperator(CriteriaOperator.LESS_THAN_OR_EQUAL_TO)
                .withTrimmedWeekcode(getWeekcode())
                .withLimit(2)
                .withFrom(0)))
                .thenReturn(firstCaseSummaryList);
        when(promatServiceConnector.listCases(new PromatServiceConnector.ListCasesParams()
                .withFormat(PromatServiceConnector.ListCasesParams.Format.EXPORT)
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withStatus(CaseStatus.PENDING_REVERT)
                .withTrimmedWeekcodeOperator(CriteriaOperator.LESS_THAN_OR_EQUAL_TO)
                .withTrimmedWeekcode(getWeekcode())
                .withLimit(2)
                .withFrom(1002)))
                .thenReturn(secondCaseSummaryList);

        // A single recordId issued and pushed back
        when(openNumberRollConnector.getId(any(OpennumberRollConnector.Params.class)))
                .thenReturn("42424242");

        when(promatServiceConnector.updateCase(1002, new CaseRequestDto().withRecordId("42424242")))
                .thenReturn(new PromatCase().withId(1002).withRecordId("42424242").withCreated(creationDate));

        final List<AddiMetaData> addiMetadataExpectations = new ArrayList<>();
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(JobSpecificationTemplate.SUBMITTER_NUMBER)
                .withFormat("-format-")
                .withBibliographicRecordId("41414141")
                .withTrackingId("promat.HarvestOperationTest.41414141")
                .withDeleted(false));
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(JobSpecificationTemplate.SUBMITTER_NUMBER)
                .withFormat("-format-")
                .withBibliographicRecordId("42424242")
                .withTrackingId("promat.HarvestOperationTest.42424242")
                .withDeleted(false));
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(JobSpecificationTemplate.SUBMITTER_NUMBER)
                .withFormat("-format-")
                .withBibliographicRecordId("43434343")
                .withTrackingId("promat.HarvestOperationTest.43434343")
                .withDeleted(true));

        final List<Expectation> addiContentExpectations = new ArrayList<>();
        addiContentExpectations.add(new Expectation(
                "<PromatCase>" +
                    "<id>1001</id>" +
                    "<created>2021-01-20</created>" +
                    "<recordId>41414141</recordId>" +
                    "<tasks>" +
                        "<task>" +
                            "<id>0</id>" +
                            "<taskFieldType>BKM</taskFieldType>" +
                            "<data>yes</data>" +
                        "</task>" +
                    "</tasks>" +
                "</PromatCase>"));
        addiContentExpectations.add(new Expectation(
                "<PromatCase>" +
                    "<id>1002</id>" +
                    "<created>2021-01-20</created>" +
                    "<recordId>42424242</recordId>" +
                "</PromatCase>"));
        addiContentExpectations.add(new Expectation(
                "<PromatCase>" +
                    "<id>1003</id>" +
                    "<created>2021-01-20</created>" +
                    "<status>PENDING_REVERT</status>" +
                    "<recordId>43434343</recordId>" +
                "</PromatCase>"));

        final PromatHarvesterConfig config = newConfig();
        final HarvestOperation harvestOperation = newHarvestOperation(config);
        final int casesHarvested = harvestOperation.execute();

        assertThat("Number of cases harvested", casesHarvested, is(3));

        final AddiFileVerifier addiFileVerifier = new AddiFileVerifier();
        addiFileVerifier.verify(harvesterTmpFile.toFile(), addiMetadataExpectations, addiContentExpectations);

        verify(promatServiceConnector).updateCase(1002, new CaseRequestDto().withRecordId("42424242"));
        verify(promatServiceConnector).updateCase(1001, new CaseRequestDto().withStatus(CaseStatus.EXPORTED));
        verify(promatServiceConnector).updateCase(1002, new CaseRequestDto().withStatus(CaseStatus.EXPORTED));
        verify(promatServiceConnector).updateCase(1003, new CaseRequestDto().withStatus(CaseStatus.REVERTED));
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(PromatHarvesterConfig.class));
    }

    @Test
    void noCasesToHarvest() throws HarvesterException, PromatServiceConnectorException,
                                   OpennumberRollConnectorException, JobStoreServiceConnectorException,
                                   FlowStoreServiceConnectorException {
        when(promatServiceConnector.listCases(new PromatServiceConnector.ListCasesParams()
                .withFormat(PromatServiceConnector.ListCasesParams.Format.EXPORT)
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withStatus(CaseStatus.PENDING_REVERT)
                .withTrimmedWeekcodeOperator(CriteriaOperator.LESS_THAN_OR_EQUAL_TO)
                .withTrimmedWeekcode(getWeekcode())
                .withLimit(2)
                .withFrom(0)))
                .thenReturn(new CaseSummaryList().withNumFound(0));

        final PromatHarvesterConfig config = newConfig();
        final HarvestOperation harvestOperation = newHarvestOperation(config);
        final int casesHarvested = harvestOperation.execute();

        assertThat("Number of cases harvested", casesHarvested, is(0));

        verify(openNumberRollConnector, never()).getId(any(OpennumberRollConnector.Params.class));
        verify(promatServiceConnector, never()).updateCase(any(Integer.class), any(CaseRequestDto.class));
        verify(jobStoreServiceConnector, never()).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(PromatHarvesterConfig.class));
    }

    private HarvestOperation newHarvestOperation(PromatHarvesterConfig config) {
        HarvestOperation.PROMAT_SERVICE_FETCH_SIZE = 2;     // force multiple iterations when dealing with >2 cases
        return new HarvestOperation(config,
                new BinaryFileStoreFsImpl(tempDir),
                fileStoreServiceConnector,
                flowStoreServiceConnector,
                jobStoreServiceConnector,
                promatServiceConnector,
                new FaustFactory(openNumberRollConnector, "testroll"));
    }

    private PromatHarvesterConfig newConfig() {
        final PromatHarvesterConfig config = new PromatHarvesterConfig(
                1, 2, new PromatHarvesterConfig.Content());
        config.getContent()
                .withName("HarvestOperationTest")
                .withFormat("-format-")
                .withDestination("-destination-");
        return config;
    }

    private static String getWeekcode() {
        final ZonedDateTime zonedDateTime = Instant.now().atZone(ZoneId.of("Europe/Copenhagen"));
        return String.format("%d%02d",  zonedDateTime.getYear(), zonedDateTime.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }
}