package dk.dbc.dataio.gui.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.gui.client.presenters.SubmittersShowPresenter;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.views.SubmittersShowView;
import dk.dbc.dataio.gui.util.ClientFactory;
import java.util.ArrayList;
import java.util.List;


/**
 * This class represents the show submitters activity
 */
public class ShowSubmittersActivity extends AbstractActivity implements SubmittersShowPresenter {
    private final ClientFactory clientFactory;
    private SubmittersShowView submittersShowView;
    private FlowStoreProxyAsync flowStoreProxy;

    public ShowSubmittersActivity(/*SubmittersShowPlace place,*/ ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
    }

    @Override
    public void bind() {
        submittersShowView = clientFactory.getSubmittersShowView();
        submittersShowView.setPresenter(this);
    }

    @Override
    public void reload() {
		submittersShowView.refresh();
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(submittersShowView.asWidget());
        fetchSubmitters();
    }


    // Local methods
    private void fetchSubmitters() {
        List<Submitter> submitters = new ArrayList<Submitter>();
        submitters.add(new Submitter(11, 111, new SubmitterContent(1111, "Submitter 1", "Description 1")));
        submitters.add(new Submitter(12, 122, new SubmitterContent(1222, "Submitter 2", "Description 2")));
        submitters.add(new Submitter(13, 133, new SubmitterContent(1333, "Submitter 3", "Description 3")));
        submittersShowView.setSubmitters(submitters);

//        flowStoreProxy.findAllSubmitters(new AsyncCallback<List<Submitter>>() {
//            @Override
//            public void onFailure(Throwable e) {
//                submittersShowView.onFailure(e.getClass().getName() + " - " + e.getMessage());
//            }
//            @Override
//            public void onSuccess(List<Submitter> submitters) {
//                submittersShowView.setSubmitters(submitters);
//            }
//        });
    }

}
