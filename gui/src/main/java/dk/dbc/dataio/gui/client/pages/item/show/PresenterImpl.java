package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TabBar;
import dk.dbc.dataio.gui.client.model.ItemListCriteriaModel;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.model.JobListCriteriaModel;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.List;

public class PresenterImpl extends AbstractActivity implements Presenter {
    protected static final int PAGE_SIZE = 20;

    protected ClientFactory clientFactory;
    protected Texts texts;
    protected View view;
    protected PlaceController placeController;
    protected JobStoreProxyAsync jobStoreProxy;
    protected LogStoreProxyAsync logStoreProxy;
    protected String jobId;
    protected int allItemCounter;
    protected int failedItemCounter;
    protected int ignoredItemCounter;
    protected JobModel.Type type;
    protected ItemListCriteriaModel.ItemSearchType itemSearchType;

    public PresenterImpl(com.google.gwt.place.shared.Place place, ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        texts = clientFactory.getItemsShowTexts();
        placeController = clientFactory.getPlaceController();
        jobStoreProxy = clientFactory.getJobStoreProxyAsync();
        logStoreProxy = clientFactory.getLogStoreProxyAsync();
        Place itemsShowPlace = (Place) place;
        this.jobId = itemsShowPlace.getJobId();
    }

     /*
     * Overridden methods
     */

    /**
     * start method
     * Is called by PlaceManager, whenever the Place is being invoked
     * This method is the start signal for the presenter
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        view = clientFactory.getItemsShowView();
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        initializeView();
        listJobs(jobId);
    }

    /**
     * This method is called by the view, whenever the All Items tab has been selected
     */
    @Override
    public void allItemsTabSelected() {
        itemSearchType = ItemListCriteriaModel.ItemSearchType.ALL;
        listItems(ItemListCriteriaModel.ItemSearchType.ALL, allItemCounter, view.allItemsList);
    }

    /**
     * This method is called by the view, whenever the Failed Items tab has been selected
     */
    @Override
    public void failedItemsTabSelected() {
        itemSearchType = ItemListCriteriaModel.ItemSearchType.FAILED;
        listItems(ItemListCriteriaModel.ItemSearchType.FAILED, failedItemCounter, view.failedItemsList);
    }

    /**
     * This method is called by the view, whenever the Ignored Items tab has been selected
     */
    @Override
    public void ignoredItemsTabSelected() {
        itemSearchType = ItemListCriteriaModel.ItemSearchType.IGNORED;
        listItems(ItemListCriteriaModel.ItemSearchType.IGNORED, ignoredItemCounter, view.ignoredItemsList);
    }

    /**
     * An indication from the view, that an item has been selected
     * @param listView The list view in question
     * @param itemModel The model for the selected item
     */
    @Override
    public void itemSelected(ItemsListView listView, ItemModel itemModel) {
        listView.detailedTabs.clear();
        addItemTabs(listView, itemModel);
        hideItemTabs(listView);
        selectItemTabsVisibility(listView, itemModel);
        selectItemTab(listView, itemModel);
    }

    /*
     * Private methods
     */

    /**
     * Sets the view according to the supplied job model
     */
    private void initializeView() {
        view.allItemsList.itemsPager.setPageSize(PAGE_SIZE);
        view.failedItemsList.itemsPager.setPageSize(PAGE_SIZE);
        view.ignoredItemsList.itemsPager.setPageSize(PAGE_SIZE);
        view.allItemsList.itemsTable.setRowCount(0); //clear table on startup
        view.failedItemsList.itemsTable.setRowCount(0); //clear table on startup
        view.ignoredItemsList.itemsTable.setRowCount(0); //clear table on startup
        view.jobDiagnosticTabContent.jobDiagnosticTable.setRowCount(0); // clear table on startup
        hideJobTabs();
    }

    /**
     * This method fetches the Job Model with the given jobId, and instantiates a callback class to take further action
     * @param jobId The id for the job model to fetch
     */
    private void listJobs(String jobId) {
        final JobListCriteriaModel jobListCriteriaModel = new JobListCriteriaModel();
        jobListCriteriaModel.setSearchType(JobListCriteriaModel.JobSearchType.ALL);
        jobListCriteriaModel.setId(Long.valueOf(jobId));
        jobStoreProxy.listJobs(jobListCriteriaModel, new JobsCallback());
    }

