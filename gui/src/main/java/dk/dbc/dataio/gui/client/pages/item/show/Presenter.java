package dk.dbc.dataio.gui.client.pages.item.show;


import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void itemSelected(ItemsListView listView, ItemModel itemModel);
    void allItemsTabSelected();
    void failedItemsTabSelected();
    void ignoredItemsTabSelected();
    void jobInfoTabSelected();
}
