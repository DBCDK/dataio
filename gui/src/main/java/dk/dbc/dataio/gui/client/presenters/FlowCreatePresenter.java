package dk.dbc.dataio.gui.client.presenters;

import java.util.List;

public interface FlowCreatePresenter extends Presenter {
    void saveFlow(String name, String description, List<String> selectedItems);
}
