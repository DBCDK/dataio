package dk.dbc.dataio.gui.client.pages.flowcreate;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;
import java.util.Collection;

public interface FlowCreateGenericPresenter extends GenericPresenter {
    void saveFlow(String name, String description, Collection<String> selectedItems);
}
