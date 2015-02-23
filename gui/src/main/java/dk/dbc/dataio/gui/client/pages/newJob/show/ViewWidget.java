package dk.dbc.dataio.gui.client.pages.newJob.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public abstract class ViewWidget extends ContentPanel<Presenter> implements IsWidget {

    // Constants
    protected static final int PAGE_SIZE = 20;
    protected static final int FAST_FORWARD_PAGES = 5;


    // Instantiate UI Binder
    interface MyUiBinder extends UiBinder<Widget, ViewWidget> {}
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    protected Texts texts;

    // UI Fields
    @UiField CellTable jobsTable;
    @UiField SimplePager pagerTop;
    @UiField SimplePager pagerBottom;


    @UiFactory SimplePager makeSimplePager() {
        // We want to make a UI Factory instantiation of the pager, because UI Binder only allows us to instantiate
        // the pager with a location, and we do also want to enable the "Show Last Page" Button and we also want to
        // set the Fast Forward button to scroll 100 items (10 pages) at a time.
        return new SimplePager(SimplePager.TextLocation.CENTER, true, FAST_FORWARD_PAGES * PAGE_SIZE, true);
    }

    /**
     * Default constructor
     * @param header The header text for the View
     * @param texts The I8n texts for this view
     */
    public ViewWidget(String header, Texts texts) {
        super(header);
        this.texts = texts;
        add(uiBinder.createAndBindUi(this));
    }

    /**
     * This method initializes the view
     */
    @Override
    public void init() {}

}
