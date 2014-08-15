package dk.dbc.dataio.gui.client.pages.harvestersShow;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.gui.client.views.GenericView;

import java.util.List;

/**
 * Created by sma on 25/04/14.
 */
//TODO - SinkShowPresenter - TODO needs to be replaced with list of Harvester objects
public interface HarvestersShowGenericView extends IsWidget, GenericView<HarvestersShowGenericPresenter> {
    void setHarvesters(List<String> job);
}
