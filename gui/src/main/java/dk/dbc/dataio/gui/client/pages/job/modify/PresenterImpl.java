/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gui.client.pages.job.modify;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.HarvesterToken;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.gui.client.exceptions.texts.LogMessageTexts;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.util.CommonGinjector;

/**
 * Abstract Presenter Implementation Class for Submitter Create and Edit
 */
public abstract class PresenterImpl extends AbstractActivity implements Presenter {

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    // Application Models
    protected JobModel jobModel = new JobModel();
    protected String header;
    Texts texts;
    LogMessageTexts logMessageTexts;
    private View view;


    /**
     * Constructor
     * Please note, that in the constructor, view has NOT been initialized and can therefore not be used
     * Put code, utilizing view in the start method
     *
     * @param header    Breadcrumb header text
     */
    public PresenterImpl(String header) {
        this.header = header;
    }

    /**
     * start method
     * Is called by PlaceManager, whenever the PlaceCreate or PlaceEdit are being invoked
     * This method is the start signal for the presenter
     * @param containerWidget the widget to use
     * @param eventBus the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        texts = getTexts();
        logMessageTexts = getLogMessageTexts();
        view = getView();
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
        initializeModel();
    }


    @Override
    public void packagingChanged(String packaging) {
        jobModel.withPackaging(packaging);
    }


    @Override
    public void formatChanged(String format) {
        jobModel.withFormat(format);
    }

    @Override
    public void charsetChanged(String charset) {
        jobModel.withCharset(charset);
    }

    @Override
    public void destinationChanged(String destination) {
        jobModel.withDestination(destination);
    }

    @Override
    public void mailForNotificationAboutVerificationChanged(String mailForNotificationvoidAboutVerification) {
        jobModel.withMailForNotificationAboutVerification(mailForNotificationvoidAboutVerification);
    }

    @Override
    public void mailForNotificationAboutProcessingChanged(String mailForNotificationvoidAboutProcessing) {
        jobModel.withMailForNotificationAboutProcessing(mailForNotificationvoidAboutProcessing);
    }

    @Override
    public void resultMailInitialsChanged(String resultMailInitialsChanged) {
        jobModel.withResultMailInitials(resultMailInitialsChanged);
    }

    @Override
    public void typeChanged(JobSpecification.Type type) {
        jobModel.withType(type);
    }

    /**
     * A signal to the presenter, saying that a key has been pressed in either of the fields
     */
    public void keyPressed() {
        view.status.setText("");
    }

    /**
     * A signal to the presenter, saying that the save button has been pressed
     */
    public void rerunButtonPressed() {
        if (jobModel != null) {
            if (jobModel.isInputFieldsEmpty()) {
                view.setErrorText(texts.error_InputFieldValidationError());
            } else {
                doReSubmitJobInJobStore();
            }
        }
    }

    /*
     * Private methods
     */

    protected abstract void initializeViewFields();
    /**
     * Method used to update all fields in the view according to the current state of the class
     */
    void updateAllFieldsAccordingToCurrentState() {
        if(isRawRepo()) {
            view.header.setText(texts.header_JobRerunFromRR());
        } else if(isTickle()) {
            view.header.setText(texts.header_JobRerunFromTickle());
        }
        view.jobId.setText(jobModel.getJobId());
        view.packaging.setText(jobModel.getPackaging());
        view.format.setText(jobModel.getFormat());
        view.charset.setText(jobModel.getCharset());
        view.destination.setText(jobModel.getDestination());
        view.mailForNotificationAboutVerification.setText(jobModel.getMailForNotificationAboutVerification());
        view.mailForNotificationAboutProcessing.setText(jobModel.getMailForNotificationAboutProcessing());
        view.resultMailInitials.setText(jobModel.getResultMailInitials());
        view.type.setText(jobModel.getType().toString());
        view.datafile.setText(jobModel.getDataFile());
        view.partnumber.setText(String.valueOf(jobModel.getPartNumber()));
        view.jobcreationtime.setText(jobModel.getJobCreationTime());
        view.jobcreationtime.setText(jobModel.getJobCompletionTime());
        initializeViewFields();
    }

    boolean isRawRepo() {
        String stringToken = this.jobModel.getHarvesterTokenAncestry();
        if (stringToken != null && !stringToken.isEmpty()) {
            HarvesterToken harvesterToken = HarvesterToken.of(stringToken);
            return harvesterToken.getHarvesterVariant().equals(HarvesterToken.HarvesterVariant.RAW_REPO);
        }
        return false;
    }

    boolean isTickle() {
        String stringToken = this.jobModel.getHarvesterTokenAncestry();
        if (stringToken != null && !stringToken.isEmpty()) {
            HarvesterToken harvesterToken = HarvesterToken.of(stringToken);
            return harvesterToken.getHarvesterVariant().equals(HarvesterToken.HarvesterVariant.TICKLE_REPO);
        }
        return false;
    }


    /*
     * Protected methods
     */

    View getView(){
        return viewInjector.getView();
    }

    Texts getTexts() {
        return viewInjector.getTexts();
    }

    LogMessageTexts getLogMessageTexts() {
        return viewInjector.getLogMessageTexts();
    }
    /**
     * Method used to set the model after a successful update or a save
     * @param jobModel The model to save
     */
    protected void setJobModel(JobModel jobModel) {
        this.jobModel = jobModel;
    }

    /*
     * Local class
     */

    /*
     * Abstract methods
     */

    /**
     * getModel
     */
    abstract void initializeModel();

    /**
     * saveModel
     */
    abstract void doReSubmitJobInJobStore();

}
