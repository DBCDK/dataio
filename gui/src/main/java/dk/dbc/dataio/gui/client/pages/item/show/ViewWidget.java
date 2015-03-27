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
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public class ViewWidget extends ContentPanel<Presenter> implements IsWidget {
    protected Texts texts;
    protected static final int PAGE_SIZE = 20;
    protected static final int FAST_FORWARD_PAGES = 5;

    @Override
    public void init() {
    }

    interface ViewUiBinder extends UiBinder<Widget, ViewWidget> {}

    private static ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);

    @UiField Label jobHeader;
    @UiField CellTable itemsTable;
    @UiField TabPanel tabPanel;
    @UiField SimplePager pager;
    @UiField RadioButton allItemsButton;
    @UiField RadioButton failedItemsButton;
    @UiField RadioButton ignoredItemsButton;

    @UiFactory
    SimplePager makeSimplePager() {
        // We want to make a UI Factory instantiation of the pager, because UI Binder only allows us to instantiate
        // the pager with a location, and we do also want to enable the "Show Last Page" Button and we also want to
        // set the Fast Forward button to scroll 100 items (10 pages) at a time.
        return new SimplePager(SimplePager.TextLocation.CENTER, true, FAST_FORWARD_PAGES * PAGE_SIZE, true);
    }

    @UiHandler("backButton")
    void backButtonPressed(ClickEvent event) {
        History.back();
    }

    @UiHandler(value={"allItemsButton", "failedItemsButton", "ignoredItemsButton"})
    void filterItemsRadioButtonPressed(ClickEvent event) {
        pager.firstPage();
        presenter.filterItems();
    }

    public ViewWidget(String header, Texts texts) {
        super(header);
        this.texts = texts;
        add(uiBinder.createAndBindUi(this));
    }

}

