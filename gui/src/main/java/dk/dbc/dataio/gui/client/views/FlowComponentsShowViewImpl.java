package dk.dbc.dataio.gui.client.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.gui.client.i18n.FlowComponentsShowConstants;
import dk.dbc.dataio.gui.client.presenters.FlowComponentsShowPresenter;
import java.util.List;


public class FlowComponentsShowViewImpl extends FlowPanel implements FlowComponentsShowView {

    // public Identifiers
    public static final String GUIID_FLOW_COMPONENTS_SHOW_WIDGET = "flowcomponentsshowwidget";

    // private objects
//    private FlowComponentsShowPresenter presenter;
    private final FlowComponentsShowConstants constants = GWT.create(FlowComponentsShowConstants.class);

    private CellTable<FlowComponent> table = new CellTable<FlowComponent>();
    
    
    
    public FlowComponentsShowViewImpl() {
        super();
        getElement().setId(GUIID_FLOW_COMPONENTS_SHOW_WIDGET);
    
        TextColumn<FlowComponent> nameColumn = new TextColumn<FlowComponent>() {
            @Override
            public String getValue(FlowComponent content) {
                return content.getContent().getName();
            }
        };
        table.addColumn(nameColumn, constants.columnHeader_Name());

        TextColumn<FlowComponent> invocationMethodColumn = new TextColumn<FlowComponent>() {
            @Override
            public String getValue(FlowComponent content) {
                return content.getContent().getInvocationMethod();
            }
        };
        table.addColumn(invocationMethodColumn, constants.columnHeader_InvocationMethod());
        
        add(table);
    }

    @Override
    public void setPresenter(FlowComponentsShowPresenter presenter) {
//        this.presenter = presenter;
    }

    @Override
    public void onFailure(String message) {
        Window.alert("Error: " + message);
    }

    @Override
    public void onSuccess(String message) {
    }

    @Override
    public void refresh() {
    }

    @Override
    public void setFlowComponents(List<FlowComponent> flowComponents) {
        table.setRowData(0, flowComponents);
        table.setRowCount(flowComponents.size());
    }

}
