package dk.dbc.dataio.gui.client.pages.harvester.corepo.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import dk.dbc.dataio.gui.client.views.ContentPanel;
import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;

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


    /**
     * Default empty constructor
     */
    public View() {
        this("");
    }

    /**
     * Default constructor
     *
     * @param header Header text
     */
    public View(String header) {
        super(header);
        harvestersTable = new HarvestersTable(this);
        add(uiBinder.createAndBindUi(this));
    }


    /*
     * UI Handler Actions
     */

    @SuppressWarnings("unused")
    @UiHandler("newCoRepoHarvesterButton")
    public void createCoRepoHarvester(ClickEvent event) {
        presenter.createCoRepoHarvester();
    }

    /*
     * Public access methods
     */

    /**
     * Set the list of actual Harvesters in the view
     *
     * @param harvesters The list of Harvesters to show
     */
    public void setHarvesters(List<CoRepoHarvesterConfig> harvesters) {
        harvestersTable.setHarvesters(presenter, harvesters);
    }


    /*
     * Local methods
     */

    protected Texts getTexts() {
        return this.viewInjector.getTexts();
    }

}
