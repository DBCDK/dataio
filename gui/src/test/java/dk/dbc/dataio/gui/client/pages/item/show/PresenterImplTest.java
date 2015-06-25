
package dk.dbc.dataio.gui.client.pages.item.show;


import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwtmockito.GwtMockitoTestRunner;
import dk.dbc.dataio.gui.client.components.PromptedLabel;
import dk.dbc.dataio.gui.client.model.DiagnosticModel;
import dk.dbc.dataio.gui.client.model.ItemListCriteriaModel;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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
public class PresenterImplTest {
    @Mock ClientFactory mockedClientFactory;
    @Mock JobStoreProxyAsync mockedJobStoreProxy;
    @Mock AcceptsOneWidget mockedContainerWidget;
    @Mock EventBus mockedEventBus;
    @Mock View mockedView;
    @Mock ItemsListView mockedAllItemsListView;
    @Mock ItemsListView mockedFailedItemsListView;
    @Mock ItemsListView mockedIgnoredItemsListView;
    @Mock Widget mockedViewWidget;
    @Mock Throwable mockedException;
    @Mock Place mockedPlace;
    @Mock LogStoreProxyAsync mockedLogStoreProxy;

    @Mock Label mockedJobHeader;
    @Mock CellTable mockedAllItemsTable;
    @Mock CellTable mockedFailedItemsTable;
    @Mock CellTable mockedIgnoredItemsTable;
    @Mock CellTable mockedJobDiagnosticTable;
    @Mock SimplePager mockedAllPager;
    @Mock SimplePager mockedFailedPager;
    @Mock SimplePager mockedIgnoredPager;
    @Mock DecoratedTabPanel mockedAllDetailedTabs;
    @Mock DecoratedTabPanel mockedFailedDetailedTabs;
    @Mock DecoratedTabPanel mockedIgnoredDetailedTabs;
    @Mock DecoratedTabPanel mockedTabPanel;
    @Mock SingleSelectionModel<ItemModel> mockedSelectionModel;
    @Mock JobInfoTabContent mockedJobInfoTabContent;
    @Mock JobDiagnosticTabContent mockedJobDiagnosticTabContent;
    @Mock PromptedLabel mockedPackaging;
    @Mock PromptedLabel mockedFormat;
    @Mock PromptedLabel mockedCharset;
    @Mock PromptedLabel mockedDestination;
    @Mock PromptedLabel mockedMailForNotificationAboutVerification;
    @Mock PromptedLabel mockedMailForNotificationAboutProcessing;
    @Mock PromptedLabel mockedResultMailInitials;

    private final static int OFFSET = 0;
    private final static int ROW_COUNT = 4;


