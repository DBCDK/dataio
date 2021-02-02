
/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.gui.client.pages.harvester.periodicjobs.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.jndi.RawRepo;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.gui.client.util.Format;
import dk.dbc.dataio.harvester.types.FtpPickup;
import dk.dbc.dataio.harvester.types.HttpPickup;
import dk.dbc.dataio.harvester.types.MailPickup;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.types.Pickup;
import dk.dbc.dataio.harvester.types.SFtpPickup;


public abstract class PresenterImpl extends AbstractActivity implements Presenter {
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    protected String header;
    protected PeriodicJobsHarvesterConfig config = null;

    public PresenterImpl(String header) {
        this.header = header;
    }

    abstract void initializeModel();
    abstract void saveModel();

    /**
     * Called by PlaceManager whenever the PlaceCreate or PlaceEdit is invoked.
     * This method is the start signal for the presenter.
     * @param containerWidget the widget to use
     * @param eventBus the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        initializeView();
        containerWidget.setWidget(getView().asWidget());
        initializeModel();
    }

    @Override
    public void pickupTypeChanged(PeriodicJobsHarvesterConfig.PickupType pickupType) {
        View view = getView();
        view.httpSection.setVisible(false);
        view.mailSection.setVisible(false);
        view.ftpSection.setVisible(false);
        view.sftpSection.setVisible(false);
        if (pickupType == PeriodicJobsHarvesterConfig.PickupType.MAIL) {
            view.mailSection.setVisible(true);
        } else if (pickupType == PeriodicJobsHarvesterConfig.PickupType.HTTP) {
            view.httpSection.setVisible(true);
        } else if (pickupType == PeriodicJobsHarvesterConfig.PickupType.FTP) {
            view.ftpSection.setVisible(true);
        } else if (pickupType == PeriodicJobsHarvesterConfig.PickupType.SFTP) {
            view.sftpSection.setVisible(true);
        } 
    }

    @Override
    public void harvesterTypeChanged(PeriodicJobsHarvesterConfig.HarvesterType harvesterType) {
        if (config != null) {
            config.getContent().withHarvesterType(harvesterType);
        }
    }

    @Override
    public void nameChanged(String name) {
        if (config != null) {
            config.getContent().withName(name);
        }
    }

    @Override
    public void scheduleChanged(String schedule) {
        if (config != null) {
            config.getContent().withSchedule(schedule);
        }
    }

    @Override
    public void descriptionChanged(String description) {
        if (config != null) {
            config.getContent().withDescription(description);
        }
    }

    @Override
    public void resourceChanged(String resource) {
        if (config != null) {
            final RawRepo rawRepo = RawRepo.fromString(resource);
            if (rawRepo != null && rawRepo.getJndiResourceName() != null) {
                config.getContent().withResource(rawRepo.getJndiResourceName());
            }
        }
    }

    @Override
    public void queryChanged(String query) {
        if (config != null) {
            config.getContent().withQuery(query);
        }
    }

    @Override
    public void collectionChanged(String collection) {
        if (config != null) {
            config.getContent().withCollection(collection);
        }
    }

    @Override
    public void destinationChanged(String destination) {
        if (config != null) {
            config.getContent().withDestination(destination);
        }
    }

    @Override
    public void formatChanged(String format) {
        if (config != null) {
            config.getContent().withFormat(format);
        }
    }

    @Override
    public void submitterChanged(String submitter) {
        if (config != null) {
            config.getContent().withSubmitterNumber(submitter);
        }
    }

    @Override
    public void contactChanged(String contact) {
        if (config != null) {
            config.getContent().withContact(contact);
        }
    }

    @Override
    public void timeOfLastHarvestChanged(String date) {
        if (config != null) {
            if (date == null || date.trim().isEmpty()) {
                config.getContent().withTimeOfLastHarvest(null);
            } else {
                config.getContent().withTimeOfLastHarvest(
                        Format.parseLongDateAsDate(date));
            }
        }
    }

    @Override
    public void overrideFilenameChanged(String overrideFilename) throws UnsupportedOperationException {
        if (config != null && !(config.getContent().getPickup() instanceof MailPickup)  ) {
            final Pickup pickup = config.getContent().getPickup();
            pickup.withOverrideFilename(overrideFilename);

        }
    }

    @Override
    public void contentHeaderChanged(String contentHeader) {
        if (config != null) {
            final Pickup pickup = config.getContent().getPickup();
            pickup.withContentHeader(contentHeader);
        }
    }

    @Override
    public void contentFooterChanged(String contentFooter) {
        if (config != null) {
            final Pickup pickup = config.getContent().getPickup();
            pickup.withContentFooter(contentFooter);

        }
    }

    @Override
    public void enabledChanged(Boolean enabled) {
        if (config != null) {
            config.getContent().withEnabled(enabled);
        }
    }

    @Override
    public void httpReceivingAgencyChanged(String agency) {
        if (config != null) {
            final HttpPickup pickup = (HttpPickup) config.getContent().getPickup();
            pickup.withReceivingAgency(agency);
        }
    }

    @Override
    public void mailRecipientsChanged(String mailRecipient) {
        if (config != null) {
            final MailPickup pickup = (MailPickup) config.getContent().getPickup();
            pickup.withRecipients(mailRecipient);
        }
    }

    @Override
    public void mailSubjectChanged(String subject) {
        if (config != null) {
            final MailPickup pickup = (MailPickup) config.getContent().getPickup();
            pickup.withSubject(subject);
        }
    }

    @Override
    public void mailMimetypeChanged(String mimetype) {
        if (config != null) {
            final MailPickup pickup = (MailPickup) config.getContent().getPickup();
            pickup.withMimetype(mimetype);
        }
    }

    @Override
    public void mailRecordLimitChanged(String recordLimit) {
        if (config != null) {
            final MailPickup pickup = (MailPickup) config.getContent().getPickup();
            pickup.withRecordLimit(recordLimit);
        }
    }

    @Override
    public void ftpAddressChanged(String subject) {
        if (config != null) {
            final FtpPickup pickup = (FtpPickup) config.getContent().getPickup();
            pickup.withFtpHost(subject);
        }
    }

    @Override
    public void ftpUserChanged(String user) {
        if (config != null) {
            final FtpPickup pickup = (FtpPickup) config.getContent().getPickup();
            pickup.withFtpUser(user);
        }
    }

    @Override
    public void ftpPasswordChanged(String pasword) {
        if (config != null) {
            final FtpPickup pickup = (FtpPickup) config.getContent().getPickup();
            pickup.withFtpPassword(pasword);
        }
    }

    @Override
    public void ftpSubdirChanged(String subdir) {
        if (config != null) {
            final FtpPickup pickup = (FtpPickup) config.getContent().getPickup();
            pickup.withFtpSubdirectory(subdir);
        }
    }

    @Override
    public void sftpAddressChanged(String subject) {
        if (config != null) {
            final SFtpPickup pickup = (SFtpPickup) config.getContent().getPickup();
            pickup.withSFtpHost(subject);
        }
    }

    @Override
    public void sFtpUserChanged(String user) {
        if (config != null) {
            final SFtpPickup pickup = (SFtpPickup) config.getContent().getPickup();
            pickup.withSFtpuser(user);
        }
    }

    @Override
    public void sftpPasswordChanged(String pasword) {
        if (config != null) {
            final SFtpPickup pickup = (SFtpPickup) config.getContent().getPickup();
            pickup.withSFtpPassword(pasword);
        }
    }

    @Override
    public void sftpSubdirChanged(String subdir) {
        if (config != null) {
            final SFtpPickup pickup = (SFtpPickup) config.getContent().getPickup();
            pickup.withSFtpSubdirectory(subdir);
        }
    }
    
    /**
     * A signal to the presenter, saying that a key has been pressed in either of the fields
     */
    @Override
    public void keyPressed() {
        if (config != null) {
            getView().status.setText("");
        }
    }

