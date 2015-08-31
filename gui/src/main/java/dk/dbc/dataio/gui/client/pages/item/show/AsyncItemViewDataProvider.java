package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.ItemListCriteriaModel;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;

import java.util.List;


public class AsyncItemViewDataProvider extends AsyncDataProvider<ItemModel>  {

    private JobStoreProxyAsync jobStoreProxy;
    private View view;

    ItemListCriteriaModel baseCriteria;
    ItemListCriteriaModel currentCriteria;
    ItemsListView listView;

    public AsyncItemViewDataProvider(ClientFactory clientFactory, View view) {
        jobStoreProxy = clientFactory.getJobStoreProxyAsync();
        this.view = view;
        baseCriteria = new ItemListCriteriaModel();

        updateCurrentCriteria();
    }

    void setBaseCriteria(ItemsListView listView, ItemListCriteriaModel newBaseCriteria) {
        this.listView = listView;
        if(baseCriteria.equals(newBaseCriteria)) {
            return;
        }
        baseCriteria = newBaseCriteria;
        updateCurrentCriteria();
    }

    void updateCurrentCriteria() {
        ItemListCriteriaModel newCurrentCriteria = new ItemListCriteriaModel();
        newCurrentCriteria.and(baseCriteria);

        if( !newCurrentCriteria.equals( currentCriteria )) {
            currentCriteria = newCurrentCriteria;
            refresh();
        }
    }

    void refresh( ) {
        view.refreshItemsTable();
    }

    /**
     * The Worker function of tha Async Data Provider.
     *
     *
     * @param display Display to get the VisibleRange from
     *
     */
    @Override
    protected void onRangeChanged(final HasData<ItemModel> display) {
        // Get the new range.
        final Range range = display.getVisibleRange();

        currentCriteria.setLimit(range.getLength());
        currentCriteria.setOffset(range.getStart());

        jobStoreProxy.listItems(currentCriteria, new FilteredAsyncCallback<List<ItemModel>>() {
                    // protection against old calls updating the view with old data.
                    ItemListCriteriaModel criteriaOnRequestCall = currentCriteria;
                    int offsetOnRequestCall = currentCriteria.getOffset();

                    @Override
                    public void onSuccess(List<ItemModel> itemModels) {
                        if (dataIsStillValid()) {
                            updateRowData(range.getStart(), itemModels);
//                            view.setItemModels(listView, itemModels);
                        }
                    }

                    @Override
                    public void onFilteredFailure(Throwable e) {
                        view.setErrorText(e.getClass().getName() + " - " + e.getMessage());
                    }


                    private boolean dataIsStillValid() {
                        return criteriaOnRequestCall.equals(currentCriteria) &&
                                offsetOnRequestCall == criteriaOnRequestCall.getOffset();
                    }
                }
        );
        updateCount();
    }

    /**
     *  Fetch a new count..
     *
     */
    public void updateCount()  {
        jobStoreProxy.countItems(currentCriteria, new FilteredAsyncCallback<Long>() {
            // protection against old calls updating the view with old data.
            ItemListCriteriaModel criteriaOnRequestCall = currentCriteria;

            @Override
            public void onSuccess(Long count) {
                if (dataIsStillValid()) {
                    updateRowCount(count.intValue(), true);
                }
            }

            @Override
            public void onFilteredFailure(Throwable e) {
                view.setErrorText(e.getClass().getName() + " - " + e.getMessage());
            }

            private boolean dataIsStillValid() {
                return criteriaOnRequestCall.equals(currentCriteria);
            }
        });
    }

}