    // Setup mocked data
    @Before
    public void setupMockedData() {
        when(mockedClientFactory.getJobStoreProxyAsync()).thenReturn(mockedJobStoreProxy);
        when(mockedClientFactory.getItemsShowView()).thenReturn(mockedView);
        when(mockedClientFactory.getLogStoreProxyAsync()).thenReturn(mockedLogStoreProxy);
        mockedView.jobHeader = mockedJobHeader;
        mockedAllItemsListView.itemsTable = mockedAllItemsTable;
        mockedFailedItemsListView.itemsTable = mockedFailedItemsTable;
        mockedView.jobDiagnosticTabContent = mockedJobDiagnosticTabContent;
        mockedIgnoredItemsListView.itemsTable = mockedIgnoredItemsTable;
        mockedView.jobDiagnosticTabContent.jobDiagnosticTable = mockedJobDiagnosticTable;
        mockedAllItemsListView.itemsPager = mockedAllPager;
        mockedFailedItemsListView.itemsPager = mockedFailedPager;
        mockedIgnoredItemsListView.itemsPager = mockedIgnoredPager;
        mockedAllItemsListView.detailedTabs = mockedAllDetailedTabs;
        mockedFailedItemsListView.detailedTabs = mockedFailedDetailedTabs;
        mockedIgnoredItemsListView.detailedTabs = mockedIgnoredDetailedTabs;
        mockedView.allItemsList = mockedAllItemsListView;
        mockedView.failedItemsList = mockedFailedItemsListView;
        mockedView.ignoredItemsList = mockedIgnoredItemsListView;
        mockedView.tabPanel = mockedTabPanel;
        when(mockedView.asWidget()).thenReturn(mockedViewWidget);
        when(mockedView.allItemsList.itemsTable.getVisibleRange()).thenReturn(new Range(OFFSET, ROW_COUNT));
        when(mockedView.failedItemsList.itemsTable.getVisibleRange()).thenReturn(new Range(OFFSET, ROW_COUNT));
        when(mockedView.ignoredItemsList.itemsTable.getVisibleRange()).thenReturn(new Range(OFFSET, ROW_COUNT));
        when(mockedView.allItemsList.itemsTable.getSelectionModel()).thenReturn(mockedSelectionModel);
        when(mockedView.failedItemsList.itemsTable.getSelectionModel()).thenReturn(mockedSelectionModel);
        when(mockedView.ignoredItemsList.itemsTable.getSelectionModel()).thenReturn(mockedSelectionModel);
        mockedView.jobInfoTabContent = mockedJobInfoTabContent;
        mockedView.jobInfoTabContent.packaging = mockedPackaging;
        mockedView.jobInfoTabContent.format = mockedFormat;
        mockedView.jobInfoTabContent.charset = mockedCharset;
        mockedView.jobInfoTabContent.destination = mockedDestination;
        mockedView.jobInfoTabContent.mailForNotificationAboutVerification = mockedMailForNotificationAboutVerification;
        mockedView.jobInfoTabContent.mailForNotificationAboutProcessing = mockedMailForNotificationAboutProcessing;
        mockedView.jobInfoTabContent.resultMailInitials = mockedResultMailInitials;
    }

    // Mocked Texts
    @Mock static Texts mockedText;
    final static String MOCKED_MENU_ITEMS = "Mocked Poster";
    final static String MOCKED_COLUMN_ITEM = "Mocked Post";
    final static String MOCKED_COLUMN_STATUS = "Mocked Status";
    final static String MOCKED_ERROR_COULDNOTFETCHITEMS = "Mocked Det var ikke muligt at hente poster fra Job Store";
    final static String MOCKED_ERROR_COULDNOTFETCHJOB = "Mocked Det var ikke muligt at hente jobbet fra Job Store";
    final static String MOCKED_ERROR_CANNOTNOTFETCHJAVASCRIPTLOG = "Mocked Det var ikke muligt at hente java script loggen";
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
    final static String MOCKED_TAB_JAVASCRIPTLOG = "Mocked Javascript log";
    final static String MOCKED_TAB_INPUTPOST = "Mocked input post log";
    @Before
    public void setupMockedTextsBehaviour() {
        when(mockedClientFactory.getItemsShowTexts()).thenReturn(mockedText);
        when(mockedText.menu_Items()).thenReturn(MOCKED_MENU_ITEMS);
        when(mockedText.column_Item()).thenReturn(MOCKED_COLUMN_ITEM);
        when(mockedText.column_Status()).thenReturn(MOCKED_COLUMN_STATUS);
        when(mockedText.error_CouldNotFetchItems()).thenReturn(MOCKED_ERROR_COULDNOTFETCHITEMS);
        when(mockedText.error_CouldNotFetchJob()).thenReturn(MOCKED_ERROR_COULDNOTFETCHJOB);
        when(mockedText.error_CannotFetchJavaScriptLog()).thenReturn(MOCKED_ERROR_CANNOTNOTFETCHJAVASCRIPTLOG);
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
        when(mockedText.tab_JavascriptLog()).thenReturn(MOCKED_TAB_JAVASCRIPTLOG);
        when(mockedText.tab_InputPost()).thenReturn(MOCKED_TAB_INPUTPOST);
    }

