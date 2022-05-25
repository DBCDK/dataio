package dk.dbc.dataio.gui.client.pages.job.purge;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;

/**
 * Presenter Implementation Class for Job Purge
 */
public class PresenterImpl extends AbstractActivity implements Presenter {

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    protected String header;

    /**
     * Constructor
     *
     * @param header Breadcrumb header text
     */
    public PresenterImpl(String header) {
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
        viewInjector.getView().setPresenter(this);
        containerWidget.setWidget(viewInjector.getView().asWidget());
        viewInjector.getView().setHeader(this.header);
    }

}
