/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.harvester.promat;

import dk.dbc.dataio.bfs.api.BinaryFileStoreFsImpl;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
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
import dk.dbc.promat.service.connector.PromatServiceConnector;
import dk.dbc.promat.service.connector.PromatServiceConnectorException;
import dk.dbc.promat.service.dto.CaseRequest;
import dk.dbc.promat.service.dto.CaseSummaryList;
import dk.dbc.promat.service.dto.CriteriaOperator;
import dk.dbc.promat.service.dto.ListCasesParams;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HarvestOperationTest {
    private JobStoreServiceConnector jobStoreServiceConnector;
    private MockedFileStoreServiceConnector fileStoreServiceConnector;
    private FlowStoreServiceConnector flowStoreServiceConnector;
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
        promatServiceConnector = mock(PromatServiceConnector.class);
    }

    @Test
    void harvestCases() throws HarvesterException, PromatServiceConnectorException, JobStoreServiceConnectorException,
                               FlowStoreServiceConnectorException {
        final LocalDate creationDate = LocalDate.of(2021, 1, 20);

        final List<PromatCase> cases = new ArrayList<>();
        cases.add(new PromatCase()
                .withNewMessagesToEditor(null)
                .withNewMessagesToReviewer(null)
                .withId(1001)
                .withCreated(creationDate)
                .withTasks(Arrays.asList(
                        new PromatTask()
                                .withTaskFieldType(TaskFieldType.BKM)
                                .withData("yes"),
                        new PromatTask()
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withRecordId("41414141"))));
        cases.add(new PromatCase()
                .withNewMessagesToEditor(null)
                .withNewMessagesToReviewer(null)
                .withId(1002)
                .withCreated(creationDate)
                .withTasks(Collections.singletonList(
                        new PromatTask()
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withRecordId("42424242"))));
        cases.add(new PromatCase()
                .withNewMessagesToEditor(null)
                .withNewMessagesToReviewer(null)
                .withId(1003)
                .withCreated(creationDate)
                .withStatus(CaseStatus.PENDING_REVERT)
                .withTasks(Collections.singletonList(
                        new PromatTask()
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withRecordId("43434343"))));

        // Two fetch cycles
        final CaseSummaryList firstCaseSummaryList = new CaseSummaryList();
        firstCaseSummaryList.setNumFound(2);
        firstCaseSummaryList.setCases(cases.subList(0, 2));

        final CaseSummaryList secondCaseSummaryList = new CaseSummaryList();
        secondCaseSummaryList.setNumFound(2);
        secondCaseSummaryList.setCases(cases.subList(2, 3));

        when(promatServiceConnector.listCases(new ListCasesParams()
                .withFormat(ListCasesParams.Format.EXPORT)
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withStatus(CaseStatus.PENDING_REVERT)
                .withTrimmedWeekcodeOperator(CriteriaOperator.LESS_THAN_OR_EQUAL_TO)
                .withTrimmedWeekcode(getWeekcode())
                .withLimit(2)
                .withFrom(0)))
                .thenReturn(firstCaseSummaryList);
        when(promatServiceConnector.listCases(new ListCasesParams()
                .withFormat(ListCasesParams.Format.EXPORT)
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withStatus(CaseStatus.PENDING_REVERT)
                .withTrimmedWeekcodeOperator(CriteriaOperator.LESS_THAN_OR_EQUAL_TO)
                .withTrimmedWeekcode(getWeekcode())
                .withLimit(2)
                .withFrom(1002)))
                .thenReturn(secondCaseSummaryList);


        final List<AddiMetaData> addiMetadataExpectations = new ArrayList<>();
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(JobSpecificationTemplate.SUBMITTER_NUMBER)
                .withFormat("-format-")
                .withTrackingId("promat.HarvestOperationTest.1001")
                .withDeleted(false));
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(JobSpecificationTemplate.SUBMITTER_NUMBER)
                .withFormat("-format-")
                .withTrackingId("promat.HarvestOperationTest.1002")
                .withDeleted(false));
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(JobSpecificationTemplate.SUBMITTER_NUMBER)
                .withFormat("-format-")
                .withTrackingId("promat.HarvestOperationTest.1003")
                .withDeleted(true));

        final List<Expectation> addiContentExpectations = new ArrayList<>();
        addiContentExpectations.add(new Expectation(
                "<PromatCase>" +
                    "<id>1001</id>" +
                    "<created>2021-01-20</created>" +
                    "<tasks>" +
                        "<task>" +
                            "<id>0</id>" +
                            "<taskFieldType>BKM</taskFieldType>" +
                            "<data>yes</data>" +
                        "</task>" +
                        "<task>" +
                            "<id>0</id>" +
                            "<taskFieldType>BRIEF</taskFieldType>" +
                            "<recordId>41414141</recordId>" +
                        "</task>" +
                    "</tasks>" +
                "</PromatCase>"));
        addiContentExpectations.add(new Expectation(
                "<PromatCase>" +
                    "<id>1002</id>" +
                    "<created>2021-01-20</created>" +
                    "<tasks>" +
                        "<task>" +
                            "<id>0</id>" +
                            "<taskFieldType>BRIEF</taskFieldType>" +
                            "<recordId>42424242</recordId>" +
                        "</task>" +
                    "</tasks>" +
                "</PromatCase>"));
        addiContentExpectations.add(new Expectation(
                "<PromatCase>" +
                    "<id>1003</id>" +
                    "<created>2021-01-20</created>" +
                    "<status>PENDING_REVERT</status>" +
                    "<tasks>" +
                        "<task>" +
                            "<id>0</id>" +
                            "<taskFieldType>BRIEF</taskFieldType>" +
                            "<recordId>43434343</recordId>" +
                        "</task>" +
                    "</tasks>" +
                "</PromatCase>"));

        final PromatHarvesterConfig config = newConfig();
        final HarvestOperation harvestOperation = newHarvestOperation(config);
        final int casesHarvested = harvestOperation.execute();

        assertThat("Number of cases harvested", casesHarvested, is(3));

        final AddiFileVerifier addiFileVerifier = new AddiFileVerifier();
        addiFileVerifier.verify(harvesterTmpFile.toFile(), addiMetadataExpectations, addiContentExpectations);

        verify(promatServiceConnector).updateCase(1001, new CaseRequest().withStatus(CaseStatus.EXPORTED));
        verify(promatServiceConnector).updateCase(1002, new CaseRequest().withStatus(CaseStatus.EXPORTED));
        verify(promatServiceConnector).updateCase(1003, new CaseRequest().withStatus(CaseStatus.REVERTED));
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(PromatHarvesterConfig.class));
    }

    @Test
    void noCasesToHarvest() throws HarvesterException, PromatServiceConnectorException, JobStoreServiceConnectorException,
                                   FlowStoreServiceConnectorException {
        when(promatServiceConnector.listCases(new ListCasesParams()
                .withFormat(ListCasesParams.Format.EXPORT)
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

        verify(promatServiceConnector, never()).updateCase(any(Integer.class), any(CaseRequest.class));
        verify(jobStoreServiceConnector, never()).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(PromatHarvesterConfig.class));
    }

    @Test
    void briefTaskWithoutRecordId() throws HarvesterException, PromatServiceConnectorException {
        final LocalDate creationDate = LocalDate.of(2021, 1, 20);

        final List<PromatCase> cases = new ArrayList<>();
        cases.add(new PromatCase()
                .withNewMessagesToEditor(null)
                .withNewMessagesToReviewer(null)
                .withId(1001)
                .withCreated(creationDate)
                .withTasks(Collections.singletonList(
                        new PromatTask()
                                .withTaskFieldType(TaskFieldType.BRIEF))));

        final CaseSummaryList caseSummaryList = new CaseSummaryList();
        caseSummaryList.setNumFound(1);
        caseSummaryList.setCases(cases);

        when(promatServiceConnector.listCases(new ListCasesParams()
                .withFormat(ListCasesParams.Format.EXPORT)
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withStatus(CaseStatus.PENDING_REVERT)
                .withTrimmedWeekcodeOperator(CriteriaOperator.LESS_THAN_OR_EQUAL_TO)
                .withTrimmedWeekcode(getWeekcode())
                .withLimit(2)
                .withFrom(0)))
                .thenReturn(caseSummaryList);

        final PromatHarvesterConfig config = newConfig();
        final HarvestOperation harvestOperation = newHarvestOperation(config);

        final HarvesterException harvesterException = assertThrows(HarvesterException.class, harvestOperation::execute);
        assertThat("Exception message", harvesterException.getMessage(),
                is("Case 1001 contains BRIEF tasks without record ID"));
    }

    private HarvestOperation newHarvestOperation(PromatHarvesterConfig config) {
        HarvestOperation.PROMAT_SERVICE_FETCH_SIZE = 2;     // force multiple iterations when dealing with >2 cases
        return new HarvestOperation(config,
                new BinaryFileStoreFsImpl(tempDir),
                fileStoreServiceConnector,
                flowStoreServiceConnector,
                jobStoreServiceConnector,
                promatServiceConnector);
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