    /**
     * This method fetches items from the job store, and instantiates a callback class to take further action
     * @param itemSearchType The search type (ALL, FAILED or IGNORED)
     * @param rowCount The number of rows in the data
     * @param listView The list view in question
     */
    private void listItems(ItemListCriteriaModel.ItemSearchType itemSearchType, int rowCount, ItemsListView listView) {
        final ItemListCriteriaModel itemListCriteriaModel = new ItemListCriteriaModel();
        final int offset = listView.itemsTable.getVisibleRange().getStart();

        view.setSelectionEnabled(false);
        listView.detailedTabs.clear();
        listView.detailedTabs.setVisible(false);
        itemListCriteriaModel.setItemSearchType(itemSearchType);
        itemListCriteriaModel.setJobId(this.jobId);
        itemListCriteriaModel.setLimit(listView.itemsPager.getPageSize());
        itemListCriteriaModel.setOffset(offset);
        jobStoreProxy.listItems(itemListCriteriaModel, new ItemsCallback(listView, rowCount, offset));
    }

    /**
     * Sets the view according to the supplied job model
     * @param jobModel containing the data to display in the view
     */
    private void setJobModel(JobModel jobModel) {
        allItemCounter = (int) jobModel.getItemCounter();
        failedItemCounter = (int) jobModel.getFailedCounter();
        ignoredItemCounter = (int) jobModel.getIgnoredCounter();
        type = jobModel.getType();
        view.jobHeader.setText(constructJobHeaderText(jobModel));
        selectJobTabVisibility(jobModel);
        selectJobTab(jobModel);
        setJobInfoTab(jobModel);
        setDiagnosticModels(jobModel);
    }

    /**
     * Sets the view according to the supplied item model list
     * @param table The CellTable in the View, hungering for data
     * @param itemModels The list of item models, containing the data to display in the view
     */
    private void setItemModels(CellTable table, List<ItemModel> itemModels, int rowCount, int offset) {
        table.setRowCount(rowCount);
        table.setRowData(offset, itemModels);
        view.setSelectionEnabled(true);
        if(itemModels.size() > 0) {
            table.getSelectionModel().setSelected(itemModels.get(0), true);
        }
    }

    /*
     * ============= > Methods used for displaying job data and showing/hiding/selecting job tabs < =============
     */

    /**
     * Hides job tabs
     */
    private void hideJobTabs() {
        setJobTabVisibility(ViewWidget.ALL_ITEMS_TAB_INDEX, false);
        setJobTabVisibility(ViewWidget.FAILED_ITEMS_TAB_INDEX, false);
        setJobTabVisibility(ViewWidget.IGNORED_ITEMS_TAB_INDEX, false);
        setJobTabVisibility(ViewWidget.JOB_INFO_TAB_CONTENT, false);
        setJobTabVisibility(ViewWidget.JOB_DIAGNOSTIC_TAB_CONTENT, false);
    }

    /**
     * This method constructs a Job Header Text from a Job Id, a Submitter Number and a Sink Name
     * @param jobModel containing the job data
     * @return The resulting Job Header Text
     */
    private String constructJobHeaderText(JobModel jobModel) {
        return texts.text_JobId() + " " + jobModel.getJobId() + ", "
                + texts.text_Submitter() + " " + jobModel.getSubmitterNumber() + ", "
                + texts.text_Sink() + " " + jobModel.getSinkName();
    }

    /**
     * Deciphers which tabs should be visible
     * @param jobModel The model containing the job data
     */
    private void selectJobTabVisibility(JobModel jobModel) {
        setJobTabVisibility(ViewWidget.JOB_INFO_TAB_CONTENT, true);

        // Show diagnostic tab if one or more diagnostics exists
        if(!jobModel.getDiagnosticModels().isEmpty()) {
            setJobTabVisibility(ViewWidget.JOB_DIAGNOSTIC_TAB_CONTENT, true);
        }
        // Show item information if one or more items exist
        if(allItemCounter != 0){
            setJobTabVisibility(ViewWidget.ALL_ITEMS_TAB_INDEX, true);
            if(failedItemCounter != 0) {
                setJobTabVisibility(ViewWidget.FAILED_ITEMS_TAB_INDEX, true);
            }
            if(ignoredItemCounter != 0) {
                setJobTabVisibility(ViewWidget.IGNORED_ITEMS_TAB_INDEX, true);
            }
        }
    }

    /**
     * Deciphers which tab should have focus, when the view initially is presented to the user
     * @param jobModel containing the job data
     */
    private void selectJobTab(JobModel jobModel) {
        if(jobModel.isDiagnosticFatal()) {
            view.tabPanel.selectTab(ViewWidget.JOB_DIAGNOSTIC_TAB_CONTENT);
        } else {
            if (failedItemCounter != 0) {
                view.tabPanel.selectTab(ViewWidget.FAILED_ITEMS_TAB_INDEX);
            } else if (ignoredItemCounter != 0) {
                view.tabPanel.selectTab(ViewWidget.IGNORED_ITEMS_TAB_INDEX);
            } else {
                view.tabPanel.selectTab(ViewWidget.ALL_ITEMS_TAB_INDEX);
            }
        }
    }

