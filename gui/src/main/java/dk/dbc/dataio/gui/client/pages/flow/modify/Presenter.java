package dk.dbc.dataio.gui.client.pages.flow.modify;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

import java.util.Map;

public interface Presenter extends GenericPresenter {
    void nameChanged(String name);

    void descriptionChanged(String description);

    void flowComponentsChanged(Map<String, String> flowComponents);

    void keyPressed();

    void saveButtonPressed();

    void addButtonPressed();

    void removeButtonPressed();

    void deleteButtonPressed();

    void selectFlowComponentButtonPressed(Map<String, String> selectedKeys);

    void newFlowComponentButtonPressed();
}
