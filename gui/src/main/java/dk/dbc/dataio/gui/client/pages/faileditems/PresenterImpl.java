package dk.dbc.dataio.gui.client.pages.faileditems;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.ChunkCompletionState;
import dk.dbc.dataio.commons.types.ItemCompletionState;
import dk.dbc.dataio.commons.types.JobCompletionState;
import dk.dbc.dataio.commons.types.JobInfo;
import dk.dbc.dataio.commons.types.JobState;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.FailedItemModel;
import dk.dbc.dataio.gui.client.pages.javascriptlog.JavaScriptLogPlace;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.List;

public class PresenterImpl extends AbstractActivity implements Presenter {
    protected ClientFactory clientFactory;
    protected Texts texts;
    protected View view;
    protected PlaceController placeController;
    protected JobStoreProxyAsync jobStoreProxy;
    protected long jobId;
    protected JobState.OperationalState operationalState;
    protected ItemCompletionState.State itemState;

    protected JobInfo jobInfo;

    public PresenterImpl(Place place, ClientFactory clientFactory, Texts texts) {
        this.clientFactory = clientFactory;
        this.texts = texts;
        placeController = clientFactory.getPlaceController();
        jobStoreProxy = clientFactory.getJobStoreProxyAsync();
        ShowPlace showPlace = (ShowPlace) place;
        this.jobId = showPlace.getJobId();
        this.operationalState = showPlace.getOperationalState();
        this.itemState = showPlace.getStatus();
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
        view = clientFactory.getFaileditemsView();
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        getAllFailedItems();
    }

    /**
     * A signal from the view, saying that an item has been selected in the failed items list
     */
    @Override
    public void failedItemSelected(FailedItemModel model) {
        placeController.goTo(new JavaScriptLogPlace(
                        Long.parseLong(model.getJobId()),
                        Long.parseLong(model.getChunkId()),
                        Long.parseLong(model.getItemId()))
        );
    }


    /*
     * Protected methods
     *
     */

    /**
     * This method fetches all failed items from the job store
     */
    protected void getAllFailedItems() {
        jobStoreProxy.findAllJobs(new GetAllFailedItemsCallback());
    }

    /**
     * This method constructs a Place for the Java Script Log
     *
     * @param model The FailedItemModel for which a Place is to be constructed
     * @return The Place
     */
    protected JavaScriptLogPlace constructJavaScriptLogPlace(FailedItemModel model) {
        return new JavaScriptLogPlace(
                Long.parseLong(model.getJobId()),
                Long.parseLong(model.getChunkId()),
                Long.parseLong(model.getItemId())
        );
    }

    /**
     * This method requests all JobCompletionStatus'es to be fetched from the job store
     *
     * @param jobInfos The jobs to fetch status for
     */
    protected void getAllJobCompletionStatus(List<JobInfo> jobInfos) {
        view.clearFailedItemsList();
        for (JobInfo jobInfo : jobInfos) {
            if (this.jobId == jobInfo.getJobId()) {
                getJobCompletionStatus(jobInfo.getJobId());
                this.jobInfo = jobInfo;
            }
        }
    }

    /**
     * This method requests a JobCompletionStatus to be fetched from the Job Store
     *
     * @param jobId The Job ID
     */
    protected void getJobCompletionStatus(final long jobId) {
        jobStoreProxy.getJobCompletionState(jobId, new GetJobCompletionStatusCallback());
    }

    /**
     * This method adds a JobCompletionState to be added to the View
     *
     * @param jobCompletionState The Job Completion State
     */
    protected void addJobCompletionStateToView(JobCompletionState jobCompletionState) {
        for (ChunkCompletionState chunkCompletionState : jobCompletionState.getChunks()) {
            String chunkId = Long.toString(chunkCompletionState.getChunkId());
            for (ItemCompletionState itemCompletionState : chunkCompletionState.getItems()) {
                filterFailedItems(chunkId, itemCompletionState);
            }
        }
    }

    /*
    * Private methods
    *
    */

    /**
     * Method deciphering which failed items to display in the view.
     *
     * @param chunkId             identifying the chunk
     * @param itemCompletionState containing information regarding item completion state for each operational state
     */

    private void filterFailedItems(String chunkId, ItemCompletionState itemCompletionState) {
        if (operationalState == null) {
            filterFailedItemsOperationalStateExcluded(chunkId, itemCompletionState);
        } else {
            filterFailedItemsOperationalStateIncluded(chunkId, itemCompletionState);
        }
    }

