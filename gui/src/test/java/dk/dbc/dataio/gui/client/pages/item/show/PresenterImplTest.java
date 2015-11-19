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


package dk.dbc.dataio.gui.client.pages.item.show;


import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TabBar;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.Range;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.components.JobNotificationPanel;
import dk.dbc.dataio.gui.client.components.PromptedLabel;
import dk.dbc.dataio.gui.client.model.DiagnosticModel;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.modelBuilders.DiagnosticModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.ItemModelBuilder;
import dk.dbc.dataio.gui.client.modelBuilders.JobModelBuilder;
import dk.dbc.dataio.gui.client.pages.PresenterImplTestBase;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxyAsync;
import dk.dbc.dataio.jobstore.test.types.JobNotificationBuilder;
import dk.dbc.dataio.jobstore.types.JobNotification;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;
import dk.dbc.dataio.jobstore.types.criteria.JobListCriteria;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * PresenterImpl unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
@RunWith(GwtMockitoTestRunner.class)
public class PresenterImplTest extends PresenterImplTestBase {

    @Mock View mockedView;
    @Mock ItemsListView mockedAllItemsListView;
    @Mock ItemsListView mockedFailedItemsListView;
    @Mock ItemsListView mockedIgnoredItemsListView;
    @Mock Widget mockedViewWidget;
    @Mock Throwable mockedException;
    @Mock Place mockedPlace;

    @Mock Label mockedJobHeader;
    @Mock CellTable mockedAllItemsTable;
    @Mock CellTable mockedFailedItemsTable;
    @Mock CellTable mockedIgnoredItemsTable;
    @Mock CellTable mockedJobDiagnosticTable;
    @Mock CellTable mockedItemDiagnosticTable;
    @Mock DecoratedTabPanel mockedAllDetailedTabs;
    @Mock DecoratedTabPanel mockedFailedDetailedTabs;
    @Mock DecoratedTabPanel mockedIgnoredDetailedTabs;
    @Mock DecoratedTabPanel mockedTabPanel;
    @Mock JobInfoTabContent mockedJobInfoTabContent;
    @Mock JobDiagnosticTabContent mockedJobDiagnosticTabContent;
    @Mock JobNotificationsTabContent mockedJobNotificationTabContent;
    @Mock ItemDiagnosticTabContent mockedItemDiagnosticTabContent;
    @Mock PromptedLabel mockedPackaging;
    @Mock PromptedLabel mockedFormat;
    @Mock PromptedLabel mockedCharset;
    @Mock PromptedLabel mockedDestination;
    @Mock PromptedLabel mockedMailForNotificationAboutVerification;
    @Mock PromptedLabel mockedMailForNotificationAboutProcessing;
    @Mock PromptedLabel mockedResultMailInitials;
    @Mock PromptedLabel mockedType;
    @Mock PromptedLabel mockedJobCreationTime;
    @Mock PromptedLabel mockedJobCompletionTime;
    @Mock TabBar mockedTabBar;
    @Mock AsyncItemViewDataProvider mockedDataProvider;
    @Mock JobStoreProxyAsync mockedJobStoreProxy;
    @Mock LogStoreProxyAsync mockedLogStoreProxy;
    @Mock ViewGinjector mockedViewInjector;

    private final static String EMPTY = "";
    private final static int OFFSET = 0;
    private final static int ROW_COUNT = 4;


