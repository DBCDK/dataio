package dk.dbc.dataio.gui.client.pages.flow.modify;

import dk.dbc.dataio.gui.client.views.GenericView;

import java.util.Map;

public interface View extends GenericView<Presenter> {
    void initializeFields();
    void setName(String name);
    String getName();
    void setDescription(String description);
    String getDescription();
    void setAvailableFlowComponents(Map<String, String> availableFlowComponents);
    void setSelectedFlowComponents(Map<String, String> availableFlowComponents);
}
