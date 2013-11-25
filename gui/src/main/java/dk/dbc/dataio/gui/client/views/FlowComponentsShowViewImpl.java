package dk.dbc.dataio.gui.client.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.gui.client.i18n.FlowComponentsShowConstants;
import dk.dbc.dataio.gui.client.presenters.FlowComponentsShowPresenter;
import java.util.List;


public class FlowComponentsShowViewImpl extends FlowPanel implements FlowComponentsShowView {

    // public Identifiers
    public static final String GUIID_FLOW_COMPONENTS_SHOW_WIDGET = "flowcomponentsshowwidget";

    // private objects
    private FlowComponentsShowPresenter presenter;
    private final FlowComponentsShowConstants constants = GWT.create(FlowComponentsShowConstants.class);

    Label counterText = new Label();

    
    public FlowComponentsShowViewImpl() {
        super();
        getElement().setId(GUIID_FLOW_COMPONENTS_SHOW_WIDGET);

//        ...
        
        add(counterText);
    }

    @Override
    public void setPresenter(FlowComponentsShowPresenter presenter) {
        this.presenter = presenter;
    }

    @Override
    public void onFailure(String message) {
        Window.alert("Error: " + message);
    }

    @Override
    public void onSuccess(String message) {
//        saveButton.setStatusText(message);
    }

    @Override
    public void refresh() {
    }

    @Override
    public void setFlowComponents(List<FlowComponent> flowComponents) {
        counterText.setText("Antal flowkomponenter: " + flowComponents.size());
    }

}
