package dk.dbc.dataio.gui.client.pages.item.show;


import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.ColumnSortList;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.helpers.SortHelper;

import java.util.Comparator;
import java.util.List;

/**
 * This class is the View class for the Items Show View
 */
public class View extends ViewWidget {
    ListDataProvider<ItemModel> dataProvider;
    ColumnSortEvent.ListHandler<ItemModel> columnSortHandler;
    Column itemNumberColumn;

    /**
     * Default constructor
     *
     * @param header The header text for the View
     * @param texts  The I8n texts for this view
     */
    public View(String header, Texts texts) {
        super(header, texts);
        setupColumns();
    }


    /**
     * This method is used to put data into the view
     *
     * @param itemModels The list of items to put into the view
     */
    public void setItems(List<ItemModel> itemModels) {
        dataProvider.getList().clear();
        dataProvider.getList().addAll(itemModels);

        // Do sort by item number
        ColumnSortList columnSortList = itemsTable.getColumnSortList();
        columnSortList.clear();  // Clear the Sort List
        columnSortList.push(itemNumberColumn);  // Default sorting is by item number
        ColumnSortEvent.fire(itemsTable, columnSortList);  // Do sort right now

        // Set page size parameters
        itemsTable.setPageSize(PAGE_SIZE);
        itemsTable.setRowCount(itemModels.size());
    }


    /**
     * Private methods
     */

    /**
     * This method sets up all columns in the view
     * It is called before data has been applied to the view - data is being applied in the setItems method
     */
    @SuppressWarnings("unchecked")
    private void setupColumns() {
        dataProvider = new ListDataProvider<ItemModel>();
        dataProvider.addDataDisplay(itemsTable);

        columnSortHandler = new ColumnSortEvent.ListHandler<ItemModel>(dataProvider.getList());
        itemsTable.addColumnSortHandler(columnSortHandler);

        itemsTable.addColumn(itemNumberColumn = constructItemColumn(), texts.column_Item());
        itemsTable.addColumn(constructStatusColumn(), texts.column_Status());

        itemsTable.setSelectionModel(constructSelectionModel());
        pager.setDisplay(itemsTable);
    }

    /**
     * This method constructs the Item column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Item column
     */
    Column constructItemColumn() {
        TextColumn<ItemModel> column = new TextColumn<ItemModel>() {
            @Override
            public String getValue(ItemModel model) {
                return texts.text_Item() + " " + model.getItemNumber();
            }
        };
        column.setSortable(true);
        columnSortHandler.setComparator(column, new Comparator<ItemModel>() {
            public int compare(ItemModel o1, ItemModel o2) {
                return SortHelper.validateObjects(o1, o2) ? SortHelper.compareLongs(Long.valueOf(o1.getItemNumber()), Long.valueOf(o2.getItemNumber())) : 1;
            }
        });
        column.setDefaultSortAscending(true);  // Set default sort order for Item Number Column to Ascending
        return column;
    }

    /**
     * This method constructs the Status column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Status column
     */
    Column constructStatusColumn() {
        TextColumn<ItemModel> column = new TextColumn<ItemModel>() {
            @Override
            public String getValue(ItemModel model) {
                return formatStatus(model.getStatus());
            }
        };
        column.setSortable(true);
        columnSortHandler.setComparator(column, new Comparator<ItemModel>() {
            public int compare(ItemModel o1, ItemModel o2) {
                return SortHelper.validateObjects(o1, o2) ? SortHelper.compareStrings(formatStatus(o1.getStatus()), formatStatus(o2.getStatus())) : 1;
            }
        });
        column.setDefaultSortAscending(false);  // Set default sort order for Status Column to Descending
        return column;
    }



    /*
     * Private methods
     */

    private String formatStatus(ItemModel.LifeCycle lifeCycle) {
        switch (lifeCycle) {
            case PARTITIONING:
                return texts.lifecycle_Partitioning();
            case PROCESSING:
                return texts.lifecycle_Processing();
            case DELIVERING:
                return texts.lifecycle_Delivering();
            case DONE:
                return texts.lifecycle_Done();
            default:
                return texts.lifecycle_Unknown();
        }
    }

    /*
     * Private classes
     */

    private SelectionModel constructSelectionModel() {
        final NoSelectionModel<ItemModel> selectionModel = new NoSelectionModel<ItemModel>();
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            public void onSelectionChange(SelectionChangeEvent event) {
                ItemModel selected = selectionModel.getLastSelectedObject();
                if (selected != null) {
                    presenter.itemSelected(selected);
                }
            }
        });
        return selectionModel;
    }


}
