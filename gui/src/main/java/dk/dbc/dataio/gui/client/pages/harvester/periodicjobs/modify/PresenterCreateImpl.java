/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;

public class PresenterCreateImpl<Place extends CreatePlace> extends PresenterImpl {

    public PresenterCreateImpl(String header) {
        super(header);
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        super.start(containerWidget, eventBus);
    }

    @Override
    public void initializeModel() {
        final PeriodicJobsHarvesterConfig config = new PeriodicJobsHarvesterConfig(1, 1,
                new PeriodicJobsHarvesterConfig.Content()
                        .withName("")
                        .withSchedule("")
                        .withDescription("")
                        .withResource("")
                        .withQuery("")
                        .withCollection("")
                        .withDestination("")
                        .withFormat("")
                        .withSubmitterNumber("")
                        .withContact("")
                        .withEnabled(false));
        setConfig(config);
    }

    @Override
    void saveModel() {
        commonInjector.getFlowStoreProxyAsync().createPeriodicJobsHarvesterConfig(
                config, new CreateHarvesterConfigAsyncCallback());
    }

    /**
     * This has no implementation because "Create" does not have a delete button!
     */
    public void deleteButtonPressed() {}

    class CreateHarvesterConfigAsyncCallback implements AsyncCallback<PeriodicJobsHarvesterConfig> {
        @Override
        public void onFailure(Throwable e) {
            String msg = "HarvesterConfig.id: [new Harvester]";
            getView().setErrorText(ProxyErrorTranslator
                    .toClientErrorFromFlowStoreProxy(e, commonInjector.getProxyErrorTexts(), msg));
        }
        @Override
        public void onSuccess(PeriodicJobsHarvesterConfig harvesterConfig) {
            getView().status.setText(getTexts().status_ConfigSuccessfullySaved());
            History.back();
        }
    }
}
