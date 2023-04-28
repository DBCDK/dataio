package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public class ViewWidget extends ContentPanel<Presenter> implements IsWidget {
    // Please do note, that this list of sequential numbers are maintained manually
    // They must follow the order, given in the UI Binder file View.ui.xml
    static final int ALL_ITEMS_TAB_INDEX = 0;
    static final int FAILED_ITEMS_TAB_INDEX = 1;
    static final int IGNORED_ITEMS_TAB_INDEX = 2;
    static final int JOB_INFO_TAB_CONTENT = 3;
    static final int JOB_DIAGNOSTIC_TAB_CONTENT = 4;
    static final int JOB_NOTIFICATION_TAB_CONTENT = 5;
    static final int WORKFLOW_NOTE_TAB_CONTENT = 6;
    protected static final int PAGE_SIZE = 20;
    protected static final int FAST_FORWARD_PAGES = 5;

    interface ViewUiBinder extends UiBinder<Widget, ViewWidget> {
    }

    private static ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);

    @UiField
    Label jobHeader;
    @UiField
    DecoratedTabPanel tabPanel;
    @UiField
    HTMLPanel allItemsListTab;
    @UiField
    HTMLPanel failedItemsListTab;
    @UiField
    HTMLPanel ignoredItemsListTab;
    @UiField
    HTMLPanel jobInfoTab;
    @UiField
    ItemsListView itemsListView;
    @UiField
    JobInfoTabContent jobInfoTabContent;
    @UiField
    JobDiagnosticTabContent jobDiagnosticTabContent;
    @UiField
    JobNotificationsTabContent jobNotificationsTabContent;
    @UiField
    WorkflowNoteTabContent workflowNoteTabContent;
    @UiField
    SimplePager itemsPager;
    @UiField
    TextBox recordIdInputField;
    @UiField
    HorizontalPanel recordIdPanel;
    @UiField
    PushButton showRecordsButton;

    /**
     * Constructor with header and text
     *
     * @param header Breadcrumb header text
     */
    public ViewWidget(String header) {
        super(header);
        add(uiBinder.createAndBindUi(this));
        itemsPager.firstPage();
    }

    @UiFactory
    SimplePager makeSimplePager() {
        // We want to make a UI Factory instantiation of the pager, because UI Binder only allows us to instantiate
        // the pager with a location, and we do also want to enable the "Show Last Page" Button and we also want to
        // set the Fast Forward button to scroll 100 items (10 pages) at a time.
        return new SimplePager(SimplePager.TextLocation.CENTER, true, FAST_FORWARD_PAGES * PAGE_SIZE, true);
    }

    /**
     * Ui Handler to catch click events on the Back button
     *
     * @param event Clicked event
     */
    @UiHandler("backButton")
    void backButtonPressed(ClickEvent event) {
        History.back();
    }

    /**
     * Ui Handler to catch selection events on the tabs in the tab panel
     *
     * @param event Selected event
     */
    @UiHandler("tabPanel")
    void tabPanelSelection(SelectionEvent<Integer> event) {
        itemsListView.setVisible(true);
        switch (event.getSelectedItem()) {
            case ALL_ITEMS_TAB_INDEX:
                presenter.allItemsTabSelected();
                recordIdPanel.setVisible(true);
                break;
            case FAILED_ITEMS_TAB_INDEX:
                presenter.failedItemsTabSelected();
                recordIdPanel.setVisible(true);
                break;
            case IGNORED_ITEMS_TAB_INDEX:
                presenter.ignoredItemsTabSelected();
                recordIdPanel.setVisible(true);
                break;
            case WORKFLOW_NOTE_TAB_CONTENT:
                presenter.hideDetailedTabs();
                presenter.noteTabSelected();
                recordIdPanel.setVisible(false);
                break;
            case JOB_INFO_TAB_CONTENT:
                presenter.hideDetailedTabs();
                recordIdPanel.setVisible(false);
                break;
            case JOB_NOTIFICATION_TAB_CONTENT:
                presenter.hideDetailedTabs();
                recordIdPanel.setVisible(false);
                break;
            case JOB_DIAGNOSTIC_TAB_CONTENT:
                presenter.hideDetailedTabs();
                recordIdPanel.setVisible(false);
                break;
        }
    }

    @UiHandler("showRecordsButton")
    @SuppressWarnings("unused")
    void showJobButtonPressed(ClickEvent event) {
        presenter.recordSearch();
    }

    @UiHandler("recordIdInputField")
    void onKeyDown(KeyDownEvent event) {
        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
            presenter.recordSearch();
        }
    }

    @SuppressWarnings("unused")
    @UiHandler("saveButton")
    void saveButtonPressed(ClickEvent event) {
        presenter.setWorkflowNoteModel(workflowNoteTabContent.note.getText());
    }
}

