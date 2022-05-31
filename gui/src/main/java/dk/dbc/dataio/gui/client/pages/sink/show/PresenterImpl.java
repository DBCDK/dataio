package dk.dbc.dataio.gui.client.pages.sink.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.pages.sink.modify.CreatePlace;
import dk.dbc.dataio.gui.client.pages.sink.modify.EditPlace;
import dk.dbc.dataio.gui.client.util.CommonGinjector;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * This class represents the show sinks presenter implementation
 */
public class PresenterImpl extends AbstractActivity implements Presenter {

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    PlaceController placeController;
    private String header;

    /**
     * Default constructor
     *
     * @param placeController Common place controller
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
        fetchSinks();
    }


    /**
     * This method opens a new view, for editing the sink in question
     *
     * @param model The model for the sink to edit
     */
    @Override
    public void editSink(SinkModel model) {
        this.placeController.goTo(new EditPlace(model));
    }

    /**
     * This method opens a new view, for creating a new sink
     */
    @Override
    public void createSink() {
        getView().selectionModel.clear();
        this.placeController.goTo(new CreatePlace());
    }

    /*
     * Private methods
     */

    /**
     * This method fetches all sinks, and sends them to the view
     */
    private void fetchSinks() {
        commonInjector.getFlowStoreProxyAsync().findAllSinks(new FetchSinksCallback());
    }

    /**
     * This method deciphers if a sink has been added, updated or deleted.
     * The view and selection model are updated accordingly
     *
     * @param dataProviderSet The set of data already stored in the view
     * @param models          the list of sinks returned from flow store proxy
     */
    private void setSinksAndDecipherSelection(Set<SinkModel> dataProviderSet, List<SinkModel> models) {
        if (dataProviderSet.size() > models.size() || dataProviderSet.size() == 0) {
            getView().selectionModel.clear();
            getView().setSinks(models);
        } else {
            for (SinkModel current : models) {
                if (!dataProviderSet.contains(current)) {
                    getView().setSinks(models);
                    getView().selectionModel.setSelected(current, true);
                    break;
                }
            }
        }
    }

    /*
     * Private classes
     */

    /**
     * This class is the callback class for the findAllSinks method in the Flow Store
     */
    protected class FetchSinksCallback extends FilteredAsyncCallback<List<SinkModel>> {
        @Override
        public void onFilteredFailure(Throwable e) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), this.getClass().getCanonicalName()));
        }

        @Override
        public void onSuccess(List<SinkModel> models) {
            sortAccordingToNames(models);
            sortAccordingToTypes(models);
            setSinksAndDecipherSelection(new HashSet<>(getView().dataProvider.getList()), models);
        }
    }

    View getView() {
        return viewInjector.getView();
    }

    private void sortAccordingToNames(List<SinkModel> models) {
        Collections.sort(models, new Comparator<SinkModel>() {
            @Override
            public int compare(SinkModel o1, SinkModel o2) {
                return o1.getSinkName().toLowerCase().compareTo(o2.getSinkName().toLowerCase());
            }
        });
    }

    private void sortAccordingToTypes(List<SinkModel> models) {
        Collections.sort(models, new Comparator<SinkModel>() {
            @Override
            public int compare(SinkModel o1, SinkModel o2) {
                return o1.getSinkType().compareTo(o2.getSinkType());
            }
        });
    }

}
