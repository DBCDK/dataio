package dk.dbc.dataio.gui.client.pages.basemaintenance;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.gui.client.util.Format;


/**
 * This class represents the show ftp's presenter implementation
 */
public class PresenterImpl extends AbstractActivity implements Presenter {
    private static final String TRACE_ID = "TRACEID";

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    private String urlElk = null;

    /**
     * Default constructor
     */
    public PresenterImpl() {
        commonInjector.getUrlResolverProxyAsync().getUrl("ELK_URL",
                new AsyncCallback<String>() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        viewInjector.getView().setErrorText(viewInjector.getTexts().error_JndiElkUrlFetchError());
                    }

                    @Override
                    public void onSuccess(String jndiUrl) {
                        urlElk = jndiUrl;
                    }
                });
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
        View view = viewInjector.getView();
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
    }


    /**
     * Opens a new tab in the browser, containing a search in ELK for the item with trackingId as supplied
     *
     * @param trackingId The tracking ID to search for
     */
    @Override
    public void traceItem(String trackingId) {
        if (urlElk != null) {
            Window.open(Format.macro(urlElk, TRACE_ID, trackingId), "_blank", "");
        }
    }

}