    // Setup mocked data
    @Before
    public void setupMockedData() {
        when(mockedCommonGinjector.getJobStoreProxyAsync()).thenReturn(mockedJobStoreProxy);
        when(mockedViewInjector.getView()).thenReturn(mockedView);
        when(mockedCommonGinjector.getLogStoreProxyAsync()).thenReturn(mockedLogStoreProxy);

        mockedView.jobHeader = mockedJobHeader;
        mockedAllItemsListView.itemsTable = mockedAllItemsTable;
        mockedFailedItemsListView.itemsTable = mockedFailedItemsTable;
        mockedView.jobDiagnosticTabContent = mockedJobDiagnosticTabContent;
        mockedView.jobNotificationsTabContent = mockedJobNotificationTabContent;
        mockedIgnoredItemsListView.itemsTable = mockedIgnoredItemsTable;
        mockedAllItemsListView.itemDiagnosticTabContent = mockedItemDiagnosticTabContent;
        mockedAllItemsListView.itemDiagnosticTabContent.itemDiagnosticTable = mockedItemDiagnosticTable;
        mockedFailedItemsListView.itemDiagnosticTabContent = mockedItemDiagnosticTabContent;
        mockedFailedItemsListView.itemDiagnosticTabContent.itemDiagnosticTable = mockedItemDiagnosticTable;
        mockedIgnoredItemsListView.itemDiagnosticTabContent = mockedItemDiagnosticTabContent;
        mockedIgnoredItemsListView.itemDiagnosticTabContent.itemDiagnosticTable = mockedItemDiagnosticTable;
        mockedView.jobDiagnosticTabContent.jobDiagnosticTable = mockedJobDiagnosticTable;
        mockedAllItemsListView.detailedTabs = mockedAllDetailedTabs;
        mockedFailedItemsListView.detailedTabs = mockedFailedDetailedTabs;
        mockedIgnoredItemsListView.detailedTabs = mockedIgnoredDetailedTabs;
        mockedView.allItemsList = mockedAllItemsListView;
        mockedView.failedItemsList = mockedFailedItemsListView;
        mockedView.ignoredItemsList = mockedIgnoredItemsListView;
        mockedView.tabPanel = mockedTabPanel;
        when(mockedTabPanel.getTabBar()).thenReturn(mockedTabBar);
        when(mockedAllDetailedTabs.getTabBar()).thenReturn(mockedTabBar);
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);
        when(mockedView.allItemsList.itemsTable.getVisibleRange()).thenReturn(new Range(OFFSET, ROW_COUNT));
        when(mockedView.failedItemsList.itemsTable.getVisibleRange()).thenReturn(new Range(OFFSET, ROW_COUNT));
        when(mockedView.ignoredItemsList.itemsTable.getVisibleRange()).thenReturn(new Range(OFFSET, ROW_COUNT));
        mockedView.jobInfoTabContent = mockedJobInfoTabContent;
        mockedView.jobInfoTabContent.packaging = mockedPackaging;
        mockedView.jobInfoTabContent.format = mockedFormat;
        mockedView.jobInfoTabContent.charset = mockedCharset;
        mockedView.jobInfoTabContent.destination = mockedDestination;
        mockedView.jobInfoTabContent.mailForNotificationAboutVerification = mockedMailForNotificationAboutVerification;
        mockedView.jobInfoTabContent.mailForNotificationAboutProcessing = mockedMailForNotificationAboutProcessing;
        mockedView.jobInfoTabContent.resultMailInitials = mockedResultMailInitials;
        mockedView.jobInfoTabContent.type = mockedType;
        mockedView.jobInfoTabContent.jobCreationTime = mockedJobCreationTime;
        mockedView.jobInfoTabContent.jobCompletionTime = mockedJobCompletionTime;
        mockedView.dataProvider = mockedDataProvider;
    }

    // Mocked Texts
    @Mock Texts mockedText;
    final static String MOCKED_MENU_ITEMS = "Mocked Poster";
    final static String MOCKED_COLUMN_ITEM = "Mocked Post";
    final static String MOCKED_COLUMN_STATUS = "Mocked Status";
    final static String MOCKED_COLUMN_LEVEL = "Niveau";
    final static String MOCKED_COLUMN_MESSAGE = "Besked";
    final static String MOCKED_ERROR_COULDNOTFETCHJOB = "Mocked Det var ikke muligt at hente jobbet fra Job Store";
    final static String MOCKED_ERROR_COULDNOTFINDJOB = "Mocked Kunne ikke finde det ønskede job i Job Store";
    final static String MOCKED_ERROR_COULDNOTFETCHITEMS = "Mocked Det var ikke muligt at hente poster fra Job Store";
    final static String MOCKED_ERROR_CANNOTNOTFETCHJAVASCRIPTLOG = "Mocked Det var ikke muligt at hente java script loggen";
    final static String MOCKED_ERROR_COULDNOTFETCHDATA = "Det var ikke muligt at hente data fra Job Store";
    final static String MOCKED_ERROR_COULDNOTFETCHJOBNOTIFICATIONS = "Det var ikke muligt at hente Job Notifications fra Job Store";
    final static String MOCKED_LABEL_BACK = "Mocked Tilbage til Joboversigten";
    final static String MOCKED_TEXT_ITEM = "Mocked Post";
    final static String MOCKED_TEXT_JOBID = "Mocked Job Id:";
    final static String MOCKED_TEXT_SUBMITTER = "Mocked Submitter:";
    final static String MOCKED_TEXT_SINK = "Mocked Sink:";
    final static String MOCKED_LIFECYCLE_PARTITIONING = "Mocked Partitioning";
    final static String MOCKED_LIFECYCLE_PROCESSING = "Mocked Processing";
    final static String MOCKED_LIFECYCLE_DELIVERING = "Mocked Delivering";
    final static String MOCKED_LIFECYCLE_DONE = "Mocked Done";
    final static String MOCKED_LIFECYCLE_UNKNOWN = "Mocked Ukendt Lifecycle";
    final static String MOCKED_TAB_ALLITEMS = "Mocked Alle poster";
    final static String MOCKED_TAB_FAILEDITEMS = "Mocked Fejlede poster";
    final static String MOCKED_TAB_IGNOREDITEMS = "Mocked Ignorerede poster";
    final static String MOCKED_TAB_JOBINFO = "Mocked Job info";
    final static String MOCKED_TAB_JOBDIAGNOSTIC = "Job diagnose";
    final static String MOCKED_TAB_JOBNOTIFICATION = "Job Notifikation";
    final static String MOCKED_TAB_JAVASCRIPTLOG = "Mocked Javascript log";
    final static String MOCKED_TAB_PARTITIONINGPOST = "Input post";
    final static String MOCKED_TAB_PROCESSINGPOST = "Output post";
    final static String MOCKED_TAB_DELIVERINGPOST = "Sinkresultat";
    final static String MOCKED_TAB_NEXT_OUTPUTPOST = "Mocked next output post";
    final static String MOCKED_TAB_ITEM_DIAGNOSTIC = "Mocked item diagnostic result";
    final static String MOCKED_PROMPTJOBINFO_PACKAGING = "Rammeformat";
    final static String MOCKED_PROMPTJOBINFO_FORMAT = "Indholdsformat";
    final static String MOCKED_PROMPTJOBINFO_CHARSET = "Tegnsæt";
    final static String MOCKED_PROMPTJOBINFO_DESTINATION = "Destination";
    final static String MOCKED_PROMPTJOBINFO_MAILFORNOTIFICATIONABOUTVERIFICATION = "Mail 1";
    final static String MOCKED_PROMPTJOBINFO_MAILFORNOTIFICATIONABOUTPROCESSING = "Mail 2";
    final static String MOCKED_PROMPTJOBINFO_RESULTMAILINITIALS = "Initialer";
    final static String MOCKED_PROMPTJOBINFO_TYPE = "Type";
    final static String MOCKED_PROMPTJOBINFO_JOBCREATIONTIME   = "JOB startet:";
    final static String MOCKED_PROMPTJOBINFO_JOBCOMPLETIONTIME = "JOB afsluttet:";
    final static String MOCKED_PROMPTJOBNOTIFICATION_JOBID = "Job Id";
    final static String MOCKED_PROMPTJOBNOTIFICATION_DESTINATION = "Modtager";
    final static String MOCKED_PROMPTJOBNOTIFICATION_JOBCREATIONTIME = "Oprettet";
    final static String MOCKED_PROMPTJOBNOTIFICATION_JOBCOMPLETIONTIME = "Afsendt";
    final static String MOCKED_PROMPTJOBNOTIFICATION_TYPE = "Type";
    final static String MOCKED_PROMPTJOBNOTIFICATION_STATUS = "Status";
    final static String MOCKED_PROMPTJOBNOTIFICATION_STATUSMESSAGE = "Status besked";
    final static String MOCKED_TYPEJOBCOMPLETED = "Completed";
    final static String MOCKED_TYPEJOBCREATED = "Created";
    final static String MOCKED_STATUSCOMPLETED = "Completed";
    final static String MOCKED_STATUSFAILED = "Failed";
    final static String MOCKED_STATUSWAITING = "Waiting";

    @Before
    public void setupMockedTextsBehaviour() {
        when(mockedMenuTexts.menu_Items()).thenReturn(MOCKED_MENU_ITEMS);
        when(mockedText.column_Item()).thenReturn(MOCKED_COLUMN_ITEM);
        when(mockedText.column_Status()).thenReturn(MOCKED_COLUMN_STATUS);
        when(mockedText.column_Level()).thenReturn(MOCKED_COLUMN_LEVEL);
        when(mockedText.column_Message()).thenReturn(MOCKED_COLUMN_MESSAGE);
        when(mockedText.error_CouldNotFetchJob()).thenReturn(MOCKED_ERROR_COULDNOTFETCHJOB);
        when(mockedText.error_CouldNotFindJob()).thenReturn(MOCKED_ERROR_COULDNOTFINDJOB);
        when(mockedText.error_CouldNotFetchItems()).thenReturn(MOCKED_ERROR_COULDNOTFETCHITEMS);
        when(mockedText.error_CannotFetchJavaScriptLog()).thenReturn(MOCKED_ERROR_CANNOTNOTFETCHJAVASCRIPTLOG);
        when(mockedText.error_CouldNotFetchData()).thenReturn(MOCKED_ERROR_COULDNOTFETCHDATA);
        when(mockedText.error_CouldNotFetchJobNotifications()).thenReturn(MOCKED_ERROR_COULDNOTFETCHJOBNOTIFICATIONS);
        when(mockedText.label_Back()).thenReturn(MOCKED_LABEL_BACK);
        when(mockedText.text_Item()).thenReturn(MOCKED_TEXT_ITEM);
        when(mockedText.text_JobId()).thenReturn(MOCKED_TEXT_JOBID);
        when(mockedText.text_Submitter()).thenReturn(MOCKED_TEXT_SUBMITTER);
        when(mockedText.text_Sink()).thenReturn(MOCKED_TEXT_SINK);
        when(mockedText.lifecycle_Partitioning()).thenReturn(MOCKED_LIFECYCLE_PARTITIONING);
        when(mockedText.lifecycle_Processing()).thenReturn(MOCKED_LIFECYCLE_PROCESSING);
        when(mockedText.lifecycle_Delivering()).thenReturn(MOCKED_LIFECYCLE_DELIVERING);
        when(mockedText.lifecycle_Done()).thenReturn(MOCKED_LIFECYCLE_DONE);
        when(mockedText.lifecycle_Unknown()).thenReturn(MOCKED_LIFECYCLE_UNKNOWN);
        when(mockedText.tab_AllItems()).thenReturn(MOCKED_TAB_ALLITEMS);
        when(mockedText.tab_FailedItems()).thenReturn(MOCKED_TAB_FAILEDITEMS);
        when(mockedText.tab_IgnoredItems()).thenReturn(MOCKED_TAB_IGNOREDITEMS);
        when(mockedText.tab_JobInfo()).thenReturn(MOCKED_TAB_JOBINFO);
        when(mockedText.tab_JobDiagnostic()).thenReturn(MOCKED_TAB_JOBDIAGNOSTIC);
        when(mockedText.tab_JobNotification()).thenReturn(MOCKED_TAB_JOBNOTIFICATION);
        when(mockedText.tab_JavascriptLog()).thenReturn(MOCKED_TAB_JAVASCRIPTLOG);
        when(mockedText.tab_PartitioningPost()).thenReturn(MOCKED_TAB_PARTITIONINGPOST);
        when(mockedText.tab_ProcessingPost()).thenReturn(MOCKED_TAB_PROCESSINGPOST);
        when(mockedText.tab_DeliveringPost()).thenReturn(MOCKED_TAB_DELIVERINGPOST);
        when(mockedText.tab_NextOutputPost()).thenReturn(MOCKED_TAB_NEXT_OUTPUTPOST);
        when(mockedText.tab_ItemDiagnostic()).thenReturn(MOCKED_TAB_ITEM_DIAGNOSTIC);
        when(mockedText.promptJobInfo_Packaging()).thenReturn(MOCKED_PROMPTJOBINFO_PACKAGING);
        when(mockedText.promptJobInfo_Format()).thenReturn(MOCKED_PROMPTJOBINFO_FORMAT);
        when(mockedText.promptJobInfo_Charset()).thenReturn(MOCKED_PROMPTJOBINFO_CHARSET);
        when(mockedText.promptJobInfo_Destination()).thenReturn(MOCKED_PROMPTJOBINFO_DESTINATION);
        when(mockedText.promptJobInfo_MailForNotificationAboutVerification()).thenReturn(MOCKED_PROMPTJOBINFO_MAILFORNOTIFICATIONABOUTVERIFICATION);
        when(mockedText.promptJobInfo_MailForNotificationAboutProcessing()).thenReturn(MOCKED_PROMPTJOBINFO_MAILFORNOTIFICATIONABOUTPROCESSING);
        when(mockedText.promptJobInfo_ResultMailInitials()).thenReturn(MOCKED_PROMPTJOBINFO_RESULTMAILINITIALS);
        when(mockedText.promptJobInfo_Type()).thenReturn(MOCKED_PROMPTJOBINFO_TYPE);
        when(mockedText.promptJobInfo_JobCreationTime()).thenReturn(MOCKED_PROMPTJOBINFO_JOBCREATIONTIME);
        when(mockedText.promptJobInfo_JobCompletionTime()).thenReturn(MOCKED_PROMPTJOBINFO_JOBCOMPLETIONTIME);
        when(mockedText.promptJobNotification_JobId()).thenReturn(MOCKED_PROMPTJOBNOTIFICATION_JOBID);
        when(mockedText.promptJobNotification_Destination()).thenReturn(MOCKED_PROMPTJOBNOTIFICATION_DESTINATION);
        when(mockedText.promptJobNotification_JobCreationTime()).thenReturn(MOCKED_PROMPTJOBNOTIFICATION_JOBCREATIONTIME);
        when(mockedText.promptJobNotification_JobCompletionTime()).thenReturn(MOCKED_PROMPTJOBNOTIFICATION_JOBCOMPLETIONTIME);
        when(mockedText.promptJobNotification_Type()).thenReturn(MOCKED_PROMPTJOBNOTIFICATION_TYPE);
        when(mockedText.promptJobNotification_Status()).thenReturn(MOCKED_PROMPTJOBNOTIFICATION_STATUS);
        when(mockedText.promptJobNotification_StatusMessage()).thenReturn(MOCKED_PROMPTJOBNOTIFICATION_STATUSMESSAGE);
        when(mockedText.typeJobCompleted()).thenReturn(MOCKED_TYPEJOBCOMPLETED);
        when(mockedText.typeJobCreated()).thenReturn(MOCKED_TYPEJOBCREATED);
        when(mockedText.statusCompleted()).thenReturn(MOCKED_STATUSCOMPLETED);
        when(mockedText.statusFailed()).thenReturn(MOCKED_STATUSFAILED);
        when(mockedText.statusWaiting()).thenReturn(MOCKED_STATUSWAITING);
    }

    // Subject Under Test
    private PresenterImplConcrete presenterImpl;


    // Test specialization of Presenter to enable test of callback's
    class PresenterImplConcrete extends PresenterImpl {
        ItemsListView itemsListView;
        public JobsCallback getJobsCallback;
        public JobNotificationsCallback getJobNotificationCallback;

        public PresenterImplConcrete(Place place, PlaceController placeController, ItemsListView itemsListView) {
            super(place, placeController, mockedView, "");
            this.itemsListView = itemsListView;
            this.getJobsCallback = new JobsCallback();
            this.getJobNotificationCallback = new JobNotificationsCallback();
        }

        public PresenterImplConcrete(Place place, PlaceController placeController, String header) {
            super(place, placeController, mockedView, header);

        }

        @Override
        View getView() {
            return mockedView;
        }

        @Override
        Texts getTexts() {
            return mockedText;
        }
    }

    // Test Data
    private ItemModel testModelDelivering = new ItemModelBuilder().setItemNumber("11").setItemId("1001").setChunkId("1111").setJobId("1").setLifeCycle(ItemModel.LifeCycle.DELIVERING).setDiagnosticModels(Collections.singletonList(new DiagnosticModelBuilder().build())).build();
    private ItemModel testModelProcessing = new ItemModelBuilder().setItemNumber("14").setItemId("1004").setChunkId("1114").setJobId("1").setLifeCycle(ItemModel.LifeCycle.PROCESSING).setDiagnosticModels(Collections.singletonList(new DiagnosticModelBuilder().build())).build();
    private ItemModel testModelFatalError = new ItemModelBuilder().setHasDiagnosticFatal(true).setDiagnosticModels(Collections.singletonList(new DiagnosticModelBuilder().setLevel("FATAL").build())).build();
    private ItemModel testModelEmptyDiagn = new ItemModelBuilder().setDiagnosticModels(new ArrayList<DiagnosticModel>()).build();

    private JobModel testJobModelSucceeded = new JobModelBuilder()
            .setJobId("1418716277429")
            .setSubmitterNumber("150014")
            .setSubmitterName("SubmitterName1")
            .setFlowBinderName("FlowBinderName1")
            .setSinkId(5678L)
            .setSinkName("SinkName1")
            .setItemCounter(20)
            .setFailedCounter(0)
            .setIgnoredCounter(0)
            .setPartitionedCounter(11)
            .setProcessedCounter(12)
            .setDeliveredCounter(13)
            .setPackaging("packagingA")
            .setFormat("formatA")
            .setCharset("charsetA")
            .setDestination("destinationA")
            .setMailForNotificationAboutVerification("mailNotificationA")
            .setMailForNotificationAboutProcessing("mailProcessingA")
            .setResultMailInitials("resultMailInitialsA")
            .setJobCreationTime("2015-09-02 10:38:43")
            .setJobCompletionTime("2015-09-02 10:39:55")
            .setType(JobModel.Type.TRANSIENT)
            .build();

    private JobModel testJobModelFailed = new JobModelBuilder()
            .setJobId("1418716277429")
            .setSubmitterNumber("150014")
            .setSubmitterName("SubmitterName1")
            .setFlowBinderName("FlowBinderName1")
            .setSinkId(5678L)
            .setSinkName("SinkName1")
            .setItemCounter(20)
            .setFailedCounter(1)
            .setIgnoredCounter(0)
            .setPartitionedCounter(14)
            .setProcessedCounter(15)
            .setDeliveredCounter(16)
            .setPackaging("packagingA")
            .setFormat("formatA")
            .setCharset("charsetA")
            .setDestination("destinationA")
            .setMailForNotificationAboutVerification("mailNotificationA")
            .setMailForNotificationAboutProcessing("mailProcessingA")
            .setResultMailInitials("resultMailInitialsA")
            .setType(JobModel.Type.TEST)
            .build();

    private JobModel testJobModelIgnored = new JobModelBuilder()
            .setJobId("1418716277429")
            .setSubmitterNumber("150014")
            .setSubmitterName("SubmitterName1")
            .setFlowBinderName("FlowBinderName1")
            .setSinkId(5678L)
            .setSinkName("SinkName1")
            .setItemCounter(20)
            .setFailedCounter(0)
            .setIgnoredCounter(1)
            .setPartitionedCounter(17)
            .setProcessedCounter(18)
            .setDeliveredCounter(19)
            .setPackaging("packagingA")
            .setFormat("formatA")
            .setCharset("charsetA")
            .setDestination("destinationA")
            .setMailForNotificationAboutVerification("mailNotificationA")
            .setMailForNotificationAboutProcessing("mailProcessingA")
            .setResultMailInitials("resultMailInitialsA")
            .setType(JobModel.Type.PERSISTENT)
            .build();

    private JobModel testJobModelIgnored2 = new JobModelBuilder()
            .setJobId("1418716277429")
            .setJobCreationTime("2014-12-17 00:37:48")
            .setJobId("1418773068083")
            .setSubmitterNumber("424242")
            .setSubmitterName("SubmitterName2")
            .setFlowBinderName("FlowBinderName2")
            .setSinkId(5679L)
            .setSinkName("SinkName2")
            .setItemCounter(10)
            .setFailedCounter(0)
            .setIgnoredCounter(5)
            .setPartitionedCounter(20)
            .setProcessedCounter(21)
            .setDeliveredCounter(22)
            .setPackaging("packagingB")
            .setFormat("formatB")
            .setCharset("charsetB")
            .setDestination("destinationB")
            .setMailForNotificationAboutVerification("mailNotificationB")
            .setMailForNotificationAboutProcessing("mailProcessingB")
            .setResultMailInitials("resultMailInitialsB")
            .setType(JobModel.Type.ACCTEST)
            .build();

    private List<JobModel> testJobModelsEmpty = new ArrayList<>();
    private List<JobModel> testJobModelsSucceeded = new ArrayList<>(Collections.singletonList(testJobModelSucceeded));
    private List<JobModel> testJobModelsFailed = new ArrayList<>(Collections.singletonList(testJobModelFailed));
    private List<JobModel> testJobModelsIgnored = new ArrayList<>(Collections.singletonList(testJobModelIgnored));
    private List<JobModel> testJobModelsIgnored2 = Arrays.asList(testJobModelIgnored2, testJobModelSucceeded);

    private List<JobNotification> testJobNotificationsEmpty = new ArrayList<>();
    private JobNotification testJobNotificationCompleted = new JobNotificationBuilder().
            setId(123).
            setTimeOfCreation(new Date(12345886L)).
            setTimeOfLastModification(new Date(8457987548L)).
            setType(JobNotification.Type.JOB_COMPLETED).
            setStatus(JobNotification.Status.COMPLETED).
            setStatusMessage("Status Completed").
            setDestination("Destination Completed").
            setContent("Content Completed").
            setJobId(234).
            build();
    private JobNotification testJobNotificationFailed = new JobNotificationBuilder().
            setId(124).
            setTimeOfCreation(new Date(12345887L)).
            setTimeOfLastModification(new Date(8457987547L)).
            setType(JobNotification.Type.JOB_CREATED).
            setStatus(JobNotification.Status.FAILED).
            setStatusMessage("Status Failed").
            setDestination("Destination Failed").
            setContent("Content Failed").
            setJobId(235).
            build();
    private List<JobNotification> testJobNotifications = Arrays.asList(testJobNotificationCompleted, testJobNotificationFailed);


    // Tests start here

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {

        // Test Subject Under Test
        setupPresenterImpl();

        // Verify Test
        verify(mockedPlace).getJobId();
        verifyNoMoreInteractions(mockedPlace);
    }

    @Test
    public void start_callStart_ok() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";

        // Test Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verify Test
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedView).asWidget();
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
        verifyNoMoreInteractions(mockedContainerWidget);
        verify(mockedAllItemsTable).setRowCount(0);
        verifyNoMoreInteractions(mockedAllItemsTable);
        verify(mockedFailedItemsTable).setRowCount(0);
        verifyNoMoreInteractions(mockedFailedItemsTable);
        verify(mockedIgnoredItemsTable).setRowCount(0);
        verifyNoMoreInteractions(mockedIgnoredItemsTable);
        verify(mockedJobDiagnosticTable).setRowCount(0);
        verifyNoMoreInteractions(mockedJobDiagnosticTable);
        verify(mockedTabBar).getTab(ViewWidget.ALL_ITEMS_TAB_INDEX);
        verify(mockedTabBar).getTab(ViewWidget.FAILED_ITEMS_TAB_INDEX);
        verify(mockedTabBar).getTab(ViewWidget.IGNORED_ITEMS_TAB_INDEX);
        verify(mockedTabBar).getTab(ViewWidget.JOB_INFO_TAB_CONTENT);
        verify(mockedTabBar).getTab(ViewWidget.JOB_DIAGNOSTIC_TAB_CONTENT);
        verify(mockedTabBar).getTab(ViewWidget.JOB_NOTIFICATION_TAB_CONTENT);
        verifyNoMoreInteractions(mockedTabBar);
        verify(mockedJobStoreProxy).listJobs(any(JobListCriteria.class), any(PresenterImpl.JobsCallback.class));
        verify(mockedJobStoreProxy).listJobNotificationsForJob(any(Integer.class), any(PresenterImpl.JobNotificationsCallback.class));
        verifyNoMoreInteractions(mockedJobStoreProxy);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void allItemsTabSelected_callAllItemsTabSelected_allItemsRequested() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject under test
        presenterImpl.allItemsTabSelected();

        // Verify Test
        // Verifications from the start method (to be able to track no-more-interactions below)
        verify(mockedAllItemsTable).setRowCount(0);
        verify(mockedFailedItemsTable).setRowCount(0);
        verify(mockedIgnoredItemsTable).setRowCount(0);
        verify(mockedView.dataProvider).setBaseCriteria(eq(ItemListCriteria.Field.JOB_ID), eq(mockedAllItemsListView), any(ItemListCriteria.class));

        // Verifications from subject under test
        verify(mockedView).setSelectionEnabled(false);
        verify(mockedAllDetailedTabs).clear();
        verify(mockedAllDetailedTabs).setVisible(false);
        verifyNoMoreInteractions(mockedFailedItemsTable);
        verifyNoMoreInteractions(mockedIgnoredItemsTable);
        verifyNoMoreInteractions(mockedFailedDetailedTabs);
        verifyNoMoreInteractions(mockedIgnoredDetailedTabs);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void failedItemsTabSelected_callFailedItemsTabSelected_failedItemsRequested() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject under test
        presenterImpl.failedItemsTabSelected();

        // Verify Test
        // Verifications from the start method (to be able to track no-more-interactions below)
        verify(mockedAllItemsTable).setRowCount(0);
        verify(mockedFailedItemsTable).setRowCount(0);
        verify(mockedIgnoredItemsTable).setRowCount(0);
        verify(mockedJobDiagnosticTable).setRowCount(0);
        verify(mockedView.dataProvider).setBaseCriteria(eq(ItemListCriteria.Field.STATE_FAILED), eq(mockedFailedItemsListView), any(ItemListCriteria.class));

        // Verifications from subject under test
        verify(mockedView).setSelectionEnabled(false);
        verify(mockedFailedDetailedTabs).clear();
        verify(mockedFailedDetailedTabs).setVisible(false);
        verifyNoMoreInteractions(mockedAllItemsTable);
        verifyNoMoreInteractions(mockedIgnoredItemsTable);
        verifyNoMoreInteractions(mockedAllDetailedTabs);
        verifyNoMoreInteractions(mockedIgnoredDetailedTabs);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void ignoredItemsTabSelected_callIgnoredItemsTabSelected_ignoredItemsRequested() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject under test
        presenterImpl.ignoredItemsTabSelected();

        // Verify Test
        // Verifications from the start method (to be able to track no-more-interactions below)
        verify(mockedAllItemsTable).setRowCount(0);
        verify(mockedFailedItemsTable).setRowCount(0);
        verify(mockedIgnoredItemsTable).setRowCount(0);
        verify(mockedView.dataProvider).setBaseCriteria(eq(ItemListCriteria.Field.STATE_IGNORED), eq(mockedIgnoredItemsListView), any(ItemListCriteria.class));

        // Verifications from subject under test
        verify(mockedView).setSelectionEnabled(false);
        verify(mockedIgnoredDetailedTabs).clear();
        verify(mockedIgnoredDetailedTabs).setVisible(false);
        verifyNoMoreInteractions(mockedAllItemsTable);
        verifyNoMoreInteractions(mockedFailedItemsTable);
        verifyNoMoreInteractions(mockedAllDetailedTabs);
        verifyNoMoreInteractions(mockedFailedDetailedTabs);
    }

    @Test
    public void itemSelected_verifyMockedText_jobWithoutDiagnosticOk() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.type = JobModel.Type.TRANSIENT;
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject under test
        presenterImpl.itemSelected(mockedAllItemsListView, testModelEmptyDiagn);

        // Verify Test
        verify(mockedText).tab_JavascriptLog();
        verify(mockedText).tab_PartitioningPost();
        verify(mockedText).tab_ProcessingPost();
        verify(mockedText).tab_DeliveringPost();
        verifyNoMoreInteractions(mockedText);
    }

    @Test
    public void itemSelected_verifyMockedText_jobWithWarningDiagnosticOk() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.type = JobModel.Type.TRANSIENT;
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject under test
        presenterImpl.itemSelected(mockedAllItemsListView, testModelDelivering);

        // Verify Test
        verify(mockedText).tab_JavascriptLog();
        verify(mockedText).tab_PartitioningPost();
        verify(mockedText).tab_ProcessingPost();
        verify(mockedText).tab_DeliveringPost();
        verify(mockedText).tab_ItemDiagnostic();
        verifyNoMoreInteractions(mockedText);
    }

    @Test
    public void itemSelected_verifyMockedText_jobWithFatalDiagnosticOk() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.type = JobModel.Type.TRANSIENT;
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject under test
        presenterImpl.itemSelected(mockedAllItemsListView, testModelFatalError);

        // Verify Test
        verify(mockedText).tab_ItemDiagnostic();

        verify(mockedText, times(0)).tab_JavascriptLog();
        verify(mockedText, times(0)).tab_PartitioningPost();
        verify(mockedText, times(0)).tab_ProcessingPost();
        verify(mockedText, times(0)).tab_NextOutputPost();
        verify(mockedText, times(0)).tab_DeliveringPost();
    }

    @Test
    public void itemSelected_verifyMockedText_acceptanceTestJobWithoutDiagnosticOk() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.type = JobModel.Type.ACCTEST;
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject under test
        presenterImpl.itemSelected(mockedAllItemsListView, testModelEmptyDiagn);

        // Verify Test
        verify(mockedText).tab_JavascriptLog();
        verify(mockedText).tab_PartitioningPost();
        verify(mockedText).tab_ProcessingPost();
        verify(mockedText).tab_DeliveringPost();
        verify(mockedText).tab_NextOutputPost();

        verify(mockedText, times(0)).tab_ItemDiagnostic();
    }

    @Test
    public void itemSelected_verifyMockedText_acceptanceTestJobWithWarningDiagnosticOk() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.type = JobModel.Type.ACCTEST;
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject under test
        presenterImpl.itemSelected(mockedAllItemsListView, testModelDelivering);

        // Verify Test
        verify(mockedText).tab_JavascriptLog();
        verify(mockedText).tab_PartitioningPost();
        verify(mockedText).tab_ProcessingPost();
        verify(mockedText).tab_DeliveringPost();
        verify(mockedText).tab_NextOutputPost();
        verify(mockedText).tab_ItemDiagnostic();
    }

    @Test
    public void itemSelected_verifyMockedText_acceptanceTestJobWithFatalDiagnosticOk() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.type = JobModel.Type.ACCTEST;
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject under test
        presenterImpl.itemSelected(mockedAllItemsListView, testModelFatalError);

        // Verify Test
        verify(mockedText).tab_ItemDiagnostic();

        verify(mockedText, times(0)).tab_JavascriptLog();
        verify(mockedText, times(0)).tab_PartitioningPost();
        verify(mockedText, times(0)).tab_ProcessingPost();
        verify(mockedText, times(0)).tab_NextOutputPost();
        verify(mockedText, times(0)).tab_DeliveringPost();
    }


    @Test
    public void itemSelected_callItemSelected_ok() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        presenterImpl.itemSearchType = ItemListCriteria.Field.JOB_ID;
        presenterImpl.type = JobModel.Type.PERSISTENT;

        // Subject under test
        presenterImpl.itemSelected(mockedAllItemsListView, testModelDelivering);

        // Verify Test
        // Default tab index for jobs is: javascript log
        genericMockedAllDetailedTabsAssert(false, true, false, 0);
    }

    @Test
    public void itemSelected_itemFailedWithFatalDiagnostic_callItemSelected_ok() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.itemSearchType = ItemListCriteria.Field.STATE_FAILED;
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        presenterImpl.type = JobModel.Type.TRANSIENT;

        // Subject under test
        presenterImpl.itemSelected(mockedAllItemsListView, testModelFatalError);

        // Verify Test
        // Expected tab index for jobs that have fatal diagnostics is: item diagnostic
        genericMockedAllDetailedTabsAssert(true, true, false, 0);
    }

    @Test
    public void itemSelected_itemFailedWithZeroDiagnostics_callItemSelected_ok() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.itemSearchType = ItemListCriteria.Field.STATE_FAILED;
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        presenterImpl.type = JobModel.Type.TRANSIENT;

        // Subject under test
        presenterImpl.itemSelected(mockedAllItemsListView, testModelEmptyDiagn);

        // Verify Test
        // Expected tab index for jobs that have zero diagnostics is: javascript log
        genericMockedAllDetailedTabsAssert(false, false, false, 0);
    }

    @Test
    public void itemSelected_itemFailedInDelivering_callItemSelected_ok() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.itemSearchType = ItemListCriteria.Field.STATE_FAILED;
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        presenterImpl.type = JobModel.Type.TRANSIENT;

        // Subject under test
        presenterImpl.itemSelected(mockedAllItemsListView, testModelDelivering);

        // Verify Test
        // Expected tab index for jobs that are failed in delivering is: sink result
        genericMockedAllDetailedTabsAssert(false, true, false, 3);
    }

    @Test
    public void itemSelected_itemFailedInProcessing_callItemSelected_ok() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.itemSearchType = ItemListCriteria.Field.STATE_FAILED;
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        presenterImpl.type = JobModel.Type.TRANSIENT;

        // Subject under test
        presenterImpl.itemSelected(mockedAllItemsListView, testModelProcessing);

        // Verify Test
        // Expected tab index for jobs that are failed in processing is: output post
        genericMockedAllDetailedTabsAssert(false, true, false, 2);
    }

    @Test
    public void itemSelected_itemIgnoredInProcessing_callItemSelected_ok() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.itemSearchType = ItemListCriteria.Field.STATE_IGNORED;
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        presenterImpl.type = JobModel.Type.TRANSIENT;

        // Subject under test
        presenterImpl.itemSelected(mockedAllItemsListView, testModelProcessing);

        // Verify Test
        // Expected tab index for jobs that are ignored in processing is: output post
        genericMockedAllDetailedTabsAssert(false, true, false, 2);
    }

    @Test
    public void itemSelected_itemIgnoredInDelivering_callItemSelected_ok() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.itemSearchType = ItemListCriteria.Field.STATE_IGNORED;
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        presenterImpl.type = JobModel.Type.TRANSIENT;

        // Subject under test
        presenterImpl.itemSelected(mockedAllItemsListView, testModelDelivering);

        // Verify Test
        // Expected tab index for jobs that are ignored in delivering is: output post
        genericMockedAllDetailedTabsAssert(false, true, false, 2);
    }

    @Test
    public void itemSelected_itemFailedWithZeroDiagnosticsForAcceptanceTestJob_callItemSelected_ok() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.itemSearchType = ItemListCriteria.Field.STATE_FAILED;
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        presenterImpl.type = JobModel.Type.ACCTEST;

        // Subject under test
        presenterImpl.itemSelected(mockedAllItemsListView, testModelEmptyDiagn);

        // Verify Test
        // Expected tab index for acceptance-test jobs that have zero diagnostics is: sink result
        genericMockedAllDetailedTabsAssert(false, false, true, 4);
    }

    @Test
    public void itemSelected_callItemSelectedForAcceptanceTestJob_ok() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        presenterImpl.itemSearchType = ItemListCriteria.Field.JOB_ID;
        presenterImpl.type = JobModel.Type.ACCTEST;

        // Subject under test
        presenterImpl.itemSelected(mockedAllItemsListView, testModelDelivering);

        // Verify Test
        // Default tab index for acceptance-test jobs is: sink result
        genericMockedAllDetailedTabsAssert(false, true, true, 4);
    }

    @Test
    public void itemSelected_itemIgnoredInDeliveringForAcceptanceTestJob_callItemSelected_ok() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.itemSearchType = ItemListCriteria.Field.STATE_IGNORED;
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        presenterImpl.type = JobModel.Type.ACCTEST;

        // Subject under test
        presenterImpl.itemSelected(mockedAllItemsListView, testModelDelivering);

        // Verify Test
        // Expected tab index for acceptance-test jobs that are ignored in delivering is: sink result
        genericMockedAllDetailedTabsAssert(false, true, true, 4);
    }

    @Test
    public void itemSelected_itemIgnoredInProcessingForAcceptanceTestJob_callItemSelected_ok() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.itemSearchType = ItemListCriteria.Field.STATE_IGNORED;
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        presenterImpl.type = JobModel.Type.ACCTEST;

        // Subject under test
        presenterImpl.itemSelected(mockedAllItemsListView, testModelProcessing);

        // Verify Test
        // Expected tab index for acceptance-test jobs that are ignored in processing is: sink result
        genericMockedAllDetailedTabsAssert(false, true, true, 4);
    }

    @Test
    public void itemSelected_itemFailedInProcessingForAcceptanceTestJob_callItemSelected_ok() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.itemSearchType = ItemListCriteria.Field.STATE_FAILED;
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        presenterImpl.type = JobModel.Type.ACCTEST;

        // Subject under test
        presenterImpl.itemSelected(mockedAllItemsListView, testModelProcessing);

        // Verify Test
        // Expected tab index for acceptance-test jobs that are failed in processing is: sink result
        genericMockedAllDetailedTabsAssert(false, true, true, 4);
    }

    @Test
    public void itemSelected_itemFailedInDeliveringForAcceptanceTestJob_callItemSelected_ok() {
        setupPresenterImpl();
        presenterImpl.jobId = "1234";
        presenterImpl.itemSearchType = ItemListCriteria.Field.STATE_FAILED;
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        presenterImpl.type = JobModel.Type.ACCTEST;

        // Subject under test
        presenterImpl.itemSelected(mockedAllItemsListView, testModelDelivering);

        // Verify Test
        // Expected tab index for acceptance-test jobs that are failed in delivering is: sink result
        genericMockedAllDetailedTabsAssert(false, true, true, 4);
    }

    private void genericMockedAllDetailedTabsAssert(boolean isFatal, boolean hasDiagnostics, boolean isAccTest, int selectedTabIndex) {
        verify(mockedAllDetailedTabs).clear();
        if(isFatal) {
            verify(mockedAllDetailedTabs).add(any(ItemDiagnosticTabContent.class), eq(MOCKED_TAB_ITEM_DIAGNOSTIC));
            verify(mockedAllDetailedTabs, times(0)).add(any(JavascriptLogTabContent.class), eq(MOCKED_TAB_JAVASCRIPTLOG));
            verify(mockedAllDetailedTabs, times(0)).add(any(ItemTabContent.class), eq(MOCKED_TAB_PARTITIONINGPOST));
            verify(mockedAllDetailedTabs, times(0)).add(any(ItemTabContent.class), eq(MOCKED_TAB_PROCESSINGPOST));
            verify(mockedAllDetailedTabs, times(0)).add(any(NextTabContent.class), eq(MOCKED_TAB_NEXT_OUTPUTPOST));
            verify(mockedAllDetailedTabs, times(0)).add(any(ItemTabContent.class), eq(MOCKED_TAB_DELIVERINGPOST));

        } else {
            verify(mockedAllDetailedTabs).add(any(JavascriptLogTabContent.class), eq(MOCKED_TAB_JAVASCRIPTLOG));
            verify(mockedAllDetailedTabs).add(any(ItemTabContent.class), eq(MOCKED_TAB_PARTITIONINGPOST));
            verify(mockedAllDetailedTabs).add(any(ItemTabContent.class), eq(MOCKED_TAB_PROCESSINGPOST));
            if(isAccTest) {
                verify(mockedAllDetailedTabs).add(any(NextTabContent.class), eq(MOCKED_TAB_NEXT_OUTPUTPOST));
            } else {
                verify(mockedAllDetailedTabs, times(0)).add(any(NextTabContent.class), eq(MOCKED_TAB_NEXT_OUTPUTPOST));
            }
            verify(mockedAllDetailedTabs).add(any(ItemTabContent.class), eq(MOCKED_TAB_DELIVERINGPOST));
            if(hasDiagnostics){
                verify(mockedAllDetailedTabs).add(any(ItemDiagnosticTabContent.class), eq(MOCKED_TAB_ITEM_DIAGNOSTIC));
            } else {
                verify(mockedAllDetailedTabs, times(0)).add(any(ItemDiagnosticTabContent.class), eq(MOCKED_TAB_ITEM_DIAGNOSTIC));
            }
        }
        verify(mockedAllDetailedTabs).selectTab(selectedTabIndex);
        verify(mockedAllDetailedTabs).setVisible(true);
    }

    // Test JobsCallback
    @Test
    public void getJob_callbackWithError_errorMessageInView() {
        setupPresenterImplConcrete();
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobsCallback.onFailure(mockedException);

        // Verify Test
        verify(mockedView).setErrorText(MOCKED_ERROR_COULDNOTFETCHJOB);
    }

    @Test
    public void getJob_callbackWithSuccessAndFailedJobs_jobFetchedCorrectly() {
        setupPresenterImplConcrete();
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobsCallback.onSuccess(testJobModelsFailed);

        // Verify Test
        verify(mockedView.jobHeader).setText("Mocked Job Id: 1418716277429, Mocked Submitter: 150014, Mocked Sink: SinkName1");
        verify(mockedTabPanel).selectTab(ViewWidget.FAILED_ITEMS_TAB_INDEX);
        verify(mockedPackaging).setText("packagingA");
        verify(mockedFormat).setText("formatA");
        verify(mockedCharset).setText("charsetA");
        verify(mockedDestination).setText("destinationA");
        verify(mockedMailForNotificationAboutVerification).setText("mailNotificationA");
        verify(mockedMailForNotificationAboutProcessing).setText("mailProcessingA");
        verify(mockedResultMailInitials).setText("resultMailInitialsA");
        verify(mockedJobCreationTime).setText("2015-08-13 14:56:11");
        verify(mockedJobCompletionTime).setText(EMPTY);
        verify(mockedType).setText(JobSpecification.Type.TEST.name());
        verify(mockedTabBar, times(2)).getTab(ViewWidget.ALL_ITEMS_TAB_INDEX);
        verify(mockedTabBar, times(2)).getTab(ViewWidget.FAILED_ITEMS_TAB_INDEX);
        verify(mockedTabBar, times(2)).getTab(ViewWidget.JOB_INFO_TAB_CONTENT);
        verify(mockedTabBar, times(1)).getTab(ViewWidget.IGNORED_ITEMS_TAB_INDEX);
        verify(mockedTabBar, times(1)).getTab(ViewWidget.JOB_DIAGNOSTIC_TAB_CONTENT);
        verify(mockedTabBar, times(1)).getTab(ViewWidget.JOB_NOTIFICATION_TAB_CONTENT);
        verifyNoMoreInteractions(mockedView.jobHeader);
        verifyNoMoreInteractionsForJobInfoFields();
    }

    @Test
    public void getJob_callbackWithSuccessAndIgnoredJobs_jobFetchedCorrectly() {
        setupPresenterImplConcrete();
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobsCallback.onSuccess(testJobModelsIgnored);

        // Verify Test
        verify(mockedView.jobHeader).setText("Mocked Job Id: 1418716277429, Mocked Submitter: 150014, Mocked Sink: SinkName1");
        verify(mockedTabPanel).selectTab(ViewWidget.IGNORED_ITEMS_TAB_INDEX);
        verify(mockedPackaging).setText("packagingA");
        verify(mockedFormat).setText("formatA");
        verify(mockedCharset).setText("charsetA");
        verify(mockedDestination).setText("destinationA");
        verify(mockedMailForNotificationAboutVerification).setText("mailNotificationA");
        verify(mockedMailForNotificationAboutProcessing).setText("mailProcessingA");
        verify(mockedResultMailInitials).setText("resultMailInitialsA");
        verify(mockedJobCreationTime).setText("2015-08-13 14:56:11");
        verify(mockedJobCompletionTime).setText(EMPTY);
        verify(mockedType).setText(JobModel.Type.PERSISTENT.name());
        verify(mockedTabBar, times(2)).getTab(ViewWidget.ALL_ITEMS_TAB_INDEX);
        verify(mockedTabBar, times(2)).getTab(ViewWidget.IGNORED_ITEMS_TAB_INDEX);
        verify(mockedTabBar, times(2)).getTab(ViewWidget.JOB_INFO_TAB_CONTENT);
        verify(mockedTabBar, times(1)).getTab(ViewWidget.FAILED_ITEMS_TAB_INDEX);
        verify(mockedTabBar, times(1)).getTab(ViewWidget.JOB_DIAGNOSTIC_TAB_CONTENT);
        verify(mockedTabBar, times(1)).getTab(ViewWidget.JOB_NOTIFICATION_TAB_CONTENT);
        verifyNoMoreInteractions(mockedView.jobHeader);
        verifyNoMoreInteractionsForJobInfoFields();
    }

    @Test
    public void getJob_callbackWithSuccessAndSuccessfulJobs_jobFetchedCorrectly() {
        setupPresenterImplConcrete();
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobsCallback.onSuccess(testJobModelsSucceeded);

        // Verify Test
        verify(mockedView.jobHeader).setText("Mocked Job Id: 1418716277429, Mocked Submitter: 150014, Mocked Sink: SinkName1");
        verify(mockedTabPanel).selectTab(ViewWidget.ALL_ITEMS_TAB_INDEX);
        verify(mockedPackaging).setText("packagingA");
        verify(mockedFormat).setText("formatA");
        verify(mockedCharset).setText("charsetA");
        verify(mockedDestination).setText("destinationA");
        verify(mockedMailForNotificationAboutVerification).setText("mailNotificationA");
        verify(mockedMailForNotificationAboutProcessing).setText("mailProcessingA");
        verify(mockedResultMailInitials).setText("resultMailInitialsA");
        verify(mockedJobCreationTime).setText("2015-09-02 10:38:43");
        verify(mockedJobCompletionTime).setText("2015-09-02 10:39:55");
        verify(mockedType).setText(JobModel.Type.TRANSIENT.name());
        verify(mockedTabBar, times(2)).getTab(ViewWidget.ALL_ITEMS_TAB_INDEX);
        verify(mockedTabBar, times(2)).getTab(ViewWidget.JOB_INFO_TAB_CONTENT);
        verify(mockedTabBar, times(1)).getTab(ViewWidget.IGNORED_ITEMS_TAB_INDEX);
        verify(mockedTabBar, times(1)).getTab(ViewWidget.FAILED_ITEMS_TAB_INDEX);
        verify(mockedTabBar, times(1)).getTab(ViewWidget.JOB_DIAGNOSTIC_TAB_CONTENT);
        verify(mockedTabBar, times(1)).getTab(ViewWidget.JOB_NOTIFICATION_TAB_CONTENT);
        verifyNoMoreInteractions(mockedView.jobHeader);
        verifyNoMoreInteractionsForJobInfoFields();
    }

    @Test
    public void getJob_callbackWithSuccessAndMultipleJobs_firstJobFetchedCorrectly() {
        setupPresenterImplConcrete();
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobsCallback.onSuccess(testJobModelsIgnored2);

        // Verify Test
        verify(mockedView.jobHeader).setText("Mocked Job Id: 1418773068083, Mocked Submitter: 424242, Mocked Sink: SinkName2");
        verify(mockedTabPanel).selectTab(ViewWidget.IGNORED_ITEMS_TAB_INDEX);
        verify(mockedPackaging).setText("packagingB");
        verify(mockedFormat).setText("formatB");
        verify(mockedCharset).setText("charsetB");
        verify(mockedDestination).setText("destinationB");
        verify(mockedMailForNotificationAboutVerification).setText("mailNotificationB");
        verify(mockedMailForNotificationAboutProcessing).setText("mailProcessingB");
        verify(mockedResultMailInitials).setText("resultMailInitialsB");
        verify(mockedJobCreationTime).setText("2014-12-17 00:37:48");
        verify(mockedJobCompletionTime).setText(EMPTY);
        verify(mockedType).setText(JobModel.Type.ACCTEST.name());
        verify(mockedTabBar, times(2)).getTab(ViewWidget.ALL_ITEMS_TAB_INDEX);
        verify(mockedTabBar, times(2)).getTab(ViewWidget.JOB_INFO_TAB_CONTENT);
        verify(mockedTabBar, times(2)).getTab(ViewWidget.IGNORED_ITEMS_TAB_INDEX);
        verify(mockedTabBar, times(1)).getTab(ViewWidget.FAILED_ITEMS_TAB_INDEX);
        verify(mockedTabBar, times(1)).getTab(ViewWidget.JOB_DIAGNOSTIC_TAB_CONTENT);
        verify(mockedTabBar, times(1)).getTab(ViewWidget.JOB_NOTIFICATION_TAB_CONTENT);
        verifyNoMoreInteractions(mockedView.jobHeader);
        verifyNoMoreInteractionsForJobInfoFields();
    }

    @Test
    public void getJob_callbackWithSuccessAndNoJobs_noJobFetched() {
        setupPresenterImplConcrete();
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobsCallback.onSuccess(testJobModelsEmpty);

        // Verify Test
        verify(mockedTabBar).getTab(ViewWidget.ALL_ITEMS_TAB_INDEX);
        verify(mockedTabBar).getTab(ViewWidget.FAILED_ITEMS_TAB_INDEX);
        verify(mockedTabBar).getTab(ViewWidget.IGNORED_ITEMS_TAB_INDEX);
        verify(mockedTabBar, times(2)).getTab(ViewWidget.JOB_INFO_TAB_CONTENT);
        verify(mockedTabBar).getTab(ViewWidget.JOB_DIAGNOSTIC_TAB_CONTENT);
        verify(mockedView.jobHeader).setText(buildHeaderText(presenterImpl.jobId, "", ""));
        verify(mockedView).setErrorText(MOCKED_ERROR_COULDNOTFINDJOB);
        verifyNoMoreInteractionsForJobInfoFields();
    }

    @Test
    public void getJob_callbackWithSuccessAndNullJobsList_noJobFetched() {
        setupPresenterImplConcrete();
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobsCallback.onSuccess(null);

        // Verify Test
        verify(mockedTabBar).getTab(ViewWidget.ALL_ITEMS_TAB_INDEX);
        verify(mockedTabBar).getTab(ViewWidget.FAILED_ITEMS_TAB_INDEX);
        verify(mockedTabBar).getTab(ViewWidget.IGNORED_ITEMS_TAB_INDEX);
        verify(mockedTabBar, times(2)).getTab(ViewWidget.JOB_INFO_TAB_CONTENT);
        verify(mockedTabBar).getTab(ViewWidget.JOB_DIAGNOSTIC_TAB_CONTENT);
        verify(mockedView.jobHeader).setText(buildHeaderText(presenterImpl.jobId, "", ""));
        verify(mockedView).setErrorText(MOCKED_ERROR_COULDNOTFINDJOB);
        verifyNoMoreInteractionsForJobInfoFields();
    }


    // Test JobNotificationCallback
    @Test
    public void getJobNotifications_callbackWithError_errorMessageInView() {
        setupPresenterImplConcrete();
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobNotificationCallback.onFailure(mockedException);

        // Verify Test
        verify(mockedView).setErrorText(MOCKED_ERROR_COULDNOTFETCHJOBNOTIFICATIONS);
    }

    @Test
    public void getJobNotifications_callbackWithSuccessAndNoNotifications_noJobNotificationsFetchedCorrectly() {
        setupPresenterImplConcrete();
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobNotificationCallback.onSuccess(testJobNotificationsEmpty);

        // Verify Test
        verify(mockedView).setPresenter(any(Presenter.class));
        verify(mockedView).asWidget();
//        verifyNoMoreInteractions(mockedView);
        verify(mockedJobNotificationTabContent, times(1)).clear();
        verify(mockedJobNotificationTabContent, times(1)).getNotificationsCount();
        verifyNoMoreInteractions(mockedJobNotificationTabContent);
    }

    @Test
    public void getJobNotifications_callbackWithSuccessAndTwoNotifications_twoJobNotificationsFetchedCorrectly() {
        setupPresenterImplConcrete();
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobNotificationCallback.onSuccess(testJobNotifications);

        // Verify Test
        verify(mockedView).setPresenter(any(Presenter.class));
        verify(mockedView).asWidget();
//        verifyNoMoreInteractions(mockedView);
        verify(mockedJobNotificationTabContent, times(1)).clear();
        verify(mockedJobNotificationTabContent, times(2)).add(any(JobNotificationPanel.class));
        verify(mockedJobNotificationTabContent, times(1)).getNotificationsCount();
        verifyNoMoreInteractions(mockedJobNotificationTabContent);
    }

    //Private methods

    private String buildHeaderText(String jobId, String submitterNumber, String sinkName) {
        return MOCKED_TEXT_JOBID + " " + jobId + ", "
                + MOCKED_TEXT_SUBMITTER + " " + submitterNumber + ", "
                + MOCKED_TEXT_SINK + " " + sinkName;
    }

    private void verifyNoMoreInteractionsForJobInfoFields() {
        verifyNoMoreInteractions(mockedPackaging);
        verifyNoMoreInteractions(mockedFormat);
        verifyNoMoreInteractions(mockedCharset);
        verifyNoMoreInteractions(mockedDestination);
        verifyNoMoreInteractions(mockedMailForNotificationAboutVerification);
        verifyNoMoreInteractions(mockedMailForNotificationAboutProcessing);
        verifyNoMoreInteractions(mockedResultMailInitials);
        verifyNoMoreInteractions(mockedType);
        verifyNoMoreInteractions(mockedJobCreationTime);
        verifyNoMoreInteractions(mockedJobCompletionTime);
    }

    private void setupPresenterImpl() {
        presenterImpl = new PresenterImplConcrete(mockedPlace, mockedPlaceController, header);
        presenterImpl.viewInjector = mockedViewInjector;
        presenterImpl.commonInjector = mockedCommonGinjector;
    }

    private void setupPresenterImplConcrete() {
        presenterImpl = new PresenterImplConcrete(mockedPlace, mockedPlaceController, mockedAllItemsListView);
        presenterImpl.viewInjector = mockedViewInjector;
        presenterImpl.commonInjector = mockedCommonGinjector;
    }
}