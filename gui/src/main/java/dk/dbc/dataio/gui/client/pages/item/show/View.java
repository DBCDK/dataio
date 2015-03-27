package dk.dbc.dataio.gui.client.pages.item.show;


import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.gui.client.model.ItemModel;

import java.util.List;

/**
 * This class is the View class for the Items Show View
 */
public class View extends ViewWidget {
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
     * @param itemModels The list of items to put into the view
     * @param offset the start index
     * @param rowCount the number of rows which should be displayed
     */
    public void setItems(List<ItemModel> itemModels, int offset, int rowCount) {
        itemsTable.setRowCount(rowCount);
        itemsTable.setRowData(offset, itemModels);
    }

    /**
     * Adds a tab to the Tab Panel
     * @param widget The widget to fill into the content area of the tab
     * @param title Til title of the Tab
     */
    public void addTab(Widget widget, String title) {
        if (widget != null) {
            tabPanel.add(widget, title);
        }
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
        itemsTable.addColumn(itemNumberColumn = constructItemColumn(), texts.column_Item());
        itemsTable.addColumn(constructStatusColumn(), texts.column_Status());
        itemsTable.setSelectionModel(constructSelectionModel());

        itemsTable.addRangeChangeHandler(new RangeChangeEvent.Handler() {
            @Override
            public void onRangeChange(RangeChangeEvent event) {
                presenter.filterItems();
            }
        });

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
        final SingleSelectionModel<ItemModel> selectionModel = new SingleSelectionModel<ItemModel>();
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            public void onSelectionChange(SelectionChangeEvent event) {
                ItemModel selected = selectionModel.getSelectedObject();
                if (selected != null) {
                    presenter.itemSelected(selected);
                }
            }
        });
        return selectionModel;
    }

}
