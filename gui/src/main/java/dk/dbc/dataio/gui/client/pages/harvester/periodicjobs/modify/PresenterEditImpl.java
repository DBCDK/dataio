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
import dk.dbc.dataio.harvester.types.FtpPickup;
import dk.dbc.dataio.harvester.types.SFtpPickup;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.HttpPickup;
import dk.dbc.dataio.harvester.types.MailPickup;
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
        setLogMessage(getTexts().status_WaitForHarvesterStatus());
        commonInjector.getPeriodicJobsHarvesterProxy().executePeriodicJob(config.getId(),
                new RunPeriodicJobAsyncCallback());
    }

    private void handlePickupType() {
        if (config != null) {
            final View view = getView();
            view.overrideFilename.setVisible(false);
            view.mailSection.setVisible(false);
            view.httpSection.setVisible(false);
            view.ftpSection.setVisible(false);
            view.sftpSection.setVisible(false);
            view.contentFooter.setVisible(true);
            view.contentHeader.setVisible(true);
            if (config.getContent().getPickup() == null) {
                view.contentFooter.setVisible(false);
                view.contentHeader.setVisible(false);
                view.pickupTypeSelection.setValue(PeriodicJobsHarvesterConfig.PickupType.ANY_SINK.name());
            } else if (config.getContent().getPickup() instanceof HttpPickup) {
                final HttpPickup httpPickup = (HttpPickup) config.getContent().getPickup();
                view.overrideFilename.setVisible(true);
                view.httpReceivingAgency.setText(httpPickup.getReceivingAgency());
                view.httpSection.setVisible(true);
                view.pickupTypeSelection.setValue(PeriodicJobsHarvesterConfig.PickupType.HTTP.name());
            } else if (config.getContent().getPickup() instanceof MailPickup) {
                final MailPickup mailPickup = (MailPickup) config.getContent().getPickup();
                view.mailRecipient.setText(mailPickup.getRecipients());
                view.mailSubject.setText(mailPickup.getSubject());
                view.mailMimetype.setText(mailPickup.getMimetype());
                view.mailRecordLimit.setText(mailPickup.getRecordLimit() != null ? Integer.toString(mailPickup.getRecordLimit()) : "");
                view.mailSection.setVisible(true);
                view.pickupTypeSelection.setValue(PeriodicJobsHarvesterConfig.PickupType.MAIL.name());
            } else if (config.getContent().getPickup() instanceof  FtpPickup){
                final FtpPickup ftpPickup = (FtpPickup) config.getContent().getPickup();
                view.overrideFilename.setVisible(true);
                view.ftpAddress.setText(ftpPickup.getFtpHost());
                view.ftpUser.setText(ftpPickup.getFtpUser());
                view.ftpPassword.setText(ftpPickup.getFtpPassword());
                view.ftpSubdir.setText(ftpPickup.getFtpSubdirectory());
                view.ftpSection.setVisible(true);
                view.pickupTypeSelection.setValue(PeriodicJobsHarvesterConfig.PickupType.FTP.name());
            } else if (config.getContent().getPickup() instanceof SFtpPickup) {
                final SFtpPickup sftpPickup = (SFtpPickup) config.getContent().getPickup();
                view.overrideFilename.setVisible(true);
                view.sftpAddress.setText(sftpPickup.getsFtpHost());
                view.sFtpUser.setText(sftpPickup.getsFtpUser());
                view.sftpPassword.setText(sftpPickup.getsFtpPassword());
                view.sftpSubdir.setText(sftpPickup.getsFtpSubdirectory());
                view.sftpSection.setVisible(true);
                view.pickupTypeSelection.setValue(PeriodicJobsHarvesterConfig.PickupType.SFTP.name());
            }
            view.pickupTypeSelection.setEnabled(false);
        }
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
                handlePickupType();
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
            setLogMessage(getTexts().status_JobStartFailed()+throwable.getLocalizedMessage());
        }

        @Override
        public void onSuccess(Void aVoid) {
            setLogMessage(getTexts().status_JobSuccessfullyStarted());
        }
    }

    private void setLogMessage(String message) {
        LogPanel logPanel = ((ContentPanel) Document.get().getElementById(GUIID_CONTENT_PANEL).getPropertyObject(GUIID_CONTENT_PANEL)).getLogPanel();
        logPanel.clear();
        logPanel.showMessage(message);
    }
}
