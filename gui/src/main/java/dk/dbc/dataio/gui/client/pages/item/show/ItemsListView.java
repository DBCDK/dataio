package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.HTMLPanel;

public class ItemsListView extends Composite {
    protected static final int PAGE_SIZE = 20;
    protected static final int FAST_FORWARD_PAGES = 5;
    final int JAVASCRIPT_LOG_TAB_CONTENT = 0;
    final int OUTPUT_POST_TAB_CONTENT = 2;
    final int SINK_RESULT_TAB_CONTENT = 3;

    interface ItemsListUiBinder extends UiBinder<HTMLPanel, ItemsListView> {
    }

    private static ItemsListUiBinder ourUiBinder = GWT.create(ItemsListUiBinder.class);

    @UiField CellTable itemsTable;
    @UiField SimplePager itemsPager;
    @UiField DecoratedTabPanel detailedTabs;

    public ItemsListView() {
        initWidget(ourUiBinder.createAndBindUi(this));
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
     * @param event Clicked event
     */
    @UiHandler("backButton")
    void backButtonPressed(ClickEvent event) {
        History.back();
    }
}