    /**
     * A signal to the presenter, saying that the save button has been pressed
     */
    @Override
    public void saveButtonPressed() {
        if (isIllegalResource()) {
            getView().setErrorText(getTexts().error_IllegalResourceValidationError());
            return;
        } else if (isInputFieldMissing()) {
            getView().setErrorText(getTexts().error_InputFieldValidationError());
            return;
        }
        saveModel();
    }


    /**
     * Sets the model after a successful save
     * @param config the config to set
     */
    void setConfig(PeriodicJobsHarvesterConfig config) {
        this.config = config;
        setViewFields();
    }

    private void initializeView() {
        final View view = getView();
        view.setHeader(this.header);
        view.setPresenter(this);
        view.pickupTypeSelection.setEnabled(true);
        view.name.setEnabled(true);
        view.schedule.setEnabled(true);
        view.description.setEnabled(true);
        view.resource.setEnabled(true);
        view.query.setEnabled(true);
        view.collection.setEnabled(true);
        view.destination.setEnabled(true);
        view.format.setEnabled(true);
        view.submitter.setEnabled(true);
        view.contact.setEnabled(true);
        view.contentFooter.setEnabled(true);
        view.contentHeader.setEnabled(true);
        view.enabled.setEnabled(true);
    }

    private void setViewFields() {
        final View view = getView();
        final PeriodicJobsHarvesterConfig.Content configContent = config.getContent();
        final Pickup pickup = configContent.getPickup();
        if (pickup == null) {
            view.contentFooter.setVisible(false);
            view.contentHeader.setVisible(false);
            view.pickupTypeSelection.setSelectedValue(PeriodicJobsHarvesterConfig.PickupType.ANY_SINK.name());
        } else if (pickup instanceof HttpPickup) {
            view.pickupTypeSelection.setSelectedValue(PeriodicJobsHarvesterConfig.PickupType.HTTP.name());
        } else if (pickup instanceof MailPickup) {
            view.pickupTypeSelection.setSelectedValue(PeriodicJobsHarvesterConfig.PickupType.MAIL.name());
        } else if (pickup instanceof FtpPickup) {
            view.pickupTypeSelection.setSelectedValue(PeriodicJobsHarvesterConfig.PickupType.FTP.name());
        } else if (pickup instanceof SFtpPickup) {
            view.pickupTypeSelection.setSelectedValue(PeriodicJobsHarvesterConfig.PickupType.SFTP.name());
        }
        view.harvesterTypeSelection.setSelectedValue(configContent.getHarvesterType().name());
        view.name.setText(configContent.getName());
        view.schedule.setText(configContent.getSchedule());
        view.description.setText(configContent.getDescription());
        view.resource.setText(getResourceName());
        view.query.setText(configContent.getQuery());
        view.collection.setText(configContent.getCollection());
        view.destination.setText(configContent.getDestination());
        view.format.setText(configContent.getFormat());
        view.submitter.setText(configContent.getSubmitterNumber());
        view.contact.setText(configContent.getContact());
        view.timeOfLastHarvest.setValue(getTimeOfLastHarvest());
        if (pickup != null) {
            view.overrideFilename.setValue(configContent.getPickup().getOverrideFilename());
            view.contentHeader.setValue(configContent.getPickup().getContentHeader());
            view.contentFooter.setValue(configContent.getPickup().getContentFooter());
        }
        view.enabled.setValue(configContent.isEnabled());
        view.status.setText("");
    }

