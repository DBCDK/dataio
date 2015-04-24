package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public class ViewWidget extends ContentPanel<Presenter> implements IsWidget {
    protected Texts texts;
    static final int ALL_ITEMS_TAB_INDEX = 0;
    static final int FAILED_ITEMS_TAB_INDEX = 1;
    static final int IGNORED_ITEMS_TAB_INDEX = 2;
    static final int JOB_INFO_TAB_INDEX = 3;

    @Override
    public void init() {
    }

    interface ViewUiBinder extends UiBinder<Widget, ViewWidget> {}

    private static ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);

    @UiField Label jobHeader;
    @UiField DecoratedTabPanel tabPanel;
    @UiField ItemsListView allItemsList;
    @UiField ItemsListView failedItemsList;
    @UiField ItemsListView ignoredItemsList;


    /**
     * Constructor with header and text
     * @param header Header text
     * @param texts Texts
     */
    public ViewWidget(String header, Texts texts) {
        super(header);
        this.texts = texts;
        add(uiBinder.createAndBindUi(this));
        allItemsList.itemsPager.firstPage();
        failedItemsList.itemsPager.firstPage();
        ignoredItemsList.itemsPager.firstPage();
    }

    /**
     * Ui Handler to catch click events on the Back button
     * @param event Clicked event
     */
    @UiHandler("backButton")
    void backButtonPressed(ClickEvent event) {
        History.back();
    }


    /**
     * Ui Handler to catch selection events on the tabs in the tab panel
     * @param event Selected event
     */
    @UiHandler("tabPanel")
    void tabPanelSelection(SelectionEvent<Integer> event) {
        switch(event.getSelectedItem()) {
            case ALL_ITEMS_TAB_INDEX:
                presenter.allItemsTabSelected();
                break;
            case FAILED_ITEMS_TAB_INDEX:
                presenter.failedItemsTabSelected();
                break;
            case IGNORED_ITEMS_TAB_INDEX:
                presenter.ignoredItemsTabSelected();
                break;
            case JOB_INFO_TAB_INDEX:
                presenter.jobInfoTabSelected();
                break;
        }
    }

}

