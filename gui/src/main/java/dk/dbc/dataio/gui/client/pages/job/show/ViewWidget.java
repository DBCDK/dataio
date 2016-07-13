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

package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.components.jobfilter.JobFilter;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public abstract class ViewWidget extends ContentPanel<Presenter> implements IsWidget {

    // Constants
    protected static final int PAGE_SIZE = 20;
    protected static final int FAST_FORWARD_PAGES = 5;


    // Instantiate UI Binder
    interface MyUiBinder extends UiBinder<Widget, ViewWidget> {}
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    /**
     * Default constructor
     * @param headerText, the text for the header in the view
     */
    public ViewWidget(String headerText) {
        super(headerText);
        add(uiBinder.createAndBindUi(this));
        allJobsButton.setValue(true);
        rerunAllShownJobsConfirmationDialog.show();  // First show the DialogBox in order to add it to the DOM
        rerunAllShownJobsConfirmationDialog.hide();  // ... but we don't want it shown upon startup - so hide it again
        rerunAllShownJobsConfirmationDialog.center();
    }

    // UI Fields
    @UiField CellTable jobsTable;
    @UiField JobFilter jobFilter;
    @UiField SimplePager pagerTop;
    @UiField SimplePager pagerBottom;
    @UiField RadioButton allJobsButton;
    @UiField RadioButton processingFailedJobsButton;
    @UiField RadioButton deliveringFailedJobsButton;
    @UiField RadioButton fatalJobsButton;
    @UiField Button refreshButton;
    @UiField Button rerunAllShownJobsButton;
    @UiField TextBox jobIdInputField;
    @UiField PushButton showJobButton;
    @UiField DialogBox rerunAllShownJobsConfirmationDialog;
    @UiField Label rerunJobsCount;
    @UiField Label rerunJobsList;
    @UiField Label rerunJobsConfirmation;
    @UiField Button rerunOkButton;


    @UiFactory SimplePager makeSimplePager() {
        // We want to make a UI Factory instantiation of the pager, because UI Binder only allows us to instantiate
        // the pager with a location, and we do also want to enable the "Show Last Page" Button and we also want to
        // set the Fast Forward button to scroll 100 items (10 pages) at a time.
        return new SimplePager(SimplePager.TextLocation.CENTER, true, FAST_FORWARD_PAGES * PAGE_SIZE, true);
    }

    @UiHandler(value={"allJobsButton", "processingFailedJobsButton", "deliveringFailedJobsButton", "fatalJobsButton"})
    @SuppressWarnings("unused")
    void filterItemsRadioButtonPressed(ClickEvent event) {
        pagerTop.firstPage();
        presenter.updateSelectedJobs();
    }

    @UiHandler("jobFilter")
    @SuppressWarnings("unused")
    void jobFilterChanged(ChangeEvent event) {
        presenter.updateSelectedJobs();
    }

    @UiHandler("refreshButton")
    @SuppressWarnings("unused")
    void refreshButtonPressed(ClickEvent event) {
        presenter.refresh();
    }

    @UiHandler("rerunAllShownJobsButton")
    @SuppressWarnings("unused")
    void setRerunAllShownJobsButtonPressed(ClickEvent event) {
        rerunAllShownJobs();
    }

    @UiHandler("showJobButton")
    @SuppressWarnings("unused")
    void showJobButtonPressed(ClickEvent event) {
        presenter.showJob();
    }

    @UiHandler("jobIdInputField")
    void onKeyDown(KeyDownEvent event) {
        if(event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            presenter.showJob();
        }
    }

    @UiHandler("rerunOkButton")
    @SuppressWarnings("unused")
    void onRerunOkButtonClick(ClickEvent event) {
        rerunAllShownJobsConfirmed();
        rerunAllShownJobsConfirmationDialog.hide();
    }

    @UiHandler("rerunCancelButton")
    @SuppressWarnings("unused")
    void onRerunCancelButtonClick(ClickEvent event) {
        rerunAllShownJobsConfirmationDialog.hide();  // Just hide - do nothing else...
    }


    // Abstract methods
    abstract void rerunAllShownJobs();
    abstract void rerunAllShownJobsConfirmed();

}
