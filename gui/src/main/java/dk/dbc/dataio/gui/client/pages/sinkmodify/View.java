package dk.dbc.dataio.gui.client.pages.sinkmodify;

import dk.dbc.dataio.gui.client.views.GenericView;

public interface View extends GenericView<Presenter> {
    void setName(String name);
    String getName();
    void setResource(String resource);
    String getResource();
}
