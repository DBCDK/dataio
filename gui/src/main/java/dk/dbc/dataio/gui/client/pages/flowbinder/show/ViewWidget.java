package dk.dbc.dataio.gui.client.pages.flowbinder.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import dk.dbc.dataio.gui.util.ClientFactory;

public abstract class ViewWidget extends ContentPanel<Presenter> implements IsWidget {

    // Instantiate UI Binder
    interface MyUiBinder extends UiBinder<Widget, ViewWidget> {}
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    protected Texts texts;

    // UI Fields
    @UiField CellTable flowBindersTable;


    /**
     * Default constructor
     * @param clientFactory, the client factory
     */
    public ViewWidget(ClientFactory clientFactory) {
        super(clientFactory.getMenuTexts().menu_FlowBinders());
        texts = clientFactory.getFlowBindersShowTexts();
        add(uiBinder.createAndBindUi(this));
    }

    /**
     * This method initalizes the view
     */
    @Override
    public void init() {}


    /**
     * Ui Handler to catch click events on the create button
     * @param event Clicked event
     */
    @UiHandler("createButton")
    void backButtonPressed(ClickEvent event) {
        presenter.createFlowBinder();
    }

}
