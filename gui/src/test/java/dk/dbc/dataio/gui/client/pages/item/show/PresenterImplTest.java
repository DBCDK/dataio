
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
import dk.dbc.dataio.gui.client.model.ItemListCriteriaModel;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

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
    @Mock SimplePager mockedAllPager;
    @Mock SimplePager mockedFailedPager;
    @Mock SimplePager mockedIgnoredPager;
    @Mock DecoratedTabPanel mockedAllDetailedTabs;
    @Mock DecoratedTabPanel mockedFailedDetailedTabs;
    @Mock DecoratedTabPanel mockedIgnoredDetailedTabs;
    @Mock DecoratedTabPanel mockedTabPanel;
    @Mock SingleSelectionModel<ItemModel> mockedSelectionModel;

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
        mockedIgnoredItemsListView.itemsTable = mockedIgnoredItemsTable;
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
        when(mockedPlace.getItemCounter()).thenReturn("4");
        when(mockedPlace.getFailedItemCounter()).thenReturn("0");
        when(mockedPlace.getIgnoredItemCounter()).thenReturn("0");
        when(mockedView.allItemsList.itemsTable.getVisibleRange()).thenReturn(new Range(OFFSET, ROW_COUNT));
        when(mockedView.failedItemsList.itemsTable.getVisibleRange()).thenReturn(new Range(OFFSET, ROW_COUNT));
        when(mockedView.ignoredItemsList.itemsTable.getVisibleRange()).thenReturn(new Range(OFFSET, ROW_COUNT));
        when(mockedView.allItemsList.itemsTable.getSelectionModel()).thenReturn(mockedSelectionModel);
        when(mockedView.failedItemsList.itemsTable.getSelectionModel()).thenReturn(mockedSelectionModel);
        when(mockedView.ignoredItemsList.itemsTable.getSelectionModel()).thenReturn(mockedSelectionModel);
    }

    // Mocked Texts
    @Mock static Texts mockedText;
    final static String MOCKED_MENU_ITEMS = "Mocked Poster";
    final static String MOCKED_COLUMN_ITEM = "Mocked Post";
    final static String MOCKED_COLUMN_STATUS = "Mocked Status";
    final static String MOCKED_ERROR_COULDNOTFETCHITEMS = "Mocked Det var ikke muligt at hente poster fra Job Store";
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
    @Before
    public void setupMockedTextsBehaviour() {
        when(mockedClientFactory.getItemsShowTexts()).thenReturn(mockedText);
        when(mockedText.menu_Items()).thenReturn(MOCKED_MENU_ITEMS);
        when(mockedText.column_Item()).thenReturn(MOCKED_COLUMN_ITEM);
        when(mockedText.column_Status()).thenReturn(MOCKED_COLUMN_STATUS);
        when(mockedText.error_CouldNotFetchItems()).thenReturn(MOCKED_ERROR_COULDNOTFETCHITEMS);
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
    }

    // Subject Under Test
    private PresenterImpl presenterImpl;


    // Test specialization of Presenter to enable test of callback's
    class PresenterImplConcrete extends PresenterImpl {
        ItemsListView itemsListView;
        public ItemsCallback getItemsCallback;

        public PresenterImplConcrete(Place place, ClientFactory clientFactory, ItemsListView itemsListView) {
            super(place, clientFactory);
            this.itemsListView = itemsListView;
            this.getItemsCallback = new ItemsCallback(itemsListView, ROW_COUNT, OFFSET);
        }
    }


    // Test Data
    private ItemModel testModel1 = new ItemModel("11", "1001", "1111", "JobId1", ItemModel.LifeCycle.DELIVERING);
    private ItemModel testModel2 = new ItemModel("12", "ItemId2", "ChunkId2", "JobId2", ItemModel.LifeCycle.DONE);
    private ItemModel testModel3 = new ItemModel("13", "ItemId3", "ChunkId3", "JobId3", ItemModel.LifeCycle.PARTITIONING);
    private ItemModel testModel4 = new ItemModel("14", "ItemId4", "ChunkId4", "JobId4", ItemModel.LifeCycle.PROCESSING);
    private List<ItemModel> testModels = Arrays.asList(testModel1, testModel2, testModel3, testModel4);

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
        verify(mockedPlace).getSubmitterNumber();
        verify(mockedPlace).getSinkName();
        verify(mockedPlace).getItemCounter();
        verify(mockedPlace).getFailedItemCounter();
        verify(mockedPlace).getIgnoredItemCounter();
        verifyNoMoreInteractions(mockedPlace);
    }

    @Test
    public void start_callStart_initialPopulationOfViewSetsAllItems() {
        setupSpecificMockedItemCounters("4", "0", "0");
        genericStart_callStart_ok();

        verify(mockedView.tabPanel).selectTab(ViewWidget.ALL_ITEMS_TAB_INDEX);

    }

    @Test
    public void start_callStart_initialPopulationOfViewSetsIgnoredItems() {
        setupSpecificMockedItemCounters("4", "0", "1");
        genericStart_callStart_ok();

        verify(mockedView.tabPanel).selectTab(ViewWidget.IGNORED_ITEMS_TAB_INDEX);
    }

    @Test
    public void start_callStart_initialPopulationOfViewSetsFailedItems() {
        setupSpecificMockedItemCounters("4", "4", "2");
        genericStart_callStart_ok();

        verify(mockedView.tabPanel).selectTab(ViewWidget.FAILED_ITEMS_TAB_INDEX);
    }

    @Test
    public void allItemsTabSelected_callAllItemsTabSelected_allItemsRequested() {
        presenterImpl = new PresenterImpl(mockedPlace, mockedClientFactory);
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
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Subject under test
        presenterImpl.failedItemsTabSelected();

        // Verify Test
        // Verifications from the start method (to be able to track no-more-interactions below)
        verify(mockedAllItemsTable).setRowCount(0);
        verify(mockedFailedItemsTable).setRowCount(0);
        verify(mockedIgnoredItemsTable).setRowCount(0);
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
        presenterImpl.start(mockedContainerWidget, mockedEventBus);
        when(mockedAllDetailedTabs.getWidgetCount()).thenReturn(3); // Simulate not empty

        // Subject under test
        presenterImpl.itemSelected(mockedAllItemsListView, testModel1);

        // Verify Test
        verify(mockedAllDetailedTabs).clear();
        verify(mockedText).tab_JavascriptLog();
        verify(mockedAllDetailedTabs).add(any(JavascriptLogTabContent.class), eq(MOCKED_TAB_JAVASCRIPTLOG));
        verify(mockedAllDetailedTabs).getWidgetCount();
        verify(mockedAllDetailedTabs).selectTab(0);
        verify(mockedAllDetailedTabs).setVisible(true);
    }

    @Test
    public void fetchJob_callbackWithError_errorMessageInView() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedPlace, mockedClientFactory, mockedAllItemsListView);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getItemsCallback.onFailure(mockedException);

        // Verify Test
        verify(mockedView).setErrorText(MOCKED_ERROR_COULDNOTFETCHITEMS);
    }

    @Test
    public void fetchAllJobs_callbackWithSuccess_allJobsAreFetched() {
        PresenterImplConcrete presenterImpl = new PresenterImplConcrete(mockedPlace, mockedClientFactory, mockedAllItemsListView);
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getItemsCallback.onSuccess(testModels);

        // Verify Test
        // Called from start()
        verify(mockedAllItemsTable).setRowCount(0);
        verify(mockedFailedItemsTable).setRowCount(0);
        verify(mockedIgnoredItemsTable).setRowCount(0);
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
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getItemsCallback.onSuccess(testModels);

        // Verify Test
        // Called from start()
        verify(mockedAllItemsTable).setRowCount(0);
        verify(mockedFailedItemsTable).setRowCount(0);
        verify(mockedIgnoredItemsTable).setRowCount(0);
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
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Test Subject Under Test
        presenterImpl.getItemsCallback.onSuccess(testModels);

        // Verify Test
        // Called from start()
        verify(mockedAllItemsTable).setRowCount(0);
        verify(mockedFailedItemsTable).setRowCount(0);
        verify(mockedIgnoredItemsTable).setRowCount(0);
        // Called from subject under test
        verify(mockedIgnoredItemsTable).setRowCount(4);
        verify(mockedIgnoredItemsTable).setRowData(OFFSET, testModels);
        verify(mockedView).setSelectionEnabled(true);
        verify(mockedSelectionModel).setSelected(testModel1, true);
        verifyNoMoreInteractions(mockedAllItemsTable);
        verifyNoMoreInteractions(mockedFailedItemsTable);
    }



    /*
     * Private methods
     */

    private void genericStart_callStart_ok() {
        presenterImpl = new PresenterImpl(mockedPlace, mockedClientFactory);
        presenterImpl.jobId = "1234";
        presenterImpl.submitterNumber = "Submi";
        presenterImpl.sinkName = "Sinki";

        // Test Subject Under Test
        presenterImpl.start(mockedContainerWidget, mockedEventBus);

        // Verify Test
        verify(mockedClientFactory).getItemsShowView();
        verify(mockedView).setPresenter(presenterImpl);
        verify(mockedJobHeader).setText("Mocked Job Id: 1234, Mocked Submitter: Submi, Mocked Sink: Sinki");
        verify(mockedContainerWidget).setWidget(mockedViewWidget);
        verify(mockedAllPager).setPageSize(PresenterImpl.PAGE_SIZE);
        verify(mockedFailedPager).setPageSize(PresenterImpl.PAGE_SIZE);
        verify(mockedIgnoredPager).setPageSize(PresenterImpl.PAGE_SIZE);
        verify(mockedAllItemsTable).setRowCount(0);
        verify(mockedFailedItemsTable).setRowCount(0);
        verify(mockedIgnoredItemsTable).setRowCount(0);
    }

    private void setupSpecificMockedItemCounters(String allItemCounter, String failedItemCounter, String ignoredItemCounter) {
        when(mockedPlace.getItemCounter()).thenReturn(allItemCounter);
        when(mockedPlace.getFailedItemCounter()).thenReturn(failedItemCounter);
        when(mockedPlace.getIgnoredItemCounter()).thenReturn(ignoredItemCounter);
    }

}
