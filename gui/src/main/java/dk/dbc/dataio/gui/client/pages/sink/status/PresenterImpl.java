package dk.dbc.dataio.gui.client.pages.sink.status;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.components.jobfilter.JobStatusFilter;
import dk.dbc.dataio.gui.client.components.jobfilter.SinkJobFilter;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.pages.job.show.ShowJobsPlace;
import dk.dbc.dataio.gui.client.util.CommonGinjector;

import java.util.List;


/**
 * This class represents the show sinks presenter implementation
 */
public class PresenterImpl extends AbstractActivity implements Presenter {

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);
    private PlaceController placeController;
    private String header;

    /**
     * Default constructor
     *
     * @param placeController The Placecontroller
     * @param header          breadcrumb Header text
     */
    public PresenterImpl(PlaceController placeController, String header) {
        this.placeController = placeController;
        this.header = header;
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
        getView().setHeader(this.header);
        getView().setPresenter(this);
        containerWidget.setWidget(getView().asWidget());
        fetchSinkStatus();
    }


    /*
     * Public interface methods
     */

    /**
     * This method links to the Jobs view, showing only sinks with the given id, and highlights the earliest active job
     *
     * @param sinkId The sink to use as the key for the filtering
     */
    public void showJobsFilteredBySink(long sinkId) {
        ShowJobsPlace showJobsPlace = new ShowJobsPlace(
                SinkJobFilter.class.getSimpleName() + "=" + sinkId +                          // "SinkJobFilter=xxx"
                        "&" + JobStatusFilter.class.getSimpleName() +                                       // "&ActiveJobFilter"
                        "&" + dk.dbc.dataio.gui.client.pages.job.show.PresenterImpl.SHOW_EARLIEST_ACTIVE);  // "&ShowEarliestActive"
        placeController.goTo(showJobsPlace);
    }

    /**
     * This method fetches all sinks, and sends them to the view
     */
    public void fetchSinkStatus() {
        commonInjector.getJobStoreProxyAsync().getSinkStatusModels(new GetSinkStatusListFilteredAsyncCallback());
    }



    /*
     * Private stuff
     */

    private View getView() {
        return viewInjector.getView();
    }

    class GetSinkStatusListFilteredAsyncCallback extends FilteredAsyncCallback<List<SinkStatusTable.SinkStatusModel>> {
        @Override
        public void onFilteredFailure(Throwable throwable) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromJobStoreProxy(throwable, commonInjector.getProxyErrorTexts(), null));
        }

        @Override
        public void onSuccess(List<SinkStatusTable.SinkStatusModel> sinkStatusSnapshots) {
            getView().setSinkStatus(sinkStatusSnapshots);
        }

    }

}
