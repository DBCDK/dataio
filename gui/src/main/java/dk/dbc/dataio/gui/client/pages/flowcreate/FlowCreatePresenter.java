package dk.dbc.dataio.gui.client.pages.flowcreate;

import dk.dbc.dataio.gui.client.presenters.Presenter;
import java.util.Collection;

public interface FlowCreatePresenter extends Presenter {
    void saveFlow(String name, String description, Collection<String> selectedItems);
}
