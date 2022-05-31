package dk.dbc.dataio.gui.client.pages.basemaintenance;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.components.prompted.PromptedTextBox;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public class View extends ContentPanel<Presenter> implements IsWidget {

    // Injectors
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    // Instantiate UI Binder
    interface MyUiBinder extends UiBinder<Widget, View> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    // UI Fields
    @UiField
    PromptedTextBox trackingIdSearch;
    @UiField
    Button trackingIdSearchButton;

    /**
     * Default constructor
     */
    public View() {
        super("");
        add(uiBinder.createAndBindUi(this));
        setHeader(commonInjector.getMenuTexts().menu_BaseMaintenance());
    }

    @UiHandler("trackingIdSearchButton")
    void trackingIdSearchButtonClicked(ClickEvent event) {
        String value = trackingIdSearch.getValue();
        if (value == null || value.isEmpty()) {
            setErrorText(viewInjector.getTexts().error_EmptyTrackingIdError());
        } else {
            presenter.traceItem(value);
        }
    }

}
