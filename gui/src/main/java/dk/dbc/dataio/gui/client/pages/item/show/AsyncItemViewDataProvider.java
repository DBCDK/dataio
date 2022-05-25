package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.view.client.AsyncDataProvider;
import com.google.gwt.view.client.HasData;
import com.google.gwt.view.client.Range;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.jobstore.types.criteria.ItemListCriteria;

import java.util.List;


public class AsyncItemViewDataProvider extends AsyncDataProvider<ItemModel> {

    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);
    private View view;
    private int criteriaIncarnation = 0;
    private ItemListCriteria currentCriteria = new ItemListCriteria();

    ItemListCriteria baseCriteria = null;
    ItemListCriteria.Field searchType;

    public AsyncItemViewDataProvider(View view) {
        this.view = view;

        updateCurrentCriteria();
    }

    void setBaseCriteria(ItemListCriteria.Field searchType, ItemListCriteria newBaseCriteria) {
        this.searchType = searchType;
        baseCriteria = newBaseCriteria;
        updateCurrentCriteria();
    }

    void updateCurrentCriteria() {
        ItemListCriteria newItemListCriteria = new ItemListCriteria();

        if (baseCriteria != null) {
            newItemListCriteria.and(baseCriteria);
        }

        if (!currentCriteria.equals(newItemListCriteria)) {
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
     * @param display Display to get the VisibleRange from
     */
    @Override
    protected void onRangeChanged(final HasData<ItemModel> display) {
        // Get the new range.
        final Range range = display.getVisibleRange();

        currentCriteria.limit(range.getLength());
        currentCriteria.offset(range.getStart());

        if (searchType != null) {
            commonInjector.getJobStoreProxyAsync().listItems(searchType, currentCriteria, new FilteredAsyncCallback<List<ItemModel>>() {
                        // protection against old calls updating the view with old data.
                        int criteriaIncarnationOnRequestCall = criteriaIncarnation;
                        int offsetOnRequestCall = currentCriteria.getOffset();

                        @Override
                        public void onSuccess(List<ItemModel> itemModels) {
                            if (dataIsStillValid()) {
                                updateRowData(range.getStart(), itemModels);
                                view.setItemModels(itemModels);
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
     * Fetch a new count..
     */
    public void updateCount() {
        commonInjector.getJobStoreProxyAsync().countItems(currentCriteria, new FilteredAsyncCallback<Long>() {
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
