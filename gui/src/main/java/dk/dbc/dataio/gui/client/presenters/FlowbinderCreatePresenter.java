package dk.dbc.dataio.gui.client.presenters;

public interface FlowbinderCreatePresenter extends Presenter {
    void saveFlowbinder(String name, String frameFormat, String contentFormat, String characterSet, String sink, String recordSplitter);
}