    private String getTimeOfLastHarvest() {
        if (config.getContent().getTimeOfLastHarvest() != null) {
            return config.getContent().getTimeOfLastHarvest().toString();
        }
        return null;
    }

    private String getResourceName() {
       if (config.getContent().getResource() != null) {
           final String resource = config.getContent().getResource();
           if (resource != null) {
               final RawRepo rawRepo = RawRepo.fromString(resource);
               if (rawRepo != null) {
                   return rawRepo.name().toLowerCase();
               }
           }
       }
       return null;
    }

    private boolean isInputFieldMissing() {
        return config == null
                || config.getContent() == null
                || isUndefined(config.getContent().getName())
                || isUndefined(config.getContent().getSchedule())
                || isUndefined(config.getContent().getDescription())
                || isUndefined(config.getContent().getResource())
                || isUndefined(config.getContent().getQuery())
                || isUndefined(config.getContent().getCollection())
                || isUndefined(config.getContent().getDestination())
                || isUndefined(config.getContent().getFormat())
                || isUndefined(config.getContent().getContact())
                || isInputFieldMissingFromPickup();
    }

    private boolean isInputFieldMissingFromPickup() {
        final Pickup pickup = config.getContent().getPickup();
        if (pickup == null) {
            return false;
        }
        if (pickup instanceof HttpPickup) {
            return isUndefined(((HttpPickup) pickup).getReceivingAgency());
        }
        return false;
    }

    private boolean isIllegalResource() {
        final String resourceValue = getResourceValueFromView();
        if (!isUndefined(resourceValue)) {
            return RawRepo.fromString(resourceValue) == null;
        }
        return false;
    }

    private boolean isUndefined(String value) {
        return value == null || value.trim().isEmpty();
    }

    protected View getView() {
        return viewInjector.getView();
    }

    protected Texts getTexts() {
        return viewInjector.getTexts();
    }

    String getResourceValueFromView() {
        return getView().resource.getText();
    }
}

