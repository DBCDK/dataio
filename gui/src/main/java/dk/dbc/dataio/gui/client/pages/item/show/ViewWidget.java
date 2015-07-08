package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import dk.dbc.dataio.gui.util.ClientFactory;

public class ViewWidget extends ContentPanel<Presenter> implements IsWidget {
    protected Texts texts;
    static final int ALL_ITEMS_TAB_INDEX = 0;
    static final int FAILED_ITEMS_TAB_INDEX = 1;
    static final int IGNORED_ITEMS_TAB_INDEX = 2;
    static final int JOB_INFO_TAB_CONTENT = 3;
    static final int JOB_DIAGNOSTIC_TAB_CONTENT = 4;

    interface ViewUiBinder extends UiBinder<Widget, ViewWidget> {}

    private static ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);

    @UiField Label jobHeader;
    @UiField DecoratedTabPanel tabPanel;
    @UiField ItemsListView allItemsList;
    @UiField ItemsListView failedItemsList;
    @UiField ItemsListView ignoredItemsList;
    @UiField JobInfoTabContent jobInfoTabContent;
    @UiField JobDiagnosticTabContent jobDiagnosticTabContent;

    /**
     * Constructor with header and text
     * @param clientFactory, the client factory
     */
    public ViewWidget(ClientFactory clientFactory) {
        super(clientFactory.getItemsShowTexts().menu_Items());
        texts = clientFactory.getItemsShowTexts();
        add(uiBinder.createAndBindUi(this));
        allItemsList.itemsPager.firstPage();
        failedItemsList.itemsPager.firstPage();
        ignoredItemsList.itemsPager.firstPage();
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
        }
    }

}