    // Subject Under Test
    private PresenterImpl presenterImpl;


    // Test specialization of Presenter to enable test of callback's
    class PresenterImplConcrete extends PresenterImpl {
        ItemsListView itemsListView;
        public ItemsCallback getItemsCallback;
        public JobCallback getJobCallback;

        public PresenterImplConcrete(Place place, ClientFactory clientFactory, ItemsListView itemsListView) {
            super(place, clientFactory);
            this.itemsListView = itemsListView;
            this.getItemsCallback = new ItemsCallback(itemsListView, ROW_COUNT, OFFSET);
            this.getJobCallback = new JobCallback();
        }
    }


    // Test Data
    private ItemModel testModel1 = new ItemModel("11", "1001", "1111", "1", ItemModel.LifeCycle.DELIVERING);
    private ItemModel testModel2 = new ItemModel("12", "ItemId2", "ChunkId2", "JobId2", ItemModel.LifeCycle.DONE);
    private ItemModel testModel3 = new ItemModel("13", "ItemId3", "ChunkId3", "JobId3", ItemModel.LifeCycle.PARTITIONING);
    private ItemModel testModel4 = new ItemModel("14", "ItemId4", "ChunkId4", "JobId4", ItemModel.LifeCycle.PROCESSING);
    private List<ItemModel> testModels = Arrays.asList(testModel1, testModel2, testModel3, testModel4);
    private JobModel testJobModelSucceeded = new JobModel("2014-12-16 08:51:17", "1418716277429",
            "150014", "SubmitterName1",
            "FlowBinderName1", 5678L, "SinkName1",
            true, 20, 20, 0, 0, 11, 12, 13, new ArrayList<DiagnosticModel>(),
            "packagingA", "formatA", "charsetA", "destinationA", "mailNotificationA", "mailProcessingA", "resultMailInitialsA");
    private JobModel testJobModelFailed = new JobModel("2014-12-16 08:51:17", "1418716277429",
            "150014", "SubmitterName1",
            "FlowBinderName1", 5678L, "SinkName1",
            true, 20, 20, 1, 0, 14, 15, 16, new ArrayList<DiagnosticModel>(),
            "packagingA", "formatA", "charsetA", "destinationA", "mailNotificationA", "mailProcessingA", "resultMailInitialsA");
    private JobModel testJobModelIgnored = new JobModel("2014-12-16 08:51:17", "1418716277429",
            "150014", "SubmitterName1",
            "FlowBinderName1", 5678L, "SinkName1",
            true, 20, 20, 0, 1, 17, 18, 19, new ArrayList<DiagnosticModel>(),
            "packagingA", "formatA", "charsetA", "destinationA", "mailNotificationA", "mailProcessingA", "resultMailInitialsA");
    private JobModel testJobModel2 = new JobModel("2014-12-17 00:37:48", "1418773068083",
            "424242", "SubmitterName2", "FlowBinderName2", 5679L, "SinkName2",
            true, 10, 10, 0, 5, 20, 21, 22, new ArrayList<DiagnosticModel>(),
            "packagingB", "formatB", "charsetB", "destinationB", "mailNotificationB", "mailProcessingB", "resultMailInitialsB");
    private List<JobModel> testJobModels0 = new ArrayList<JobModel>();
    private List<JobModel> testJobModelsSucceeded = new ArrayList<JobModel>(Arrays.asList(testJobModelSucceeded));
    private List<JobModel> testJobModelsFailed = new ArrayList<JobModel>(Arrays.asList(testJobModelFailed));
    private List<JobModel> testJobModelsIgnored = new ArrayList<JobModel>(Arrays.asList(testJobModelIgnored));
    private List<JobModel> testJobModels2 = new ArrayList<JobModel>(Arrays.asList(testJobModel2, testJobModelSucceeded));

