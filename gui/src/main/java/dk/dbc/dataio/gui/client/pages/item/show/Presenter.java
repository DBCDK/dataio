package dk.dbc.dataio.gui.client.pages.item.show;


import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

import java.util.List;

public interface Presenter extends GenericPresenter {
    void itemSelected(ItemsListView listView, ItemModel itemModel);

    void setItemModels(ItemsListView listView, List<ItemModel> itemModels);

    void allItemsTabSelected();

    void failedItemsTabSelected();

    void ignoredItemsTabSelected();

    void noteTabSelected();

    void hideDetailedTabs();

    void recordSearch();

    void setWorkflowNoteModel(String description);

    void setWorkflowNoteModel(ItemModel itemModel, boolean isProcessed);

    void traceItem(String trackingId);
}
