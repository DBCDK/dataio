package dk.dbc.dataio.gui.client.pages.submitter.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public abstract class ViewWidget extends ContentPanel<Presenter> implements IsWidget {

    // Instantiate UI Binder
    interface MyUiBinder extends UiBinder<Widget, ViewWidget> {}
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    protected Texts texts;

    // UI Fields
    @UiField CellTable submittersTable;


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
     * This method initalizes the view
     */
    @Override
    public void init() {}

}