    @Test
    public void constructor_instantiate_objectCorrectInitialized() {

        // Test Subject Under Test
        presenterImpl = new PresenterImpl(mockedPlace, mockedClientFactory);

        // Verify Test
        verify(mockedClientFactory).getItemsShowTexts();
        verify(mockedClientFactory).getPlaceController();
        verify(mockedClientFactory).getJobStoreProxyAsync();
        verify(mockedClientFactory).getLogStoreProxyAsync();
        verifyNoMoreInteractions(mockedClientFactory);
        verify(mockedPlace).getJobId();
        verifyNoMoreInteractions(mockedPlace);
    }

    @Test
    public void start_callStart_ok() {
        presenterImpl = new PresenterImpl(mockedPlace, mockedClientFactory);
        presenterImpl.jobId = "1234";

        // Test Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verify Test
        verify(mockedClientFactory).getItemsShowView();
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
        verify(mockedAllPager).setPageSize(PresenterImpl.PAGE_SIZE);
        verify(mockedFailedPager).setPageSize(PresenterImpl.PAGE_SIZE);
        verify(mockedIgnoredPager).setPageSize(PresenterImpl.PAGE_SIZE);
        verify(mockedAllItemsTable).setRowCount(0);
        verify(mockedFailedItemsTable).setRowCount(0);
        verify(mockedIgnoredItemsTable).setRowCount(0);
        verify(mockedJobDiagnosticTable).setRowCount(0);
        verify(mockedJobStoreProxy).listJobs(any(JobListCriteriaModel.class), any(PresenterImpl.JobCallback.class));
    }

    @Test
    public void allItemsTabSelected_callAllItemsTabSelected_allItemsRequested() {
        presenterImpl = new PresenterImpl(mockedPlace, mockedClientFactory);
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject under test
        presenterImpl.allItemsTabSelected();

        // Verify Test
        // Verifications from the start method (to be able to track no-more-interactions below)
        verify(mockedAllItemsTable).setRowCount(0);
        verify(mockedFailedItemsTable).setRowCount(0);
        verify(mockedIgnoredItemsTable).setRowCount(0);
        verify(mockedAllPager).setPageSize(PresenterImpl.PAGE_SIZE);
        verify(mockedFailedPager).setPageSize(PresenterImpl.PAGE_SIZE);
        verify(mockedIgnoredPager).setPageSize(PresenterImpl.PAGE_SIZE);

        // Verifications from subject under test
        verify(mockedAllItemsTable).getVisibleRange();
        verify(mockedView).setSelectionEnabled(false);
        verify(mockedAllDetailedTabs).clear();
        verify(mockedAllDetailedTabs).setVisible(false);
        verify(mockedAllPager).getPageSize();
        verify(mockedJobStoreProxy).listItems(any(ItemListCriteriaModel.class), any(AsyncCallback.class));
        verifyNoMoreInteractions(mockedFailedItemsTable);
        verifyNoMoreInteractions(mockedIgnoredItemsTable);
        verifyNoMoreInteractions(mockedFailedDetailedTabs);
        verifyNoMoreInteractions(mockedIgnoredDetailedTabs);
        verifyNoMoreInteractions(mockedFailedPager);
        verifyNoMoreInteractions(mockedIgnoredPager);
    }

    @Test
    public void failedItemsTabSelected_callFailedItemsTabSelected_failedItemsRequested() {
        presenterImpl = new PresenterImpl(mockedPlace, mockedClientFactory);
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
        verify(mockedAllPager).setPageSize(PresenterImpl.PAGE_SIZE);
        verify(mockedFailedPager).setPageSize(PresenterImpl.PAGE_SIZE);
        verify(mockedIgnoredPager).setPageSize(PresenterImpl.PAGE_SIZE);

        // Verifications from subject under test
        verify(mockedFailedItemsTable).getVisibleRange();
        verify(mockedView).setSelectionEnabled(false);
        verify(mockedFailedDetailedTabs).clear();
        verify(mockedFailedDetailedTabs).setVisible(false);
        verify(mockedFailedPager).getPageSize();
        verify(mockedJobStoreProxy).listItems(any(ItemListCriteriaModel.class), any(AsyncCallback.class));
        verifyNoMoreInteractions(mockedAllItemsTable);
        verifyNoMoreInteractions(mockedIgnoredItemsTable);
        verifyNoMoreInteractions(mockedAllDetailedTabs);
        verifyNoMoreInteractions(mockedIgnoredDetailedTabs);
        verifyNoMoreInteractions(mockedAllPager);
        verifyNoMoreInteractions(mockedIgnoredPager);
    }

