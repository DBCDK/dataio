/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.modify;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.components.log.LogPanel;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;

import static dk.dbc.dataio.gui.client.views.ContentPanel.GUIID_CONTENT_PANEL;


public class PresenterEditImpl<Place extends EditPlace> extends PresenterImpl {
    private long id;

    public PresenterEditImpl(Place place, String header) {
        super(header);
        id = place.getHarvesterId();
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        super.start(containerWidget, eventBus);
        getView().deleteButton.setVisible(true);
    }

    @Override
    public void initializeModel() {
        commonInjector.getFlowStoreProxyAsync()
                .getPeriodicJobsHarvesterConfig(id, new GetPeriodicJobsHarvesterConfigAsyncCallback());
    }

    @Override
    void saveModel() {
        commonInjector.getFlowStoreProxyAsync()
                .updateHarvesterConfig(config, new UpdatePeriodicJobsHarvesterConfigAsyncCallback());
    }

    public void deleteButtonPressed() {
        commonInjector.getFlowStoreProxyAsync().deleteHarvesterConfig(config.getId(), config.getVersion(),
                new DeletePeriodicJobsHarvesterConfigAsyncCallback());
    }

    @Override
    public void runButtonPressed() {
        commonInjector.getPeriodicJobsHarvesterProxy().executePeriodicJob(config.getId(),
                new RunPeriodicJobAsyncCallback());
    }

    class GetPeriodicJobsHarvesterConfigAsyncCallback implements AsyncCallback<PeriodicJobsHarvesterConfig> {
        @Override
        public void onFailure(Throwable e) {
            String msg = "PeriodicJobsHarvesterConfig.id: " + id;
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(
                    e, commonInjector.getProxyErrorTexts(), msg));
        }
        @Override
        public void onSuccess(PeriodicJobsHarvesterConfig config) {
            if (config == null) {
                getView().setErrorText(getTexts().error_HarvesterNotFound());
            } else {
                setConfig(config);
            }
        }
    }

    class UpdatePeriodicJobsHarvesterConfigAsyncCallback implements AsyncCallback<HarvesterConfig> {
        @Override
        public void onFailure(Throwable e) {
            String msg = "PeriodicJobsHarvesterConfig.id: " + id;
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(
                    e, commonInjector.getProxyErrorTexts(), msg));
        }
        @Override
        public void onSuccess(HarvesterConfig config) {
            getView().status.setText(getTexts().status_ConfigSuccessfullySaved());
            History.back();
        }
    }

    class DeletePeriodicJobsHarvesterConfigAsyncCallback implements AsyncCallback<Void> {
        @Override
            public void onFailure(Throwable e) {
                String msg = "PeriodicJobsHarvesterConfig.id: " + config.getId();
                getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFlowStoreProxy(
                        e, commonInjector.getProxyErrorTexts(), msg));
            }
            @Override
            public void onSuccess(Void aVoid) {
                getView().status.setText(getTexts().status_ConfigSuccessfullyDeleted());
                History.back();
            }
    }

    class RunPeriodicJobAsyncCallback implements AsyncCallback<Void> {

        @Override
        public void onFailure(Throwable throwable) {
            setLogMessage("Kørsel fejlede med:"+throwable.getLocalizedMessage());
        }

        @Override
        public void onSuccess(Void aVoid) {
            setLogMessage("Kørsel med høster config '"+config.getContent().getName()+"' er startet.");
        }
    }

    private void setLogMessage(String message) {
        LogPanel logPanel = ((ContentPanel) Document.get().getElementById(GUIID_CONTENT_PANEL).getPropertyObject(GUIID_CONTENT_PANEL)).getLogPanel();
        logPanel.clear();
        logPanel.showMessage(message);
    }
}
