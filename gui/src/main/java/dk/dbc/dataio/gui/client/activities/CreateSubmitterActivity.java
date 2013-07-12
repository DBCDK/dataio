/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client.activities;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.model.SubmitterData;
import dk.dbc.dataio.gui.client.places.SubmitterCreatePlace;
import dk.dbc.dataio.gui.util.ClientFactory;
import dk.dbc.dataio.gui.client.views.SubmitterCreateView;
import dk.dbc.dataio.gui.client.views.SubmitterCreateViewImpl;

/**
 *
 * @author slf
 */
public class CreateSubmitterActivity extends AbstractActivity implements SubmitterCreateView.Presenter {
    ClientFactory clientFactory;
//    private SubmitterStoreProxyAsync submitterStoreProxy = SubmitterStoreProxy.Factory.getAsyncInstance();

    public CreateSubmitterActivity(SubmitterCreatePlace place, ClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public void reload() {
		this.clientFactory.getSubmitterCreateView().refresh();
    }

    @Override
    public void saveSubmitter(String name, String number, String description) {
        final SubmitterCreateView view = this.clientFactory.getSubmitterCreateView();

        SubmitterData submitterData = new SubmitterData();
        submitterData.setSubmitterName(name);
        submitterData.setSubmitterNumber(number);
        submitterData.setDescription(description);

        view.displaySuccess(SubmitterCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE);   // NB: Midlertidig - erstattes af kald til proxy (se herunder)
//        submitterStoreProxy.createSubmitter(submitterData, new AsyncCallback<Void>() {
//            @Override
//            public void onFailure(Throwable e) {
//                String errorClassName = e.getClass().getName();
//                view.displayError(errorClassName + " - " + e.getMessage());
//            }
//
//            @Override
//            public void onSuccess(Void aVoid) {
//                view.displaySuccess(SubmitterCreateViewImpl.SAVE_RESULT_LABEL_SUCCES_MESSAGE);
//            }
//        });
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        SubmitterCreateView submitterCreateView = clientFactory.getSubmitterCreateView();
        submitterCreateView.setPresenter(this);
        containerWidget.setWidget(submitterCreateView.asWidget());
    }

}
