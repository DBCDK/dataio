package dk.dbc.dataio.gui.client.pages.flowbinder.modify;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

import java.util.Map;

public interface Presenter extends GenericPresenter {
    void nameChanged(String text);
    void descriptionChanged(String text);
    void frameChanged(String text);
    void formatChanged(String text);
    void charsetChanged(String text);
    void destinationChanged(String text);
    void recordSplitterChanged(String text);
    void sequenceAnalysisChanged(boolean value);
    void submittersChanged(Map<String, String> selectedItems);
    void flowChanged(String selectedText);
    void sinkChanged(String selectedText);
    void keyPressed();
    void saveButtonPressed();
}
