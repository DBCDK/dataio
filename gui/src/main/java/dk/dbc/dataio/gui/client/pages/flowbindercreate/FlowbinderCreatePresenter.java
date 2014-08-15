package dk.dbc.dataio.gui.client.pages.flowbindercreate;

import dk.dbc.dataio.gui.client.presenters.GenericPresenter;
import java.util.List;

public interface FlowbinderCreatePresenter extends GenericPresenter {
    void saveFlowbinder(String name, String description, String packaging, String format, String charset, String destination, String recordSplitter, String flow, List<String> submitters, String sink);
}
