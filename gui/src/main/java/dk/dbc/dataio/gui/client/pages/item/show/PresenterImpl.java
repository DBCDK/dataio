package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
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

    public PresenterImpl(com.google.gwt.place.shared.Place place, ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        texts = clientFactory.getItemsShowTexts();
        placeController = clientFactory.getPlaceController();
        jobStoreProxy = clientFactory.getJobStoreProxyAsync();
        logStoreProxy = clientFactory.getLogStoreProxyAsync();
        Place itemsShowPlace = (Place) place;
        this.jobId = itemsShowPlace.getJobId();
    }

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
        getJobModel(jobId);
    }


    /*
     * Overridden methods
     */

    /**
     * This method is called by the view, whenever the All Items tab has been selected
     */
    @Override
    public void allItemsTabSelected() {
        getItems(ItemListCriteriaModel.ItemSearchType.ALL, allItemCounter, view.allItemsList);
    }

    /**
     * This method is called by the view, whenever the Failed Items tab has been selected
     */
    @Override
    public void failedItemsTabSelected() {
        getItems(ItemListCriteriaModel.ItemSearchType.FAILED, failedItemCounter, view.failedItemsList);
    }

    /**
     * This method is called by the view, whenever the Ignored Items tab has been selected
     */
    @Override
    public void ignoredItemsTabSelected() {
        getItems(ItemListCriteriaModel.ItemSearchType.IGNORED, ignoredItemCounter, view.ignoredItemsList);
    }

    /**
     * An indication from the view, that an item has been selected
     * @param itemModel The model for the selected item
     */
    @Override
    public void itemSelected(ItemsListView listView, ItemModel itemModel) {
        listView.detailedTabs.clear();
        listView.detailedTabs.add(new JavascriptLogTabContent(texts, logStoreProxy, itemModel), texts.tab_JavascriptLog());
        listView.detailedTabs.add(new ItemTabContent(texts, jobStoreProxy, itemModel, ItemModel.LifeCycle.PARTITIONING), texts.tab_PartitioningPost());
        listView.detailedTabs.add(new ItemTabContent(texts, jobStoreProxy, itemModel, ItemModel.LifeCycle.PROCESSING), texts.tab_ProcessingPost());
        listView.detailedTabs.add(new ItemTabContent(texts, jobStoreProxy, itemModel, ItemModel.LifeCycle.DELIVERING), texts.tab_DeliveringPost());
        if (listView.detailedTabs.getWidgetCount() > 0) {
            if(itemModel.getStatus() == ItemModel.LifeCycle.DELIVERING && failedItemCounter > 0) {
                listView.detailedTabs.selectTab(3);
            } else {
                listView.detailedTabs.selectTab(0);
            }
            listView.detailedTabs.setVisible(true);
        }
    }

    /*
     * Private methods
     */


    /**
     * This method fetches the Job Model with the given jobId
     * @param jobId The id for the job model to fetch
     */
    private void getJobModel(String jobId) {
        final JobListCriteriaModel jobListCriteriaModel = new JobListCriteriaModel();
        jobListCriteriaModel.setSearchType(JobListCriteriaModel.JobSearchType.ALL);
        jobListCriteriaModel.setId(Long.valueOf(jobId));
        jobStoreProxy.listJobs(jobListCriteriaModel, new JobCallback());
    }

    /**
     * This method fetches items from the job store, and instantiates a callback class to take further action
     *
     * @param itemSearchType The search type (ALL, FAILED or IGNORED)
     * @param rowCount The number of rows in the data
     * @param listView The list view in question
     */
    private void getItems(ItemListCriteriaModel.ItemSearchType itemSearchType, int rowCount, ItemsListView listView) {
        final ItemListCriteriaModel itemListCriteriaModel = new ItemListCriteriaModel();
        final int offset = listView.itemsTable.getVisibleRange().getStart();
        ItemsCallback callback = new ItemsCallback(listView, rowCount, offset);

        view.setSelectionEnabled(false);
        listView.detailedTabs.clear();
        listView.detailedTabs.setVisible(false);
        itemListCriteriaModel.setItemSearchType(itemSearchType);
        itemListCriteriaModel.setJobId(this.jobId);
        itemListCriteriaModel.setLimit(listView.itemsPager.getPageSize());
        itemListCriteriaModel.setOffset(offset);
        jobStoreProxy.listItems(itemListCriteriaModel, callback);
    }

    /**
     * This method constructs a Job Header Text from a Job Id, a Submitter Number and a Sink Name
     * @param model The model containing the job data
     * @return The resulting Job Header Text
     */
    private String constructJobHeaderText(JobModel model) {
        return texts.text_JobId() + " " + model.getJobId() + ", "
                + texts.text_Submitter() + " " + model.getSubmitterNumber() + ", "
                + texts.text_Sink() + " " + model.getSinkName();
    }

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
    }

    /**
     * Sets the view according to the supplied job model
     * @param jobModel The job model, containing the data to display in the view
     */
    private void setJobModel(JobModel jobModel) {
        allItemCounter = (int) jobModel.getItemCounter();
        failedItemCounter = (int) jobModel.getFailedCounter();
        ignoredItemCounter = (int) jobModel.getIgnoredCounter();
        view.jobHeader.setText(constructJobHeaderText(jobModel));
        if(allItemCounter == 0) {
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
        setJobInfoTab(jobModel);
        setDiagnosticModels(jobModel);
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
     * Private classes
     */

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

    /*
     * Callback class for fetching Jobs
     */
    class JobCallback implements AsyncCallback<List<JobModel>> {
        @Override
        public void onFailure(Throwable throwable) {
            view.setErrorText(texts.error_CouldNotFetchJob());
        }
        @Override
        public void onSuccess(List<JobModel> jobModels) {
            if (jobModels != null && jobModels.size()>0)
            setJobModel(jobModels.get(0));
        }
    }

}
