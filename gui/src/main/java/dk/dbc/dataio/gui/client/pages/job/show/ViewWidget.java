package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.components.jobfilter.JobFilter;
import dk.dbc.dataio.gui.client.components.popup.PopupListBox;
import dk.dbc.dataio.gui.client.components.popup.PopupSelectBox;
import dk.dbc.dataio.gui.client.components.prompted.PromptedList;
import dk.dbc.dataio.gui.client.events.DialogEvent;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public abstract class ViewWidget extends ContentPanel<Presenter> implements IsWidget {

    // Instantiate UI Binder
    interface MyUiBinder extends UiBinder<Widget, ViewWidget> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    /**
     * Default constructor
     *
     * @param headerText, the text for the header in the view
     */
    public ViewWidget(String headerText) {
        super(headerText);
        add(uiBinder.createAndBindUi(this));
        rerunAllShownJobsConfirmationDialog.center();
        rerunAllShownJobsConfirmationDialog.show();  // First show the DialogBox in order to add it to the DOM
        rerunAllShownJobsConfirmationDialog.hide();  // ... but we don't want it shown upon startup - so hide it again
    }

    // UI Fields
    @UiField
    CellTable jobsTable;
    @UiField
    JobFilter jobFilter;
    @UiField
    SimplePager pagerTop;
    @UiField
    SimplePager pagerBottom;
    @UiField
    Button refreshButton;
    @UiField
    Button rerunAllShownJobsButton;
    @UiField
    PromptedList numberOfShownJobsSelection;
    @UiField
    TextBox jobIdInputField;
    @UiField
    PushButton showJobButton;
    @UiField
    DialogBox rerunAllShownJobsConfirmationDialog;
    @UiField
    Label rerunJobsCount;
    @UiField
    Label rerunJobsList;
    @UiField
    Label rerunJobsConfirmation;
    @UiField
    Button rerunOkButton;
    @UiField
    PopupSelectBox popupSelectBox;
    @UiField
    PushButton changeColorSchemeButton;
    @UiField
    PushButton logButton;
    // Only public due to JobFilterTest
    @UiField
    public PopupListBox changeColorSchemeListBox;
    @UiField
    CheckBox autoRefresh;


    @UiFactory
    PopupSelectBox getPopupBox() {
        // Actual values are defined in View.ui.xml
        return new PopupSelectBox();
    }

    @UiFactory
    SimplePager makeSimplePager() {
        return new SimplePager(SimplePager.TextLocation.CENTER, false, true);
    }

    @UiHandler("jobFilter")
    @SuppressWarnings("unused")
    void jobFilterChanged(ChangeEvent event) {
        presenter.updateSelectedJobs();
        logButton.fireEvent(new ClickEvent() {
        });
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

    @UiHandler("logButton")
    @SuppressWarnings("unused")
    void showLogButtonPressen(ClickEvent event) {
        presenter.showLog();
    }

    @UiHandler("historyButton")
    @SuppressWarnings("unused")
    void showLogHistoryButtonPressen(ClickEvent event) {
        presenter.showHistory();
    }

    @UiHandler("clearButton")
    @SuppressWarnings("unused")
    void clearLogButtonPressen(ClickEvent event) {
        presenter.clearLog();
    }

    @UiHandler("numberOfShownJobsSelection")
    void numberOfShownJobsSelectionChanged(ValueChangeEvent<String> event) {
        switch (event.getValue()) {
            case "20":
                jobsTable.setPageSize(20);
                break;
            case "50":
                jobsTable.setPageSize(50);
                break;
            case "100":
                jobsTable.setPageSize(100);
                break;
        }
    }

    @UiHandler("jobIdInputField")
    void onKeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
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

    @UiHandler("popupSelectBox")
    void confirmationButtonClicked(DialogEvent event) {
        if (event.getDialogButton() == DialogEvent.DialogButton.OK_BUTTON) {
            presenter.rerun();
        }
    }

    @UiHandler("changeColorSchemeListBox")
    void popupListBoxClicked(DialogEvent event) {
        switch (event.getDialogButton()) {
            case OK_BUTTON:
                presenter.changeColorScheme(changeColorSchemeListBox.getValue());
                break;
            case CANCEL_BUTTON:
            default:
                // Do nothing
                break;
        }
    }

    @UiHandler("changeColorSchemeButton")
    @SuppressWarnings("unused")
    void colorSchemeChanged(ClickEvent event) {
        presenter.changeColorSchemeListBoxShow();
    }

    @UiHandler("autoRefresh")
    @SuppressWarnings("unused")
    void autoRefreshClicked(ClickEvent event) {
        setAutoRefresh(autoRefresh.getValue());
    }


    // Abstract methods
    abstract void rerunAllShownJobs();

    abstract void rerunAllShownJobsConfirmed();

    abstract void setAutoRefresh(boolean autoRefresh);

}
