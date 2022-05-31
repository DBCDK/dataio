package dk.dbc.dataio.gui.client.pages.sink.status;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.List;

public class View extends ContentPanel<Presenter> implements IsWidget {
    // Instantiate UI Binder
    interface MyUiBinder extends UiBinder<Widget, View> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    // UI Fields
    @UiField
    Button refreshButton;
    @UiField(provided = true)
    SinkStatusTable sinkStatusTable;

    /**
     * Default empty constructor
     */
    public View() {
        this("");
    }

    /**
     * Default constructor
     *
     * @param header header
     */
    public View(String header) {
        super(header);
        sinkStatusTable = new SinkStatusTable();
        add(uiBinder.createAndBindUi(this));
    }


    /*
     * Ui Handlers
     */

    @UiHandler("refreshButton")
    void sinkTypeSelectionChanged(ClickEvent event) {
        presenter.fetchSinkStatus();
    }


    /*
     * Public methods
     */

    /**
     * Setup the supplied data to the view
     *
     * @param sinkStatus Sink Status data to set in the view
     */
    public void setSinkStatus(List<SinkStatusTable.SinkStatusModel> sinkStatus) {
        sinkStatusTable.setSinkStatusData(presenter, sinkStatus);
    }

}
