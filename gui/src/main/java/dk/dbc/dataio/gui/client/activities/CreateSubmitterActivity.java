package dk.dbc.dataio.gui.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.model.SubmitterData;
import dk.dbc.dataio.gui.client.places.SubmitterCreatePlace;
import dk.dbc.dataio.gui.client.presenters.SubmitterCreatePresenter;
import dk.dbc.dataio.gui.client.proxy.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.views.SubmitterCreateView;
import dk.dbc.dataio.gui.client.views.SubmitterCreateViewImpl;
import dk.dbc.dataio.gui.util.ClientFactory;

/**
 * This class represents the create submitter activity encompassing saving
 * of submitter data in the flow store via RPC proxy
 */
public class CreateSubmitterActivity extends AbstractActivity implements SubmitterCreatePresenter {
    private ClientFactory clientFactory;
    private SubmitterCreateView submitterCreateView;
    private FlowStoreProxyAsync flowStoreProxy;

    public CreateSubmitterActivity(SubmitterCreatePlace place, ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
        flowStoreProxy = clientFactory.getFlowStoreProxyAsync();
    }

    @Override
    public void bind() {
        submitterCreateView = clientFactory.getSubmitterCreateView();
        submitterCreateView.setPresenter(this);
    }

    @Override
    public void reload() {
		submitterCreateView.refresh();
    }

    @Override
    public void saveSubmitter(String name, String number, String description) {
        final SubmitterData submitterData = new SubmitterData();
        submitterData.setSubmitterName(name);
        submitterData.setSubmitterNumber(number);
        submitterData.setDescription(description);

        submitterCreateView.displaySuccess(SubmitterCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE);   // NB: Midlertidig - erstattes af kald til proxy (se herunder)
//        flowStoreProxy.createSubmitter(submitterData, new AsyncCallback<Void>() {
//            @Override
//            public void onFailure(Throwable e) {
//                final String errorClassName = e.getClass().getName();
//                submitterCreateView.displayError(errorClassName + " - " + e.getMessage());
//            }
//
//            @Override
//            public void onSuccess(Void aVoid) {
//                submitterCreateView.displaySuccess(SubmitterCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE);
//            }
//        });
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        bind();
        containerWidget.setWidget(submitterCreateView.asWidget());
    }

}
