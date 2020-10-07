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
import dk.dbc.dataio.harvester.types.FtpPickup;
import dk.dbc.dataio.harvester.types.SFtpPickup;
import dk.dbc.dataio.harvester.types.HttpPickup;
import dk.dbc.dataio.harvester.types.MailPickup;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;

public class PresenterCreateImpl<Place extends CreatePlace> extends PresenterImpl {
    public PresenterCreateImpl(String header) {
        super(header);
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        super.start(containerWidget, eventBus);
        handlePickupType(PeriodicJobsHarvesterConfig.PickupType.HTTP);
    }

    @Override
    public void pickupTypeChanged(PeriodicJobsHarvesterConfig.PickupType pickupType) {
        super.pickupTypeChanged(pickupType);
        handlePickupType(pickupType);
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

    @Override
    public void runButtonPressed() {}
    private void handlePickupType(PeriodicJobsHarvesterConfig.PickupType pickupType) {
        final View view = getView();
        view.pickupTypeSelection.setEnabled(true);
        view.overrideFilename.setVisible(false);
        view.httpSection.setVisible(false);
        view.mailSection.setVisible(false);
        view.ftpSection.setVisible(false);
        view.sftpSection.setVisible(false);
        view.contentFooter.setVisible(false);
        view.contentHeader.setVisible(false);
        view.contentFooter.setVisible(false);
        if (pickupType == PeriodicJobsHarvesterConfig.PickupType.HTTP) {
            config.getContent().withPickup(new HttpPickup());
            view.overrideFilename.setVisible(true);
            view.httpSection.setVisible(true);
        } else if (pickupType == PeriodicJobsHarvesterConfig.PickupType.MAIL) {
            config.getContent().withPickup(new MailPickup());
            view.mailSection.setVisible(true);
            view.contentHeader.setVisible(true);
            view.contentFooter.setVisible(true);
        } else if (pickupType == PeriodicJobsHarvesterConfig.PickupType.FTP){
            config.getContent().withPickup(new FtpPickup());
            view.overrideFilename.setVisible(true);
            view.ftpSection.setVisible(true);
        } else {
            config.getContent().withPickup(new SFtpPickup());
            view.overrideFilename.setVisible(true);
            view.sftpSection.setVisible(true);
        }


    }

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
