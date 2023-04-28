package dk.dbc.dataio.gui.client.pages.flowbinder.status;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.model.FlowBinderUsage;
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
    FlowBinderStatusTable flowBinderStatusTable;

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
        flowBinderStatusTable = new FlowBinderStatusTable();
        add(uiBinder.createAndBindUi(this));
    }


    /*
     * Ui Handlers
     */
    @UiHandler("refreshButton")
    void flowbinderTypeSelectionChanged(ClickEvent event) {
        presenter.getFlowBindersUsage();
    }

    /*
     * Public methods
     */
    public void setFlowbinderStatus(List<FlowBinderUsage> flowbinderStatus) {
        flowBinderStatusTable.setFlowBinderStatusData(presenter, flowbinderStatus);
    }

}
