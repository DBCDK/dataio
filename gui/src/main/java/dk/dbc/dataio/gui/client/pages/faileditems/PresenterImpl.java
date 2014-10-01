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
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
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


    public PresenterImpl(Place place, ClientFactory clientFactory, Texts texts) {
        this.clientFactory = clientFactory;
        this.texts = texts;
        placeController = clientFactory.getPlaceController();
        jobStoreProxy = clientFactory.getJobStoreProxyAsync();
        ShowPlace showPlace = (ShowPlace) place;
        jobId = showPlace.getjobId();
    }

    /**
     * start method
     * Is called by PlaceManager, whenever the Place is being invoked
     * This method is the start signal for the presenter
     * @param containerWidget the widget to use
     * @param eventBus the eventBus to use
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
     * Private methods
     *
     */

    protected void getAllFailedItems() {
        jobStoreProxy.findAllJobs(new GetAllFailedItemsCallback());
    }

    protected JavaScriptLogPlace constructJavaScriptLogPlace(FailedItemModel model) {
        return new JavaScriptLogPlace(
                Long.parseLong(model.getJobId()),
                Long.parseLong(model.getChunkId()),
                Long.parseLong(model.getItemId())
        );
    }

    protected void getAllJobCompletionStatus(List<JobInfo> jobInfos) {
        view.clearFailedItemsList();
        for (JobInfo jobInfo: jobInfos) {
            if (this.jobId == jobInfo.getJobId()) {
                getJobCompletionStatus(jobInfo.getJobId());
            }
        }
    }

    protected void getJobCompletionStatus(final long jobId) {
        jobStoreProxy.getJobCompletionState(jobId, new GetJobCompletionStatusCallback());
    }

    protected void addJobCompletionStateToView(JobCompletionState jobCompletionState) {
        String jobId = Long.toString(jobCompletionState.getJobId());
        for (ChunkCompletionState chunkCompletionState: jobCompletionState.getChunks()) {
            String chunkId = Long.toString((chunkCompletionState.getChunkId()));
            for (ItemCompletionState itemCompletionState: chunkCompletionState.getItems()) {
                String itemId = Long.toString(itemCompletionState.getItemId());
                ItemCompletionState.State state = itemCompletionState.getState();
                if (state != ItemCompletionState.State.SUCCESS) {
                    view.addFailedItem(new FailedItemModel(jobId, chunkId, itemId, state2String(itemCompletionState.getState())));
                }
            }
        }
    }

    protected String state2String(ItemCompletionState.State state) {
        switch (state) {
            case IGNORED: return texts.status_ignored();
            case SUCCESS: return texts.status_success();
            case FAILURE: return texts.status_failure();
            case INCOMPLETE: return texts.status_incomplete();
            default: return "";
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
