package dk.dbc.dataio.gui.client.pages.sink.modify;

import dk.dbc.dataio.gui.client.views.GenericView;

public interface ViewOld extends GenericView<Presenter> {
    void initializeFields();
    void setName(String name);
    String getName();
    void setResource(String resource);
    String getResource();
}