    /**
     * Method filtering failed items when an operational state has NOT been specified.
     *
     * @param chunkId             identifying the chunk
     * @param itemCompletionState containing information regarding item completion state for each operational state
     */
    private void filterFailedItemsOperationalStateExcluded(String chunkId, ItemCompletionState itemCompletionState) {

        // => show all failed items regardless item state (SUCCESS, FAILURE, IGNORED)
        if (itemState == null) {
            view.addFailedItem(buildFailedItemModel(itemCompletionState, chunkId));
        } else {
            // => show all failed items with specific item state
            if (itemState == itemCompletionState.getChunkifyState()
                    || itemState == itemCompletionState.getProcessingState()
                    || itemState == itemCompletionState.getDeliveryState()) {
                view.addFailedItem(buildFailedItemModel(itemCompletionState, chunkId));
            }
        }
    }

    /**
     * Method filtering failed items when an operational state has been specified.
     *
     * @param chunkId             identifying the chunk
     * @param itemCompletionState containing information regarding item completion state for each operational state
     */
    private void filterFailedItemsOperationalStateIncluded(String chunkId, ItemCompletionState itemCompletionState) {

        switch (operationalState) {
            case CHUNKIFYING:
                // => show failed items with operational state (CHUNKIFYING) regardless of item state (SUCCESS, FAILURE, IGNORED)
                if (itemState == null) {
                    view.addFailedItem(buildFailedItemModel(itemCompletionState, chunkId));
                    // => show failed items with operational state (CHUNKIFYING), with specific item state
                } else if (itemState == itemCompletionState.getChunkifyState()) {
                    view.addFailedItem(buildFailedItemModel(itemCompletionState, chunkId));
                }
                break;
            case PROCESSING:
                // => show failed items with operational state (PROCESSING) regardless of item state
                if (itemState == null) {
                    view.addFailedItem(buildFailedItemModel(itemCompletionState, chunkId));
                }
                // => show all failed items with operational state (PROCESSING), with specific item state
                else if (itemState == itemCompletionState.getProcessingState()) {
                    view.addFailedItem(buildFailedItemModel(itemCompletionState, chunkId));
                }
                break;
            case DELIVERING:
                // => show all failed items with operational state (DELIVERING) regardless of item state
                if (itemState == null) {
                    view.addFailedItem(buildFailedItemModel(itemCompletionState, chunkId));
                }
                // => show all failed items with operational state (DELIVERING), with specific item state
                else if (itemState == itemCompletionState.getDeliveryState()) {
                    view.addFailedItem(buildFailedItemModel(itemCompletionState, chunkId));
                }
                break;
        }
    }

    /**
     * This method builds a FailedItemModel with given input
     *
     * @param itemCompletionState The ItemCompletionState
     * @param chunkId             The Chunk ID
     * @return FailedItemModel
     */
    private FailedItemModel buildFailedItemModel(ItemCompletionState itemCompletionState, String chunkId) {
        FailedItemModel failedItemModel = new FailedItemModel();
        failedItemModel.setJobId(Long.toString(jobId));
        failedItemModel.setChunkId(chunkId);
        failedItemModel.setItemId(Long.toString(itemCompletionState.getItemId()));
        failedItemModel.setChunkifyState(state2String(itemCompletionState.getChunkifyState()));
        failedItemModel.setProcessingState(state2String(itemCompletionState.getProcessingState()));
        failedItemModel.setDeliveryState(state2String(itemCompletionState.getDeliveryState()));
        return failedItemModel;
    }

    /**
     * This method converts an ItemCompletionState to a String text
     *
     * @param state The ItemCompletionState
     * @return The ItemCompletionState as a string
     */
    protected String state2String(ItemCompletionState.State state) {
        switch (state) {
            case IGNORED:
                return texts.status_ignored();
            case SUCCESS:
                return texts.status_success();
            case FAILURE:
                return texts.status_failure();
            case INCOMPLETE:
                return texts.status_incomplete();
            default:
                return "";
        }
    }


    /*
     * Private classes
     */

    class GetAllFailedItemsCallback extends FilteredAsyncCallback<List<JobInfo>> {
        @Override
        public void onFilteredFailure(Throwable caught) {
            view.setErrorText(texts.error_CouldNotFetchJobs());
        }

        @Override
        public void onSuccess(List<JobInfo> jobInfos) {
            getAllJobCompletionStatus(jobInfos);
        }
    }

    protected class GetJobCompletionStatusCallback extends FilteredAsyncCallback<JobCompletionState> {
        @Override
        public void onFilteredFailure(Throwable caught) {
            view.setErrorText(texts.error_CouldNotFetchJobCompletionStatusFor() + Long.toString(jobId));
        }

        @Override
        public void onSuccess(JobCompletionState jobCompletionState) {
            addJobCompletionStateToView(jobCompletionState);
        }
    }

}
