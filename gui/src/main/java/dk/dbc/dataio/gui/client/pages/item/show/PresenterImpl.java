package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.model.ItemListCriteriaModel;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.List;

public class PresenterImpl extends AbstractActivity implements Presenter {
    private static final int PAGE_SIZE = 20;
    protected ClientFactory clientFactory;
    protected Texts texts;
    protected View view;
    protected PlaceController placeController;
    protected JobStoreProxyAsync jobStoreProxy;
    protected LogStoreProxyAsync logStoreProxy;
    protected String jobId;
    protected String submitterNumber;
    protected String sinkName;
    protected int itemCounter;
    protected int failedItemCounter;
    protected int ignoredItemCounter;
    protected boolean isInitialPopulatingOfView;

    public PresenterImpl(com.google.gwt.place.shared.Place place, ClientFactory clientFactory, Texts texts) {
        this.clientFactory = clientFactory;
        this.texts = texts;
        placeController = clientFactory.getPlaceController();
        jobStoreProxy = clientFactory.getJobStoreProxyAsync();
        logStoreProxy = clientFactory.getLogStoreProxyAsync();
        Place showPlace = (Place) place;
        this.jobId = showPlace.getJobId();
        this.submitterNumber = showPlace.getSubmitterNumber();
        this.sinkName = showPlace.getSinkName();
        this.itemCounter = Integer.parseInt(showPlace.getItemCounter());
        this.failedItemCounter = Integer.parseInt(showPlace.getFailedItemCounter());
        this.ignoredItemCounter = Integer.parseInt(showPlace.getIgnoredItemCounter());
        this.isInitialPopulatingOfView = true;
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
        view.jobHeader.setText(constructJobHeaderText());
        containerWidget.setWidget(view.asWidget());
        view.pager.setPageSize(PAGE_SIZE);
        view.itemsTable.setRowCount(0); //clear table on startup
        getItems();
    }

    /*
     * Protected methods
     *
     */

    /**
     * This method fetches items from the job store, and instantiates a callback class to take further action
     * For the initial population of the view:
     *      Failed items will be shown if any failed items exists.
     *      Ignored items will be shown only if no failed items exist and one or more items were ignored.
     *      All items will be shown if no failed or ignored items exists.
     *
     * After the initial population, the method checks, whether to fetch All items, only Failed items or only Ignored items
     * (based on the user selection), and sets the ItemListCriteria according to that.
     */
    protected void getItems() {
        final ItemListCriteriaModel itemListCriteriaModel = new ItemListCriteriaModel();
        final GetItemsCallback getItemsCallback;
        final int offset = view.itemsTable.getVisibleRange().getStart();

        if (isInitialPopulatingOfView) {
            getItemsCallback = initialPopulationOfView(itemListCriteriaModel, offset);

        } else {
            if (view.failedItemsButton.getValue()) {
                itemListCriteriaModel.setItemSearchType(ItemListCriteriaModel.ItemSearchType.FAILED);
                getItemsCallback = new GetItemsCallback(failedItemCounter, offset);
            } else if (view.ignoredItemsButton.getValue()) {
                itemListCriteriaModel.setItemSearchType(ItemListCriteriaModel.ItemSearchType.IGNORED);
                getItemsCallback = new GetItemsCallback(ignoredItemCounter, offset);
            } else {
                itemListCriteriaModel.setItemSearchType(ItemListCriteriaModel.ItemSearchType.ALL);
                getItemsCallback = new GetItemsCallback(itemCounter, offset);
            }
        }
        itemListCriteriaModel.setJobId(this.jobId);
        itemListCriteriaModel.setLimit(view.pager.getPageSize());
        itemListCriteriaModel.setOffset(offset);
        jobStoreProxy.listItems(itemListCriteriaModel, getItemsCallback);
    }

    /*
     * Private methods
     */
    private String constructJobHeaderText() {
        return texts.text_JobId() + " " + jobId + ", "
                + texts.text_Submitter() + " " + submitterNumber + ", "
                + texts.text_Sink() + " " + sinkName;
    }

    private GetItemsCallback initialPopulationOfView(ItemListCriteriaModel itemListCriteriaModel, int offset) {
        view.setSelectionEnabled(false);
        view.tabPanel.setVisible(false);
        view.tabPanel.clear();
        isInitialPopulatingOfView = false;

        final GetItemsCallback getItemsCallback;
        if(failedItemCounter != 0) {
            view.failedItemsButton.setValue(true);
            itemListCriteriaModel.setItemSearchType(ItemListCriteriaModel.ItemSearchType.FAILED);
            getItemsCallback = new GetItemsCallback(failedItemCounter, offset);
        } else {
            if(ignoredItemCounter != 0) {
                view.ignoredItemsButton.setValue(true);
                itemListCriteriaModel.setItemSearchType(ItemListCriteriaModel.ItemSearchType.IGNORED);
                getItemsCallback = new GetItemsCallback(ignoredItemCounter, offset);
            } else {
                view.allItemsButton.setValue(true);
                itemListCriteriaModel.setItemSearchType(ItemListCriteriaModel.ItemSearchType.ALL);
                getItemsCallback = new GetItemsCallback(itemCounter, offset);
            }
        }
        return getItemsCallback;
    }

    /*
     * Overridden methods
     */

    /**
     * An indication from the view, that an item has been selected
     * @param itemModel The model for the selected item
     */
    @Override
    public void itemSelected(ItemModel itemModel) {
        view.tabPanel.clear();
        view.addTab(new JavascriptLogTabContent(texts, logStoreProxy, itemModel), texts.tab_JavascriptLog());
        if (view.tabPanel.getWidgetCount() > 0) {
            view.tabPanel.selectTab(0);
            view.tabPanel.setVisible(true);
        }
    }

    /**
     * This method filters the shown posts, so that the items as requested by the
     * Radio Buttons are shown
     */
    @Override
    public void filterItems() {
        getItems();
    }

    @Override
    public void filterItemsAndClearTable() {
        view.itemsTable.setRowCount(0);
        getItems();
    }

    /*
     * Private classes
     */

    class GetItemsCallback implements AsyncCallback<List<ItemModel>> {

        private final int rowCount;
        private final int offset;

        public GetItemsCallback(int rowCount, int offset) {
            this.rowCount = rowCount;
            this.offset = offset;
        }
        @Override
        public void onFailure(Throwable throwable) {
            view.setErrorText(texts.error_CouldNotFetchItems());
        }
        @Override
        public void onSuccess(List<ItemModel> itemModels) {
            view.setItems(itemModels, offset, rowCount);
            view.setSelectionEnabled(true);

            if(itemModels.size() > 0) {
                view.itemsTable.getSelectionModel().setSelected(itemModels.get(0), true);
            } else {
                view.tabPanel.clear();
                view.tabPanel.setVisible(false);
            }
        }
    }

}
