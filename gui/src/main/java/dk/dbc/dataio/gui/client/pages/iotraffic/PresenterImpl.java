package dk.dbc.dataio.gui.client.pages.iotraffic;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.util.CommonGinjector;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Concrete Presenter Implementation Class for Io Traffic
 */
public class PresenterImpl extends AbstractActivity implements Presenter {

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    FlowStoreProxyAsync flowStoreProxy = commonInjector.getFlowStoreProxyAsync();

    protected String header;

    String submitter = "";
    String packaging = "";
    String format = "";
    String destination = "";

    public PresenterImpl(String header) {
        this.header = header;
    }

    /**
     * start method
     * Is called by PlaceManager, whenever the PlaceCreate or PlaceEdit are being invoked
     * This method is the start signal for the presenter
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        containerWidget.setWidget(getView().asWidget());
        getView().setHeader(this.header);
        getView().setPresenter(this);
        initializeData();
    }

    /*
     * Indications from the View
     */
    @Override
    public void submitterChanged(String submitter) {
        this.submitter = submitter;
    }


    @Override
    public void packagingChanged(String packaging) {
        this.packaging = packaging;
    }

    @Override
    public void formatChanged(String format) {
        this.format = format;
    }

    @Override
    public void destinationChanged(String destination) {
        this.destination = destination;
    }

    @Override
    public void addButtonPressed() {
        if (submitter.isEmpty() || packaging.isEmpty() || format.isEmpty() || destination.isEmpty()) {
            getView().displayWarning(getTexts().error_InputFieldValidationError());
        } else {
            flowStoreProxy.createGatekeeperDestination(
                    new GatekeeperDestination(0L, submitter, destination, packaging, format),
                    new CreateGatekeeperDestinationCallback()
            );
        }
    }

    @Override
    public void deleteButtonPressed(long gatekeeperId) {
        flowStoreProxy.deleteGatekeeperDestination(gatekeeperId, new DeleteGatekeeperDestinationCallback());
    }

    /*
     * Local methods
     */

    protected View getView() {
        return viewInjector.getView();
    }

    protected Texts getTexts() {
        return viewInjector.getTexts();
    }

    private void initializeData() {
        submitter = "";
        getView().submitter.clearText();
        packaging = "";
        getView().packaging.clearText();
        format = "";
        getView().format.clearText();
        destination = "";
        getView().destination.clearText();
        flowStoreProxy.findAllGatekeeperDestinations(new FindAllGateKeeperDestinationsCallback());
    }

    /*
     * Local classes
     */
    class CreateGatekeeperDestinationCallback implements AsyncCallback<GatekeeperDestination> {
        @Override
        public void onFailure(Throwable throwable) {
            getView().displayWarning(getTexts().error_CannotCreateGatekeeperDestination());
        }

        @Override
        public void onSuccess(GatekeeperDestination destination) {
            initializeData();
        }
    }

    class FindAllGateKeeperDestinationsCallback implements AsyncCallback<List<GatekeeperDestination>> {
        @Override
        public void onFailure(Throwable throwable) {
            getView().displayWarning(getTexts().error_CannotFetchGatekeeperDestinations());
        }

        @Override
        public void onSuccess(List<GatekeeperDestination> gatekeeperDestinations) {
            getView().setGatekeepers(doSort(gatekeeperDestinations));
        }
    }

    class DeleteGatekeeperDestinationCallback implements AsyncCallback<Void> {

        @Override
        public void onFailure(Throwable caught) {
            getView().displayWarning(getTexts().error_CannotDeleteGatekeeperDestination());
        }

        @Override
        public void onSuccess(Void result) {
            initializeData();
        }
    }


    /*
     * Private methods
     */
    List<GatekeeperDestination> doSort(List<GatekeeperDestination> destinations) {
        // Tertiary sort key: Sort according to format
        Collections.sort(destinations, Comparator.comparing(GatekeeperDestination::getFormat));
        // Secondary sort key: Sort according to packaging
        Collections.sort(destinations, Comparator.comparing(GatekeeperDestination::getPackaging));
        // Primary sort key: Sort according to submitter
        Collections.sort(destinations, Comparator.comparing(p -> Long.valueOf(p.getSubmitterNumber())));
        return destinations;
    }

}