    /**
     * Hide or Show the Tab. Can't call setVisible directly on Tab because it
     * is an interface. Need to cast to the underlying Composite and then
     * call setVisible on it.
     *
     * @param tabIndex the index as defined in the ViewWidget constants
     * @param showTab whether to show or hide the tab.
     */
    private void setJobTabVisibility(int tabIndex, boolean showTab) {
        TabBar.Tab tabObject = view.tabPanel.getTabBar().getTab(tabIndex);
        if (tabObject == null) {
            return;
        }
        if (tabObject instanceof Composite) {
            ((Composite) tabObject).setVisible(showTab);
        }
    }

    /**
     * Sets the Job Info tab according to the supplied Job Model
     * @param jobModel The Job Model, where the Job Info data is taken
     */
    private void setJobInfoTab(JobModel jobModel) {
        view.jobInfoTabContent.packaging.setText(jobModel.getPackaging());
        view.jobInfoTabContent.format.setText(jobModel.getFormat());
        view.jobInfoTabContent.charset.setText(jobModel.getCharset());
        view.jobInfoTabContent.destination.setText(jobModel.getDestination());
        view.jobInfoTabContent.mailForNotificationAboutVerification.setText(jobModel.getMailForNotificationAboutVerification());
        view.jobInfoTabContent.mailForNotificationAboutProcessing.setText(jobModel.getMailForNotificationAboutProcessing());
        view.jobInfoTabContent.resultMailInitials.setText(jobModel.getResultmailInitials());
    }


    /**
     * Sets the Job Diagnostic tab according to the supplied Job Model
     * @param jobModel The Job Model, where the list of Diagnostic data is taken
     */
    private void setDiagnosticModels(JobModel jobModel) {
        view.jobDiagnosticTabContent.jobDiagnosticTable.setRowData(0, jobModel.getDiagnosticModels());
    }

    /*
     * ============ > Methods used for displaying item data and showing/hiding/selecting item tabs < ============
     */

    /**
     * This method adds item tabs to the list view.
     * @param listView The list view in question
     * @param itemModel The model containing the item data
     */
    private void addItemTabs(ItemsListView listView, ItemModel itemModel) {
        listView.detailedTabs.add(new JavascriptLogTabContent(texts, logStoreProxy, itemModel), texts.tab_JavascriptLog());
        listView.detailedTabs.add(new ItemTabContent(texts, jobStoreProxy, itemModel, ItemModel.LifeCycle.PARTITIONING), texts.tab_PartitioningPost());
        listView.detailedTabs.add(new ItemTabContent(texts, jobStoreProxy, itemModel, ItemModel.LifeCycle.PROCESSING), texts.tab_ProcessingPost());
        listView.detailedTabs.add(new NextTabContent(texts, jobStoreProxy, itemModel), texts.tab_NextOutputPost());
        listView.detailedTabs.add(new ItemTabContent(texts, jobStoreProxy, itemModel, ItemModel.LifeCycle.DELIVERING), texts.tab_DeliveringPost());
        if (!itemModel.getDiagnosticModels().isEmpty()) {
            setDiagnosticModels(listView, itemModel);
            listView.detailedTabs.add(listView.itemDiagnosticTabContent.itemDiagnosticTable, texts.tab_ItemDiagnostic());
        }
    }

    /**
     * Hides item tabs
     * @param listView The list view in question
     */
    private void hideItemTabs(ItemsListView listView) {
        setItemTabVisibility(listView, listView.ITEM_DIAGNOSTIC_TAB_CONTENT, false);
        setItemTabVisibility(listView, listView.JAVASCRIPT_LOG_TAB_CONTENT, false);
        setItemTabVisibility(listView, listView.INPUT_POST_TAB_CONTENT, false);
        setItemTabVisibility(listView, listView.OUTPUT_POST_TAB_CONTENT, false);
        setItemTabVisibility(listView, listView.NEXT_OUTPUT_POST_TAB_CONTENT, false);
        setItemTabVisibility(listView, listView.SINK_RESULT_TAB_CONTENT, false);
    }


