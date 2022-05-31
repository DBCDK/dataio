package dk.dbc.dataio.gui.client.pages.flowcomponent.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.pages.flowcomponent.show.jsmodulespopup.PopupDoubleList;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public abstract class ViewWidget extends ContentPanel<Presenter> implements IsWidget {

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);

    // Instantiate UI Binder
    interface MyUiBinder extends UiBinder<Widget, ViewWidget> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    // UI Fields
    @UiField
    CellTable flowComponentsTable;
    @UiField
    PopupDoubleList jsModulesPopup;


    public ViewWidget(String header) {
        super(header);
        add(uiBinder.createAndBindUi(this));
    }

    /**
     * Ui Handler to catch click events on the create button
     *
     * @param event Clicked event
     */
    @SuppressWarnings("unused")
    @UiHandler("createButton")
    void backButtonPressed(ClickEvent event) {
        presenter.createFlowComponent();
    }

    View getView() {
        return viewInjector.getView();
    }

    Texts getTexts() {
        return viewInjector.getTexts();
    }

}
