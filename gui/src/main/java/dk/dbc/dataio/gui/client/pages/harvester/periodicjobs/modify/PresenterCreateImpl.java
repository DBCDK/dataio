package dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.modify;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.harvester.types.FtpPickup;
import dk.dbc.dataio.harvester.types.HttpPickup;
import dk.dbc.dataio.harvester.types.MailPickup;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.types.SFtpPickup;

public class PresenterCreateImpl<Place extends CreatePlace> extends PresenterImpl {
    public PresenterCreateImpl(String header) {
        super(header);
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        super.start(containerWidget, eventBus);
        handlePickupType(PeriodicJobsHarvesterConfig.PickupType.ANY_SINK);
    }

    @Override
    public void pickupTypeChanged(PeriodicJobsHarvesterConfig.PickupType pickupType) {
        super.pickupTypeChanged(pickupType);
        handlePickupType(pickupType);
    }

    @Override
    public void harvesterTypeChanged(PeriodicJobsHarvesterConfig.HarvesterType harvesterType) {
        super.harvesterTypeChanged(harvesterType);
        handleHarvesterType(harvesterType);
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
                        .withHoldingsSolrUrl("")
                        .withHoldingsFilter(PeriodicJobsHarvesterConfig.HoldingsFilter.WITH_HOLDINGS)
                        .withEnabled(false));
        setConfig(config);
    }

    @Override
    void saveModel() {
        commonInjector.getFlowStoreProxyAsync().createPeriodicJobsHarvesterConfig(
                config, new CreateHarvesterConfigAsyncCallback());
    }

    @Override
    public void runButtonPressed() {
    }

    @Override
    public void refreshButtonPressed() {
    }

    @Override
    public void validateSolrButtonPressed() {

    }

    private void handlePickupType(PeriodicJobsHarvesterConfig.PickupType pickupType) {
        final View view = getView();
        view.pickupTypeSelection.setEnabled(true);
        view.overrideFilename.setVisible(false);
        view.httpSection.setVisible(false);
        view.mailSection.setVisible(false);
        view.ftpSection.setVisible(false);
        view.sftpSection.setVisible(false);
        view.contentHeader.setVisible(true);
        view.contentFooter.setVisible(true);
        view.queryStatus.setVisible(false);
        if (pickupType == PeriodicJobsHarvesterConfig.PickupType.HTTP) {
            config.getContent().withPickup(new HttpPickup());
            view.overrideFilename.setVisible(true);
            view.httpSection.setVisible(true);
        } else if (pickupType == PeriodicJobsHarvesterConfig.PickupType.MAIL) {
            config.getContent().withPickup(new MailPickup());
            view.mailSection.setVisible(true);
            view.overrideFilename.setVisible(true);
        } else if (pickupType == PeriodicJobsHarvesterConfig.PickupType.FTP) {
            config.getContent().withPickup(new FtpPickup());
            view.overrideFilename.setVisible(true);
            view.ftpSection.setVisible(true);
        } else if (pickupType == PeriodicJobsHarvesterConfig.PickupType.SFTP) {
            config.getContent().withPickup(new SFtpPickup());
            view.overrideFilename.setVisible(true);
            view.sftpSection.setVisible(true);
        } else {
            config.getContent().withPickup(null);
            view.contentHeader.setVisible(false);
            view.contentFooter.setVisible(false);
        }
    }

    private void handleHarvesterType(PeriodicJobsHarvesterConfig.HarvesterType harvesterType) {
        final View view = getView();

        view.holdingsSection.setVisible(false);
        if (harvesterType == PeriodicJobsHarvesterConfig.HarvesterType.STANDARD_WITH_HOLDINGS) {
            view.holdingsSection.setVisible(true);
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
