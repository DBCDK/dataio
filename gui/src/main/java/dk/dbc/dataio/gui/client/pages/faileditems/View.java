package dk.dbc.dataio.gui.client.pages.faileditems;

import com.google.gwt.cell.client.ClickableTextCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.AbstractDataProvider;
import com.google.gwt.view.client.ListDataProvider;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public class View extends ContentPanel<Presenter> implements IsWidget {
    private Texts texts;

    @Override
    public void init() {
    }

    interface ViewUiBinder extends UiBinder<Widget, View> {}

    private static ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);

    @UiField CellTable tableElementCell;

    AbstractDataProvider dataProvider;

    public View(String header, Texts texts) {
        super(header);
        this.texts = texts;
        add(uiBinder.createAndBindUi(this));
        setupColumns();
    }

    private void setupColumns() {
        tableElementCell.addColumn(constructFailedItemsColumn(), texts.label_FailedItems());

        // Connect the table to the data provider
        dataProvider = new ListDataProvider<FailedItemModel>();

    }

    private Column<FailedItemModel, String> constructFailedItemsColumn() {
        Column<FailedItemModel, String> failedItemsColumns = new Column<FailedItemModel, String>(new ClickableTextCell()) {
            @Override
            public String getValue(FailedItemModel failedItemModel) {
                return failedItemModel.getFailedItem();
            }
        };
        failedItemsColumns.setFieldUpdater(new FieldUpdater<FailedItemModel, String>() {
            @Override
            public void update(int index, FailedItemModel object, String value) {
                presenter.failedItemSelected(object.getId());
            }
        });
        return failedItemsColumns;
    }

    public void setFailedItemsDataProvider(AbstractDataProvider failedItemsDataProvider) {
        this.dataProvider = failedItemsDataProvider;
        dataProvider.addDataDisplay(tableElementCell);
    }

}

