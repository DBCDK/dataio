package dk.dbc.dataio.gui.client.pages.faileditems;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel;
import dk.dbc.dataio.gui.client.model.FailedItemModel;
import dk.dbc.dataio.gui.client.views.ContentPanel;

import java.util.ArrayList;
import java.util.List;

public class View extends ContentPanel<Presenter> implements IsWidget {
    private Texts texts;

    @Override
    public void init() {
    }

    interface ViewUiBinder extends UiBinder<Widget, View> {}

    private static ViewUiBinder uiBinder = GWT.create(ViewUiBinder.class);

    @UiField CellTable cellTable;

    private ListDataProvider<FailedItemModel> dataProvider;
    private List<FailedItemModel> failedItemsList;


    public View(String header, Texts texts) {
        super(header);
        this.texts = texts;
        add(uiBinder.createAndBindUi(this));
        setupColumns();
    }

    private void setupColumns() {
        dataProvider = new ListDataProvider<FailedItemModel>();
        failedItemsList = new ArrayList<FailedItemModel>();
        dataProvider.setList(failedItemsList);
        dataProvider.addDataDisplay(cellTable);

        cellTable.addColumn(constructJobIdColumn(), texts.label_JobId());
        cellTable.addColumn(constructChunkIdColumn(), texts.label_ChunkId());
        cellTable.addColumn(constructItemIdColumn(), texts.label_ItemId());
        cellTable.addColumn(constructChunkifyColumn(), texts.label_Chunkify());
        cellTable.addColumn(constructProcessingColumn(), texts.label_Processing());
        cellTable.addColumn(constructDeliveryColumn(), texts.label_Delivery());

        cellTable.setSelectionModel(constructSelectionModel());
    }

    private Column constructJobIdColumn() {
        return new TextColumn<FailedItemModel>() {
            @Override
            public String getValue(FailedItemModel model) {
                return model.getJobId();
            }
        };
    }

    private Column constructChunkIdColumn() {
        return new TextColumn<FailedItemModel>() {
            @Override
            public String getValue(FailedItemModel model) {
                return model.getChunkId();
            }
        };
    }

    private Column constructItemIdColumn() {
        return new TextColumn<FailedItemModel>() {
            @Override
            public String getValue(FailedItemModel model) {
                return model.getItemId();
            }
        };
    }

    private Column constructChunkifyColumn() {
        return new TextColumn<FailedItemModel>() {
            @Override
            public String getValue(FailedItemModel model) {
                return model.getChunkifyState();
            }
        };
    }

    private Column constructProcessingColumn() {
        return new TextColumn<FailedItemModel>() {
            @Override
            public String getValue(FailedItemModel model) {
                return model.getProcessingState();
            }
        };
    }

    private Column constructDeliveryColumn() {
        return new TextColumn<FailedItemModel>() {
            @Override
            public String getValue(FailedItemModel model) {
                return model.getDeliveryState();
            }
        };
    }

    private SelectionModel constructSelectionModel() {
        final NoSelectionModel<FailedItemModel> selectionModel = new NoSelectionModel<FailedItemModel>();
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            public void onSelectionChange(SelectionChangeEvent event) {
                FailedItemModel selected = selectionModel.getLastSelectedObject();
                if (selected != null) {
                    presenter.failedItemSelected(new FailedItemModel(selected.getJobId(), selected.getChunkId(), selected.getItemId(),
                            selected.getChunkifyState(), selected.getProcessingState(), selected.getDeliveryState()));
                }
            }
        });
        return selectionModel;
    }

    /*
     * Public methods
     */

    public void clearFailedItemsList() {
        failedItemsList = new ArrayList<FailedItemModel>();
        dataProvider.setList(failedItemsList);
    }

    public void addFailedItem(FailedItemModel failedItem) {
        failedItemsList.add(failedItem);
        dataProvider.setList(failedItemsList);
    }

}