    @Test
    public void ignoredItemsTabSelected_callIgnoredItemsTabSelected_ignoredItemsRequested() {
        presenterImpl = new PresenterImpl(mockedPlace, mockedClientFactory);
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject under test
        presenterImpl.ignoredItemsTabSelected();

        // Verify Test
        // Verifications from the start method (to be able to track no-more-interactions below)
        verify(mockedAllItemsTable).setRowCount(0);
        verify(mockedFailedItemsTable).setRowCount(0);
        verify(mockedIgnoredItemsTable).setRowCount(0);
        verify(mockedAllPager).setPageSize(PresenterImpl.PAGE_SIZE);
        verify(mockedFailedPager).setPageSize(PresenterImpl.PAGE_SIZE);
        verify(mockedIgnoredPager).setPageSize(PresenterImpl.PAGE_SIZE);

        // Verifications from subject under test
        verify(mockedIgnoredItemsTable).getVisibleRange();
        verify(mockedView).setSelectionEnabled(false);
        verify(mockedIgnoredDetailedTabs).clear();
        verify(mockedIgnoredDetailedTabs).setVisible(false);
        verify(mockedIgnoredPager).getPageSize();
        verify(mockedJobStoreProxy).listItems(any(ItemListCriteriaModel.class), any(AsyncCallback.class));
        verifyNoMoreInteractions(mockedAllItemsTable);
        verifyNoMoreInteractions(mockedFailedItemsTable);
        verifyNoMoreInteractions(mockedAllDetailedTabs);
        verifyNoMoreInteractions(mockedFailedDetailedTabs);
        verifyNoMoreInteractions(mockedAllPager);
        verifyNoMoreInteractions(mockedFailedPager);
    }

    @Test
    public void itemSelected_callItemSelected_ok() {
        presenterImpl = new PresenterImpl(mockedPlace, mockedClientFactory);
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        when(mockedAllDetailedTabs.getWidgetCount()).thenReturn(3); // Simulate not empty

        // Subject under test
        presenterImpl.itemSelected(mockedAllItemsListView, testModel1);

        // Verify Test
        verify(mockedAllDetailedTabs).clear();
        verify(mockedText).tab_JavascriptLog();
        verify(mockedAllDetailedTabs).add(any(JavascriptLogTabContent.class), eq(MOCKED_TAB_JAVASCRIPTLOG));
        verify(mockedAllDetailedTabs).add(any(ItemTabContent.class), eq(MOCKED_TAB_INPUTPOST));
        verify(mockedAllDetailedTabs).getWidgetCount();
        verify(mockedAllDetailedTabs).selectTab(0);
        verify(mockedAllDetailedTabs).setVisible(true);
    }

