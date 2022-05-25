package dk.dbc.dataio.gui.client.pages.harvester.rr.modify;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;


public interface Presenter extends GenericPresenter {
    void nameChanged(String name);

    void descriptionChanged(String text);

    void resourceChanged(String resource);

    void consumerIdChanged(String consumerId);

    void sizeChanged(String size);

    String formatOverrideAdded(String overrideKey, String overrideValue);

    void relationsChanged(Boolean relations);

    void expandChanged(Boolean expand);

    void libraryRulesChanged(Boolean libraryRules);

    void harvesterTypeChanged(String value);

    void holdingsTargetChanged(String text);

    void destinationChanged(String destination);

    void formatChanged(String format);

    void typeChanged(String type);

    void noteChanged(String text);

    void enabledChanged(Boolean value);

    void keyPressed();

    void saveButtonPressed();

    void updateButtonPressed();

    void deleteButtonPressed();

    void formatOverridesAddButtonPressed();

    void formatOverridesRemoveButtonPressed(String item);
}
