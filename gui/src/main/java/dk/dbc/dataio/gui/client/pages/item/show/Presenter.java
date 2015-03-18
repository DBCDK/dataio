package dk.dbc.dataio.gui.client.pages.item.show;


import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void itemSelected(ItemModel itemModel);
    void filterItems();
}
