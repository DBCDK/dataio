package dk.dbc.dataio.gui.client.presenters;

import java.util.List;

public interface FlowbinderCreatePresenter extends Presenter {
    void saveFlowbinder(String name, String description, String packaging, String format, String charset, String destination, String recordSplitter, String flow, List<String> submitters);
}
