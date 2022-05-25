package dk.dbc.dataio.gui.client.pages.harvester.dmat.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import dk.dbc.dataio.harvester.types.DMatHarvesterConfig;

import java.util.List;

public class View extends ContentPanel<Presenter> implements IsWidget {
    private ViewGinjector viewInjector = GWT.create(ViewGinjector.class);

    // Instantiate UI Binder
    interface MyUiBinder extends UiBinder<Widget, View> {
    }

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    // UI Fields
    @UiField(provided = true)
    HarvestersTable harvestersTable;

    public View() {
        this("");
    }

    public View(String header) {
        super(header);
        harvestersTable = new HarvestersTable();
        add(uiBinder.createAndBindUi(this));
    }

    /*
     * Public access methods
     */

    /**
     * Sets the list of harvesters in the view
     *
     * @param harvesters list of Harvesters to show
     */
    public void setHarvesters(List<DMatHarvesterConfig> harvesters) {
        harvestersTable.setHarvesters(presenter, harvesters);
    }

    protected Texts getTexts() {
        return this.viewInjector.getTexts();
    }
}
