package dk.dbc.dataio.gui.client.pages.javascriptlog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.views.ContentPanel;

/**
 *
 * The implementation of the job javaScriptLog View
 *
 */
public class View extends ContentPanel<Presenter> implements IsWidget {

    interface ViewUiBinder extends UiBinder<Widget, View> {}
    private static ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);

    @UiField
    HTML htmlLabel;

    /**
     * Constructor
     */
    public View(String header) {
        super(header);
        add(uiBinder.createAndBindUi(this));
    }

    @Override
    public void init() {}

}
