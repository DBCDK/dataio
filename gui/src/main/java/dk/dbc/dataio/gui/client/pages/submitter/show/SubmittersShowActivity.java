package dk.dbc.dataio.gui.client.pages.submitter.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.pages.submitter.modify.EditPlace;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.List;


/**
 * This class represents the show submitters activity
 */
public class SubmittersShowActivity extends AbstractActivity implements SubmittersShowPresenter {
    private final ClientFactory clientFactory;
    private SubmittersShowView submittersShowView;
    private FlowStoreProxyAsync flowStoreProxy;
    private final PlaceController placeController;

    public SubmittersShowActivity(ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
        placeController = clientFactory.getPlaceController();
    }

    private void bind() {
        submittersShowView = clientFactory.getSubmittersShowView();
        submittersShowView.setPresenter(this);
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(submittersShowView.asWidget());
        fetchSubmitters();
    }

    /**
     * Creates a new place
     * @param submitter The submitter to edit
     */
    @Override
    public void editSubmitter(Submitter submitter) {
        placeController.goTo(new EditPlace(submitter));
    }

    // Local methods
    private void fetchSubmitters() {
        flowStoreProxy.findAllSubmitters(new FilteredAsyncCallback<List<Submitter>>() {
            @Override
            public void onFilteredFailure(Throwable e) {
                submittersShowView.setErrorText(e.getClass().getName() + " - " + e.getMessage());
            }

            @Override
            public void onSuccess(List<Submitter> submitters) {
                submittersShowView.setSubmitters(submitters);
            }
        });
    }

}