    @Test
    public void fetchJob_callbackWithError_errorMessageInView() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedPlace, mockedClientFactory, mockedAllItemsListView);
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getItemsCallback.onFailure(mockedException);

        // Verify Test
        verify(mockedView).setErrorText(MOCKED_ERROR_COULDNOTFETCHITEMS);
    }

    @Test
    public void fetchAllJobs_callbackWithSuccess_allJobsAreFetched() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedPlace, mockedClientFactory, mockedAllItemsListView);
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getItemsCallback.onSuccess(testModels);

        // Verify Test
        // Called from start()
        verify(mockedAllItemsTable).setRowCount(0);
        verify(mockedFailedItemsTable).setRowCount(0);
        verify(mockedIgnoredItemsTable).setRowCount(0);
        verify(mockedJobDiagnosticTable).setRowCount(0);
        // Called from subject under test
        verify(mockedAllItemsTable).setRowCount(4);
        verify(mockedAllItemsTable).setRowData(OFFSET, testModels);
        verify(mockedView).setSelectionEnabled(true);
        verify(mockedSelectionModel).setSelected(testModel1, true);
        verifyNoMoreInteractions(mockedFailedItemsTable);
        verifyNoMoreInteractions(mockedIgnoredItemsTable);
    }

    @Test
    public void fetchFailedJobs_callbackWithSuccess_failedJobsAreFetched() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedPlace, mockedClientFactory, mockedFailedItemsListView);
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getItemsCallback.onSuccess(testModels);

        // Verify Test
        // Called from start()
        verify(mockedAllItemsTable).setRowCount(0);
        verify(mockedFailedItemsTable).setRowCount(0);
        verify(mockedIgnoredItemsTable).setRowCount(0);
        verify(mockedJobDiagnosticTable).setRowCount(0);
        // Called from subject under test
        verify(mockedFailedItemsTable).setRowCount(4);
        verify(mockedFailedItemsTable).setRowData(OFFSET, testModels);
        verify(mockedView).setSelectionEnabled(true);
        verify(mockedSelectionModel).setSelected(testModel1, true);
        verifyNoMoreInteractions(mockedAllItemsTable);
        verifyNoMoreInteractions(mockedIgnoredItemsTable);
    }

    @Test
    public void fetchIgnoredJobs_callbackWithSuccess_ignoredJobsAreFetched() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedPlace, mockedClientFactory, mockedIgnoredItemsListView);
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getItemsCallback.onSuccess(testModels);

        // Verify Test
        // Called from start()
        verify(mockedAllItemsTable).setRowCount(0);
        verify(mockedFailedItemsTable).setRowCount(0);
        verify(mockedIgnoredItemsTable).setRowCount(0);
        verify(mockedJobDiagnosticTable).setRowCount(0);
        // Called from subject under test
        verify(mockedIgnoredItemsTable).setRowCount(4);
        verify(mockedIgnoredItemsTable).setRowData(OFFSET, testModels);
        verify(mockedView).setSelectionEnabled(true);
        verify(mockedSelectionModel).setSelected(testModel1, true);
        verifyNoMoreInteractions(mockedAllItemsTable);
        verifyNoMoreInteractions(mockedFailedItemsTable);
    }

    @Test
    public void getJob_callbackWithError_errorMessageInView() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedPlace, mockedClientFactory, mockedAllItemsListView);
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobCallback.onFailure(mockedException);

        // Verify Test
        verify(mockedView).setErrorText(MOCKED_ERROR_COULDNOTFETCHJOB);
    }

    @Test
    public void getJob_callbackWithSuccessAndFailedJobs_jobFetchedCorrectly() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedPlace, mockedClientFactory, mockedAllItemsListView);
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobCallback.onSuccess(testJobModelsFailed);

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
        verifyNoMoreInteractions(mockedView.jobHeader);
        verifyNoMoreInteractions(mockedTabPanel);
        verifyNoMoreInteractions(mockedPackaging);
        verifyNoMoreInteractions(mockedFormat);
        verifyNoMoreInteractions(mockedCharset);
        verifyNoMoreInteractions(mockedDestination);
        verifyNoMoreInteractions(mockedMailForNotificationAboutVerification);
        verifyNoMoreInteractions(mockedMailForNotificationAboutProcessing);
        verifyNoMoreInteractions(mockedResultMailInitials);
    }

    @Test
    public void getJob_callbackWithSuccessAndIgnoredJobs_jobFetchedCorrectly() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedPlace, mockedClientFactory, mockedAllItemsListView);
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobCallback.onSuccess(testJobModelsIgnored);

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
        verifyNoMoreInteractions(mockedView.jobHeader);
        verifyNoMoreInteractions(mockedTabPanel);
        verifyNoMoreInteractions(mockedPackaging);
        verifyNoMoreInteractions(mockedFormat);
        verifyNoMoreInteractions(mockedCharset);
        verifyNoMoreInteractions(mockedDestination);
        verifyNoMoreInteractions(mockedMailForNotificationAboutVerification);
        verifyNoMoreInteractions(mockedMailForNotificationAboutProcessing);
        verifyNoMoreInteractions(mockedResultMailInitials);
    }

    @Test
    public void getJob_callbackWithSuccessAndSuccessfulJobs_jobFetchedCorrectly() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedPlace, mockedClientFactory, mockedAllItemsListView);
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobCallback.onSuccess(testJobModelsSucceeded);

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
        verifyNoMoreInteractions(mockedView.jobHeader);
        verifyNoMoreInteractions(mockedTabPanel);
        verifyNoMoreInteractions(mockedPackaging);
        verifyNoMoreInteractions(mockedFormat);
        verifyNoMoreInteractions(mockedCharset);
        verifyNoMoreInteractions(mockedDestination);
        verifyNoMoreInteractions(mockedMailForNotificationAboutVerification);
        verifyNoMoreInteractions(mockedMailForNotificationAboutProcessing);
        verifyNoMoreInteractions(mockedResultMailInitials);
    }

    @Test
    public void getJob_callbackWithSuccessAndMultipleJobs_firstJobFetchedCorrectly() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedPlace, mockedClientFactory, mockedAllItemsListView);
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobCallback.onSuccess(testJobModels2);

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
        verifyNoMoreInteractions(mockedView.jobHeader);
        verifyNoMoreInteractions(mockedTabPanel);
        verifyNoMoreInteractions(mockedPackaging);
        verifyNoMoreInteractions(mockedFormat);
        verifyNoMoreInteractions(mockedCharset);
        verifyNoMoreInteractions(mockedDestination);
        verifyNoMoreInteractions(mockedMailForNotificationAboutVerification);
        verifyNoMoreInteractions(mockedMailForNotificationAboutProcessing);
        verifyNoMoreInteractions(mockedResultMailInitials);
    }

    @Test
    public void getJob_callbackWithSuccessAndNoJobs_noJobFetched() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedPlace, mockedClientFactory, mockedAllItemsListView);
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobCallback.onSuccess(testJobModels0);

        // Verify Test
        verifyNoMoreInteractions(mockedView.jobHeader);
        verifyNoMoreInteractions(mockedTabPanel);
        verifyNoMoreInteractions(mockedPackaging);
        verifyNoMoreInteractions(mockedFormat);
        verifyNoMoreInteractions(mockedCharset);
        verifyNoMoreInteractions(mockedDestination);
        verifyNoMoreInteractions(mockedMailForNotificationAboutVerification);
        verifyNoMoreInteractions(mockedMailForNotificationAboutProcessing);
        verifyNoMoreInteractions(mockedResultMailInitials);
    }

    @Test
    public void getJob_callbackWithSuccessAndNullJobsList_noJobFetched() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedPlace, mockedClientFactory, mockedAllItemsListView);
        presenterImpl.jobId = "1234";
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getJobCallback.onSuccess(null);

        // Verify Test
        verifyNoMoreInteractions(mockedView.jobHeader);
        verifyNoMoreInteractions(mockedTabPanel);
        verifyNoMoreInteractions(mockedPackaging);
        verifyNoMoreInteractions(mockedFormat);
        verifyNoMoreInteractions(mockedCharset);
        verifyNoMoreInteractions(mockedDestination);
        verifyNoMoreInteractions(mockedMailForNotificationAboutVerification);
        verifyNoMoreInteractions(mockedMailForNotificationAboutProcessing);
        verifyNoMoreInteractions(mockedResultMailInitials);
    }

}