    /**
     * This method decides which detailed tab should be default selected
     * @param listView The list view in question
     * @param itemModel The model containing the item data
     */
    private void selectItemTab(ItemsListView listView, ItemModel itemModel) {

        ItemModel.LifeCycle status = itemModel.getStatus();
        if(itemModel.isDiagnosticFatal()) {
            listView.detailedTabs.selectTab(listView.ITEM_DIAGNOSTIC_TAB_CONTENT);
        } else {

            // Item failed in delivering: Show sink result
            if (itemSearchType == ItemListCriteriaModel.ItemSearchType.FAILED && status == ItemModel.LifeCycle.DELIVERING) {
                listView.detailedTabs.selectTab(listView.SINK_RESULT_TAB_CONTENT);
            }
            // Item failed in processing: Show output post
            else if (itemSearchType == ItemListCriteriaModel.ItemSearchType.FAILED && status == ItemModel.LifeCycle.PROCESSING) {
                listView.detailedTabs.selectTab(listView.OUTPUT_POST_TAB_CONTENT);
            }
            // Item ignored in processing or delivering: Show output post
            else if (itemSearchType == ItemListCriteriaModel.ItemSearchType.IGNORED && (status == ItemModel.LifeCycle.PROCESSING || status == ItemModel.LifeCycle.DELIVERING)) {
                listView.detailedTabs.selectTab(listView.OUTPUT_POST_TAB_CONTENT);
            } else {
                listView.detailedTabs.selectTab(listView.JAVASCRIPT_LOG_TAB_CONTENT);
            }
        }
        listView.detailedTabs.setVisible(true);
    }

    /**
     * Deciphers which tabs should be visible.
     * @param listView The list view in question
     * @param itemModel The model containing the item data
     */
    private void selectItemTabsVisibility(ItemsListView listView, ItemModel itemModel) {
        if (itemModel.isDiagnosticFatal()) {
            setItemTabVisibility(listView, listView.ITEM_DIAGNOSTIC_TAB_CONTENT, true);
        } else {
            if (!itemModel.getDiagnosticModels().isEmpty()) {
                setItemTabVisibility(listView, listView.ITEM_DIAGNOSTIC_TAB_CONTENT, true);
            }
            if(type == JobModel.Type.ACCTEST) {
                setItemTabVisibility(listView, listView.NEXT_OUTPUT_POST_TAB_CONTENT, true);
            }
            setItemTabVisibility(listView, listView.JAVASCRIPT_LOG_TAB_CONTENT, true);
            setItemTabVisibility(listView, listView.INPUT_POST_TAB_CONTENT, true);
            setItemTabVisibility(listView, listView.OUTPUT_POST_TAB_CONTENT, true);
            setItemTabVisibility(listView, listView.SINK_RESULT_TAB_CONTENT, true);
        }
    }

    /**
     * Hide or Show the Tab. Can't call setVisible directly on Tab because it
     * is an interface. Need to cast to the underlying Composite and then
     * call setVisible on it.
     *
     * @param listView the list view in question
     * @param tabIndex the index as defined in the ItemsListView constants
     * @param showTab whether to show or hide the tab.
     */
    private void setItemTabVisibility(ItemsListView listView, int tabIndex, boolean showTab) {
        TabBar.Tab tabObject = listView.detailedTabs.getTabBar().getTab(tabIndex);
        if (tabObject == null) {
            return;
        }
        if (tabObject instanceof Composite) {
            ((Composite) tabObject).setVisible(showTab);
        }
    }

    /**
     * Sets the Item Diagnostic tab according to the supplied Job Model
     * @param itemModel The Item Model, where the list of Diagnostic data is taken
     */
    private void setDiagnosticModels(ItemsListView listView, ItemModel itemModel) {
        listView.itemDiagnosticTabContent.itemDiagnosticTable.setRowData(0, itemModel.getDiagnosticModels());
    }

    /*
     * ================================ > Private async callback classes < =====================================
     */

    /*
     * Callback class for fetching Jobs
     */
    class JobsCallback implements AsyncCallback<List<JobModel>> {
        @Override
        public void onFailure(Throwable throwable) {
            view.setErrorText(texts.error_CouldNotFetchJob());
        }
        @Override
        public void onSuccess(List<JobModel> jobModels) {
            if (jobModels != null && jobModels.size() > 0) {
                setJobModel(jobModels.get(0));
            }
        }
    }

    /*
     * Callback class for fetching Items
     */
    class ItemsCallback implements AsyncCallback<List<ItemModel>> {
        private final ItemsListView listView;
        private final int rowCount;
        private final int offset;

        public ItemsCallback(ItemsListView listView, int rowCount, int offset) {
            this.listView = listView;
            this.rowCount = rowCount;
            this.offset = offset;
        }
        @Override
        public void onFailure(Throwable throwable) {
            view.setErrorText(texts.error_CouldNotFetchItems());
        }
        @Override
        public void onSuccess(List<ItemModel> itemModels) {
            setItemModels(listView.itemsTable, itemModels, rowCount, offset);
        }
    }

}
