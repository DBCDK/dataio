package dk.dbc.dataio.gui.client.pages.submitter.modify;

import dk.dbc.dataio.gui.client.views.GenericView;

public interface View extends GenericView<Presenter> {
    void initializeFields();
    void setNumber(String number);
    String getNumber();
    void setName(String name);
    String getName();
    void setDescription(String description);
    String getDescription();
}
