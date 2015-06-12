package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import dk.dbc.dataio.gui.util.ClientFactory;

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
    @UiField RadioButton allJobsButton;
    @UiField RadioButton processingFailedJobsButton;
    @UiField RadioButton deliveringFailedJobsButton;


    @UiFactory SimplePager makeSimplePager() {
        // We want to make a UI Factory instantiation of the pager, because UI Binder only allows us to instantiate
        // the pager with a location, and we do also want to enable the "Show Last Page" Button and we also want to
        // set the Fast Forward button to scroll 100 items (10 pages) at a time.
        return new SimplePager(SimplePager.TextLocation.CENTER, true, FAST_FORWARD_PAGES * PAGE_SIZE, true);
    }

    @UiHandler(value={"allJobsButton", "processingFailedJobsButton", "deliveringFailedJobsButton"})
    void filterItemsRadioButtonPressed(ClickEvent event) {
        pagerTop.firstPage();
        presenter.fetchSelectedJobs();
    }

    /**
     * Default constructor
     * @param clientFactory, the client factory
     */
    public ViewWidget(ClientFactory clientFactory) {
        super(clientFactory.getMenuTexts().menu_Jobs());
        texts = clientFactory.getJobsShowTexts();
        add(uiBinder.createAndBindUi(this));
        this.allJobsButton.setValue(true);
    }

}
