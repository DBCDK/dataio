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
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
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
    private PromatServiceConnector promatServiceConnector;
    private final MetricRegistry metricsHandlerBean = mock(MetricRegistry.class);
    private Path harvesterTmpFile;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setupMocks() throws JobStoreServiceConnectorException {
        // Intercept harvester data files with mocked FileStoreServiceConnectorBean
        harvesterTmpFile = tempDir.resolve("harvester.dat");
        fileStoreServiceConnector = new MockedFileStoreServiceConnector();
        fileStoreServiceConnector.destinations.add(harvesterTmpFile);

        jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
        when(jobStoreServiceConnector.addJob(any(JobInputStream.class)))
                .thenReturn(new JobInfoSnapshot());
        when(metricsHandlerBean.counter(any(String.class))).thenReturn(mock(Counter.class));
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
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withCodes(Arrays.asList("BKM202110", "BKX202107"))
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
                .withStatus(CaseStatus.PENDING_EXPORT)
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
                .withTrimmedWeekcode(HarvestOperation.getWeekcode())
                .withLimit(2)
                .withFrom(0)))
                .thenReturn(firstCaseSummaryList);
        when(promatServiceConnector.listCases(new ListCasesParams()
                .withFormat(ListCasesParams.Format.EXPORT)
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withStatus(CaseStatus.PENDING_REVERT)
                .withTrimmedWeekcodeOperator(CriteriaOperator.LESS_THAN_OR_EQUAL_TO)
                .withTrimmedWeekcode(HarvestOperation.getWeekcode())
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
                        "<status>PENDING_EXPORT</status>" +
                        "<keepEditor>false</keepEditor>" +
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
                        "<codes>" +
                        "<code>BKM202110</code>" +
                        "<code>BKX202107</code>" +
                        "</codes>" +
                        "</PromatCase>"));
        addiContentExpectations.add(new Expectation(
                "<PromatCase>" +
                        "<id>1002</id>" +
                        "<created>2021-01-20</created>" +
                        "<status>PENDING_EXPORT</status>" +
                        "<keepEditor>false</keepEditor>" +
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
                        "<keepEditor>false</keepEditor>" +
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

        verify(promatServiceConnector).updateCase(1001, new CaseRequest().withStatus(CaseStatus.PROCESSING));
        verify(promatServiceConnector).updateCase(1002, new CaseRequest().withStatus(CaseStatus.PROCESSING));
        verify(promatServiceConnector).updateCase(1003, new CaseRequest().withStatus(CaseStatus.PROCESSING));

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
                .withTrimmedWeekcode(HarvestOperation.getWeekcode())
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
    void briefTaskWithoutRecordId() throws PromatServiceConnectorException, HarvesterException, JobStoreServiceConnectorException, FlowStoreServiceConnectorException {
        final LocalDate creationDate = LocalDate.of(2021, 1, 20);

        final List<PromatCase> cases = new ArrayList<>();
        cases.add(new PromatCase()
                .withNewMessagesToEditor(null)
                .withNewMessagesToReviewer(null)
                .withId(1001)
                .withCreated(creationDate)
                .withStatus(CaseStatus.PENDING_EXPORT)
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
                .withTrimmedWeekcode(HarvestOperation.getWeekcode())
                .withLimit(2)
                .withFrom(0)))
                .thenReturn(caseSummaryList);

        final PromatHarvesterConfig config = newConfig();
        final HarvestOperation harvestOperation = newHarvestOperation(config);

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(0));

        // Set to processing before further action
        verify(promatServiceConnector).updateCase(1001, new CaseRequest().withStatus(CaseStatus.PROCESSING));

        // Checks found an error (that may be recoverable), so status is set back to PENDING_EXPORT
        verify(promatServiceConnector).updateCase(1001, new CaseRequest().withStatus(CaseStatus.PENDING_EXPORT));

        // No jobs where added
        verify(jobStoreServiceConnector, never()).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(PromatHarvesterConfig.class));
    }

    @Test
    void revertReviewWithFail() throws PromatServiceConnectorException, HarvesterException, JobStoreServiceConnectorException, FlowStoreServiceConnectorException {
        final LocalDate creationDate = LocalDate.of(2021, 1, 20);

        final List<PromatCase> cases = new ArrayList<>();
        cases.add(new PromatCase()
                .withNewMessagesToEditor(null)
                .withNewMessagesToReviewer(null)
                .withId(1001)
                .withCreated(creationDate)
                .withCodes(Arrays.asList("BKM202110", "BKX202107"))
                .withStatus(CaseStatus.PENDING_REVERT)
                .withTasks(Arrays.asList(
                        new PromatTask()
                                .withTaskFieldType(TaskFieldType.BKM)
                                .withData("yes"),
                        new PromatTask()
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                // Having a BRIEF task without recordid will make this harvest fail and rollback
                )));

        final CaseSummaryList caseSummaryList = new CaseSummaryList();
        caseSummaryList.setNumFound(1);
        caseSummaryList.setCases(cases);

        when(promatServiceConnector.listCases(new ListCasesParams()
                .withFormat(ListCasesParams.Format.EXPORT)
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withStatus(CaseStatus.PENDING_REVERT)
                .withTrimmedWeekcodeOperator(CriteriaOperator.LESS_THAN_OR_EQUAL_TO)
                .withTrimmedWeekcode(HarvestOperation.getWeekcode())
                .withLimit(2)
                .withFrom(0)))
                .thenReturn(caseSummaryList);

        final PromatHarvesterConfig config = newConfig();
        final HarvestOperation harvestOperation = newHarvestOperation(config);

        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(0));

        // Set to processing before further action
        verify(promatServiceConnector).updateCase(1001, new CaseRequest().withStatus(CaseStatus.PROCESSING));

        // Checks found an error (that may be recoverable), so status is set back to PENDING_REVERT
        verify(promatServiceConnector).updateCase(1001, new CaseRequest().withStatus(CaseStatus.PENDING_REVERT));

        // No jobs where added
        verify(jobStoreServiceConnector, never()).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(PromatHarvesterConfig.class));
    }

    @Test
    void exportReviewWithExceptionAfterHarvest() throws PromatServiceConnectorException, HarvesterException, JobStoreServiceConnectorException, FlowStoreServiceConnectorException {
        final LocalDate creationDate = LocalDate.of(2021, 1, 20);

        final List<PromatCase> cases = new ArrayList<>();
        cases.add(new PromatCase()
                .withNewMessagesToEditor(null)
                .withNewMessagesToReviewer(null)
                .withId(1001)
                .withCreated(creationDate)
                .withCodes(Arrays.asList("BKM202110", "BKX202107"))
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withTasks(Arrays.asList(
                        new PromatTask()
                                .withTaskFieldType(TaskFieldType.BKM)
                                .withData("yes"),
                        new PromatTask()
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withRecordId("43434343"))));

        final CaseSummaryList caseSummaryList = new CaseSummaryList();
        caseSummaryList.setNumFound(1);
        caseSummaryList.setCases(cases);

        when(promatServiceConnector.listCases(new ListCasesParams()
                .withFormat(ListCasesParams.Format.EXPORT)
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withStatus(CaseStatus.PENDING_REVERT)
                .withTrimmedWeekcodeOperator(CriteriaOperator.LESS_THAN_OR_EQUAL_TO)
                .withTrimmedWeekcode(HarvestOperation.getWeekcode())
                .withLimit(2)
                .withFrom(0)))
                .thenReturn(caseSummaryList);

        // Setting the status to PROCESSING throws an exception
        when(promatServiceConnector.updateCase(1001, new CaseRequest().withStatus(CaseStatus.PROCESSING)))
                .thenThrow(new PromatServiceConnectorException("Go away!"));

        final PromatHarvesterConfig config = newConfig();
        final HarvestOperation harvestOperation = newHarvestOperation(config);

        // Make sure that even though the first status update did throw, we still do not get an exception
        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(0));

        // Set to processing before further action
        verify(promatServiceConnector).updateCase(1001, new CaseRequest().withStatus(CaseStatus.PROCESSING));

        // Set to exported after processing
        verify(promatServiceConnector, never()).updateCase(1001, new CaseRequest().withStatus(CaseStatus.EXPORTED));

        // No jobs where added
        verify(jobStoreServiceConnector, never()).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(PromatHarvesterConfig.class));
    }

    @Test
    void exportReviewWithExceptionAfterProcessing() throws PromatServiceConnectorException, HarvesterException, JobStoreServiceConnectorException, FlowStoreServiceConnectorException {
        final LocalDate creationDate = LocalDate.of(2021, 1, 20);

        final List<PromatCase> cases = new ArrayList<>();
        cases.add(new PromatCase()
                .withNewMessagesToEditor(null)
                .withNewMessagesToReviewer(null)
                .withId(1001)
                .withCreated(creationDate)
                .withCodes(Arrays.asList("BKM202110", "BKX202107"))
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withTasks(Arrays.asList(
                        new PromatTask()
                                .withTaskFieldType(TaskFieldType.BKM)
                                .withData("yes"),
                        new PromatTask()
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                .withRecordId("43434343"))));

        final CaseSummaryList caseSummaryList = new CaseSummaryList();
        caseSummaryList.setNumFound(1);
        caseSummaryList.setCases(cases);

        when(promatServiceConnector.listCases(new ListCasesParams()
                .withFormat(ListCasesParams.Format.EXPORT)
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withStatus(CaseStatus.PENDING_REVERT)
                .withTrimmedWeekcodeOperator(CriteriaOperator.LESS_THAN_OR_EQUAL_TO)
                .withTrimmedWeekcode(HarvestOperation.getWeekcode())
                .withLimit(2)
                .withFrom(0)))
                .thenReturn(caseSummaryList);

        // Setting the status to EXPORTED throws an exception
        when(promatServiceConnector.updateCase(1001, new CaseRequest().withStatus(CaseStatus.PROCESSING)))
                .thenReturn(cases.get(0).withStatus(CaseStatus.PROCESSING));
        when(promatServiceConnector.updateCase(1001, new CaseRequest().withStatus(CaseStatus.EXPORTED)))
                .thenThrow(new PromatServiceConnectorException("Go away!"));

        final PromatHarvesterConfig config = newConfig();
        final HarvestOperation harvestOperation = newHarvestOperation(config);

        // Make sure that even though the last status update did throw, we still do not get an exception
        final int casesHarvested = harvestOperation.execute();
        assertThat("Number of cases harvested", casesHarvested, is(1));

        // Set to processing before further action
        verify(promatServiceConnector).updateCase(1001, new CaseRequest().withStatus(CaseStatus.PROCESSING));

        // Set to exported after processing
        verify(promatServiceConnector).updateCase(1001, new CaseRequest().withStatus(CaseStatus.EXPORTED));

        // Jobs where added
        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(PromatHarvesterConfig.class));
    }

    @Test
    void harvestCasesWithOneFail() throws HarvesterException, PromatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException {
        final LocalDate creationDate = LocalDate.of(2021, 1, 20);

        final List<PromatCase> cases = new ArrayList<>();
        cases.add(new PromatCase()
                .withNewMessagesToEditor(null)
                .withNewMessagesToReviewer(null)
                .withId(1001)
                .withCreated(creationDate)
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withCodes(Arrays.asList("BKM202110", "BKX202107"))
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
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withTasks(Collections.singletonList(
                        new PromatTask()
                                .withTaskFieldType(TaskFieldType.BRIEF)
                                // Having a BRIEF task without recordid will make this harvest fail and rollback
                )));
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
                .withTrimmedWeekcode(HarvestOperation.getWeekcode())
                .withLimit(2)
                .withFrom(0)))
                .thenReturn(firstCaseSummaryList);
        when(promatServiceConnector.listCases(new ListCasesParams()
                .withFormat(ListCasesParams.Format.EXPORT)
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withStatus(CaseStatus.PENDING_REVERT)
                .withTrimmedWeekcodeOperator(CriteriaOperator.LESS_THAN_OR_EQUAL_TO)
                .withTrimmedWeekcode(HarvestOperation.getWeekcode())
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
                .withTrackingId("promat.HarvestOperationTest.1003")
                .withDeleted(true));

        final List<Expectation> addiContentExpectations = new ArrayList<>();
        addiContentExpectations.add(new Expectation(
                "<PromatCase>" +
                        "<id>1001</id>" +
                        "<created>2021-01-20</created>" +
                        "<status>PENDING_EXPORT</status>" +
                        "<keepEditor>false</keepEditor>" +
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
                        "<codes>" +
                        "<code>BKM202110</code>" +
                        "<code>BKX202107</code>" +
                        "</codes>" +
                        "</PromatCase>"));
        addiContentExpectations.add(new Expectation(
                "<PromatCase>" +
                        "<id>1003</id>" +
                        "<created>2021-01-20</created>" +
                        "<status>PENDING_REVERT</status>" +
                        "<keepEditor>false</keepEditor>" +
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

        assertThat("Number of cases harvested", casesHarvested, is(2));

        final AddiFileVerifier addiFileVerifier = new AddiFileVerifier();
        addiFileVerifier.verify(harvesterTmpFile.toFile(), addiMetadataExpectations, addiContentExpectations);

        verify(promatServiceConnector).updateCase(1001, new CaseRequest().withStatus(CaseStatus.PROCESSING));
        verify(promatServiceConnector).updateCase(1002, new CaseRequest().withStatus(CaseStatus.PROCESSING));
        verify(promatServiceConnector).updateCase(1003, new CaseRequest().withStatus(CaseStatus.PROCESSING));

        verify(promatServiceConnector).updateCase(1001, new CaseRequest().withStatus(CaseStatus.EXPORTED));
        verify(promatServiceConnector).updateCase(1002, new CaseRequest().withStatus(CaseStatus.PENDING_EXPORT));
        verify(promatServiceConnector).updateCase(1003, new CaseRequest().withStatus(CaseStatus.REVERTED));

        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(PromatHarvesterConfig.class));
    }

    @Test
    void harvestCasesWithOneExceptionAfterHarvest() throws HarvesterException, PromatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException {
        final LocalDate creationDate = LocalDate.of(2021, 1, 20);

        final List<PromatCase> cases = new ArrayList<>();
        cases.add(new PromatCase()
                .withNewMessagesToEditor(null)
                .withNewMessagesToReviewer(null)
                .withId(1001)
                .withCreated(creationDate)
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withCodes(Arrays.asList("BKM202110", "BKX202107"))
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
                .withStatus(CaseStatus.PENDING_EXPORT)
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
                .withTrimmedWeekcode(HarvestOperation.getWeekcode())
                .withLimit(2)
                .withFrom(0)))
                .thenReturn(firstCaseSummaryList);
        when(promatServiceConnector.listCases(new ListCasesParams()
                .withFormat(ListCasesParams.Format.EXPORT)
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withStatus(CaseStatus.PENDING_REVERT)
                .withTrimmedWeekcodeOperator(CriteriaOperator.LESS_THAN_OR_EQUAL_TO)
                .withTrimmedWeekcode(HarvestOperation.getWeekcode())
                .withLimit(2)
                .withFrom(1002)))
                .thenReturn(secondCaseSummaryList);

        // Setting the status to EXPORTED on the second case throws an exception
        // (The status of the case in the response data is not used, so no need to bother setting it)
        when(promatServiceConnector.updateCase(1002, new CaseRequest().withStatus(CaseStatus.PROCESSING)))
                .thenThrow(new PromatServiceConnectorException("Sorry we're closed!"));

        final List<AddiMetaData> addiMetadataExpectations = new ArrayList<>();
        addiMetadataExpectations.add(new AddiMetaData()
                .withSubmitterNumber(JobSpecificationTemplate.SUBMITTER_NUMBER)
                .withFormat("-format-")
                .withTrackingId("promat.HarvestOperationTest.1001")
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
                        "<status>PENDING_EXPORT</status>" +
                        "<keepEditor>false</keepEditor>" +
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
                        "<codes>" +
                        "<code>BKM202110</code>" +
                        "<code>BKX202107</code>" +
                        "</codes>" +
                        "</PromatCase>"));
        addiContentExpectations.add(new Expectation(
                "<PromatCase>" +
                        "<id>1003</id>" +
                        "<created>2021-01-20</created>" +
                        "<status>PENDING_REVERT</status>" +
                        "<keepEditor>false</keepEditor>" +
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

        assertThat("Number of cases harvested", casesHarvested, is(2));

        final AddiFileVerifier addiFileVerifier = new AddiFileVerifier();
        addiFileVerifier.verify(harvesterTmpFile.toFile(), addiMetadataExpectations, addiContentExpectations);

        verify(promatServiceConnector).updateCase(1001, new CaseRequest().withStatus(CaseStatus.PROCESSING));
        verify(promatServiceConnector).updateCase(1002, new CaseRequest().withStatus(CaseStatus.PROCESSING));
        verify(promatServiceConnector).updateCase(1003, new CaseRequest().withStatus(CaseStatus.PROCESSING));

        verify(promatServiceConnector).updateCase(1001, new CaseRequest().withStatus(CaseStatus.EXPORTED));
        verify(promatServiceConnector, never()).updateCase(1002, new CaseRequest().withStatus(CaseStatus.EXPORTED));
        verify(promatServiceConnector).updateCase(1003, new CaseRequest().withStatus(CaseStatus.REVERTED));

        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(PromatHarvesterConfig.class));
    }

    @Test
    void harvestCasesWithOneExceptionAfterProcessing() throws HarvesterException, PromatServiceConnectorException, JobStoreServiceConnectorException,
            FlowStoreServiceConnectorException {
        final LocalDate creationDate = LocalDate.of(2021, 1, 20);

        final List<PromatCase> cases = new ArrayList<>();
        cases.add(new PromatCase()
                .withNewMessagesToEditor(null)
                .withNewMessagesToReviewer(null)
                .withId(1001)
                .withCreated(creationDate)
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withCodes(Arrays.asList("BKM202110", "BKX202107"))
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
                .withStatus(CaseStatus.PENDING_EXPORT)
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
                .withTrimmedWeekcode(HarvestOperation.getWeekcode())
                .withLimit(2)
                .withFrom(0)))
                .thenReturn(firstCaseSummaryList);
        when(promatServiceConnector.listCases(new ListCasesParams()
                .withFormat(ListCasesParams.Format.EXPORT)
                .withStatus(CaseStatus.PENDING_EXPORT)
                .withStatus(CaseStatus.PENDING_REVERT)
                .withTrimmedWeekcodeOperator(CriteriaOperator.LESS_THAN_OR_EQUAL_TO)
                .withTrimmedWeekcode(HarvestOperation.getWeekcode())
                .withLimit(2)
                .withFrom(1002)))
                .thenReturn(secondCaseSummaryList);

        // Setting the status to EXPORTED on the second case throws an exception
        // (The status of the case in the response data is not used, so no need to bother setting it)
        when(promatServiceConnector.updateCase(1002, new CaseRequest().withStatus(CaseStatus.PROCESSING)))
                .thenReturn(cases.get(0));
        when(promatServiceConnector.updateCase(1002, new CaseRequest().withStatus(CaseStatus.EXPORTED)))
                .thenThrow(new PromatServiceConnectorException("Sorry we're closed!"));

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
                        "<status>PENDING_EXPORT</status>" +
                        "<keepEditor>false</keepEditor>" +
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
                        "<codes>" +
                        "<code>BKM202110</code>" +
                        "<code>BKX202107</code>" +
                        "</codes>" +
                        "</PromatCase>"));
        addiContentExpectations.add(new Expectation(
                "<PromatCase>" +
                        "<id>1002</id>" +
                        "<created>2021-01-20</created>" +
                        "<status>PENDING_EXPORT</status>" +
                        "<keepEditor>false</keepEditor>" +
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
                        "<keepEditor>false</keepEditor>" +
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

        verify(promatServiceConnector).updateCase(1001, new CaseRequest().withStatus(CaseStatus.PROCESSING));
        verify(promatServiceConnector).updateCase(1002, new CaseRequest().withStatus(CaseStatus.PROCESSING));
        verify(promatServiceConnector).updateCase(1003, new CaseRequest().withStatus(CaseStatus.PROCESSING));

        verify(promatServiceConnector).updateCase(1001, new CaseRequest().withStatus(CaseStatus.EXPORTED));
        verify(promatServiceConnector).updateCase(1002, new CaseRequest().withStatus(CaseStatus.EXPORTED));
        verify(promatServiceConnector).updateCase(1003, new CaseRequest().withStatus(CaseStatus.REVERTED));

        verify(jobStoreServiceConnector).addJob(any(JobInputStream.class));
        verify(flowStoreServiceConnector).updateHarvesterConfig(any(PromatHarvesterConfig.class));
    }

    @Test
    void getWeekcode() {
        final ZoneId zoneId = ZoneId.of("Europe/Copenhagen");

        assertThat("monday in week 25 2021",
                HarvestOperation.getWeekcode(Instant.parse("2021-06-21T10:00:00Z").atZone(zoneId)), is("202125"));
        assertThat("tuesday in week 25 2021",
                HarvestOperation.getWeekcode(Instant.parse("2021-06-22T10:00:00Z").atZone(zoneId)), is("202125"));
        assertThat("wednesday in week 25 2021",
                HarvestOperation.getWeekcode(Instant.parse("2021-06-23T10:00:00Z").atZone(zoneId)), is("202125"));
        assertThat("thursday in week 25 2021",
                HarvestOperation.getWeekcode(Instant.parse("2021-06-24T10:00:00Z").atZone(zoneId)), is("202125"));
        assertThat("friday in week 25 2021",
                HarvestOperation.getWeekcode(Instant.parse("2021-06-25T10:00:00Z").atZone(zoneId)), is("202126"));
        assertThat("saturday in week 25 2021",
                HarvestOperation.getWeekcode(Instant.parse("2021-06-26T10:00:00Z").atZone(zoneId)), is("202126"));
        assertThat("sunday in week 25 2021",
                HarvestOperation.getWeekcode(Instant.parse("2021-06-27T10:00:00Z").atZone(zoneId)), is("202126"));
    }

    private HarvestOperation newHarvestOperation(PromatHarvesterConfig config) {
        HarvestOperation.PROMAT_SERVICE_FETCH_SIZE = 2;     // force multiple iterations when dealing with >2 cases
        return new HarvestOperation(config,
                new BinaryFileStoreFsImpl(tempDir),
                fileStoreServiceConnector,
                flowStoreServiceConnector,
                jobStoreServiceConnector,
                promatServiceConnector,
                metricsHandlerBean);
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
}
