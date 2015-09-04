package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.util.ClientFactory;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;

import java.util.List;


public class AsyncItemViewDataProvider extends AsyncDataProvider<ItemModel>  {

    private JobStoreProxyAsync jobStoreProxy;
    private View view;
    private int criteriaIncarnation = 0;
    private ItemListCriteria currentCriteria = new ItemListCriteria();

    ItemListCriteria baseCriteria = null;
    ItemsListView listView;
    ItemListCriteria.Field searchType;

    public AsyncItemViewDataProvider(ClientFactory clientFactory, View view) {
        jobStoreProxy = clientFactory.getJobStoreProxyAsync();
        this.view = view;

        updateCurrentCriteria();
    }

    void setBaseCriteria(ItemListCriteria.Field searchType, ItemsListView listView, ItemListCriteria newBaseCriteria) {
        this.searchType = searchType;
        this.listView = listView;
        baseCriteria = newBaseCriteria;
        updateCurrentCriteria();
    }

    void updateCurrentCriteria() {
        ItemListCriteria newItemListCriteria = new ItemListCriteria();

        if( baseCriteria != null) {
            newItemListCriteria.and(baseCriteria);
        }

        if( !currentCriteria.equals(newItemListCriteria)) {
            criteriaIncarnation++;
            currentCriteria = newItemListCriteria;
            refresh();
        }
    }

    void refresh() {
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

        currentCriteria.limit(range.getLength());
        currentCriteria.offset(range.getStart());

        if(searchType != null) {
            jobStoreProxy.listItems(searchType, currentCriteria, new FilteredAsyncCallback<List<ItemModel>>() {
                        // protection against old calls updating the view with old data.
                        int criteriaIncarnationOnRequestCall = criteriaIncarnation;
                        int offsetOnRequestCall = currentCriteria.getOffset();

                        @Override
                        public void onSuccess(List<ItemModel> itemModels) {
                            if (dataIsStillValid()) {
                                updateRowData(range.getStart(), itemModels);
                                view.setItemModels(listView, itemModels);
                            }
                        }

                        @Override
                        public void onFilteredFailure(Throwable e) {
                            view.setErrorText(e.getClass().getName() + " - " + e.getMessage());
                        }


                        private boolean dataIsStillValid() {
                            return criteriaIncarnationOnRequestCall == criteriaIncarnation &&
                                    offsetOnRequestCall == currentCriteria.getOffset();
                        }
                    }
            );
            updateCount();
        }
    }

    /**
     *  Fetch a new count..
     *
     */
    public void updateCount()  {
        jobStoreProxy.countItems(currentCriteria, new FilteredAsyncCallback<Long>() {
            // protection against old calls updating the view with old data.
            int criteriaIncarnationOnCall = criteriaIncarnation;

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
                return criteriaIncarnationOnCall == criteriaIncarnation;
            }
        });
    }

}
