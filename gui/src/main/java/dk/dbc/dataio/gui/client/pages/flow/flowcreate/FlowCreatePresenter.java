package dk.dbc.dataio.gui.client.pages.flow.flowcreate;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;
import java.util.Collection;

public interface FlowCreatePresenter extends GenericPresenter {
    void saveFlow(String name, String description, Collection<String> selectedItems);
}
