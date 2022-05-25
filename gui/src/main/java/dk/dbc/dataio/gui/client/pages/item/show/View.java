package dk.dbc.dataio.gui.client.pages.item.show;


import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.CheckboxCell;
import com.google.gwt.cell.client.TextCell;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.Header;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.gui.client.components.ClickableImageResourceCell;
import dk.dbc.dataio.gui.client.model.DiagnosticModel;
import dk.dbc.dataio.gui.client.model.ItemModel;
import dk.dbc.dataio.gui.client.resources.Resources;

import java.util.List;


/**
 * This class is the View class for the Items Show View
 */
public class View extends ViewWidget {

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);

    protected Boolean fixedColumnVisible = false;
    SingleSelectionModel<ItemModel> selectionModel = new SingleSelectionModel<>();
    HandlerRegistration handlerRegistration;
    public AsyncItemViewDataProvider dataProvider;

    public View() {
        this(true);
    }

    /*
     * Package scoped Constructor used for unit testing.
     */
    View(Boolean setupColumns) {
        super("");
        if (setupColumns) {
            dataProvider = new AsyncItemViewDataProvider(this);
            setupColumns(itemsListView);
            dataProvider.addDataDisplay(itemsListView.itemsTable);
            itemsListView.itemsTable.setSelectionModel(selectionModel);
            setupColumns(jobDiagnosticTabContent);
            handlerRegistration = selectionModel.addSelectionChangeHandler(new SelectionChangeHandlerClass());
        }

    }

    /**
     * Force a data refresh of the itemsTables belonging to allItemsList, failedItemsList, ignoredItemsList.
     */
    public void refreshItemsTable() {
        itemsListView.itemsTable.setVisibleRangeAndClearData(new Range(0, 20), true);
    }

    /**
     * Stores the itemModels given as a parameter to the presenter
     *
     * @param itemModels The itemModels to store to the presenter
     */
    public void setItemModels(List<ItemModel> itemModels) {
        presenter.setItemModels(itemsListView, itemModels);
    }

    /**
     * Private methods
     */

    /**
     * This method sets up all columns in the view
     * It is called before data has been applied to the view - data is being applied in the setItems method
     */
    @SuppressWarnings("unchecked")
    void setupColumns(final ItemsListView listView) {
        itemsListView.itemsTable.setWidth("100%", true);
        itemsListView.itemsTable.addColumn(constructItemColumn(), getTexts().column_Item());
        itemsListView.itemsTable.addColumn(constructRecordIdColumn(), getTexts().column_RecordId());
        itemsListView.itemsTable.addColumn(constructStatusColumn(), getTexts().column_Status());
        itemsListView.itemsTable.addColumn(constructFixedColumn(), new HidableColumnHeader(getTexts().column_Fixed()));
        itemsListView.itemsTable.addColumn(constructTrackingIdColumn(), getTexts().column_Trace());
        itemsListView.itemsTable.setVisibleRange(0, 20);

        itemsPager.setDisplay(listView.itemsTable);

        ignoredItemsListTab.add(itemsListView.itemsTable);
        ignoredItemsListTab.add(itemsPager);
        allItemsListTab.add(itemsListView.itemsTable);
        allItemsListTab.add(itemsPager);
        failedItemsListTab.add(itemsListView.itemsTable);
        failedItemsListTab.add(itemsPager);

    }

    @SuppressWarnings("unchecked")
    void setupColumns(final JobDiagnosticTabContent jobDiagnosticTabContent) {
        jobDiagnosticTabContent.jobDiagnosticTable.addColumn(constructDiagnosticLevelColumn(), getTexts().column_Level());
        jobDiagnosticTabContent.jobDiagnosticTable.addColumn(constructDiagnosticMessageColumn(), getTexts().column_Message());
        jobDiagnosticTabContent.jobDiagnosticTable.addColumn(constructDiagnosticStacktraceColumn(), getTexts().column_Stacktrace());
    }

    Column constructDiagnosticLevelColumn() {
        return new TextColumn<DiagnosticModel>() {
            @Override
            public String getValue(DiagnosticModel model) {
                return model.getLevel();
            }
        };
    }

    Column constructDiagnosticMessageColumn() {
        return new TextColumn<DiagnosticModel>() {
            @Override
            public String getValue(DiagnosticModel model) {
                return model.getMessage();
            }
        };
    }

    Column constructDiagnosticStacktraceColumn() {
        final Resources resources = GWT.create(Resources.class);
        final ClickableImageResourceCell statusCell = new ClickableImageResourceCell();

        Column column = new Column(statusCell) {
            @Override
            public Object getValue(Object model) {
                final String stacktrace = ((DiagnosticModel) model).getStacktrace();
                if (stacktrace != null && !stacktrace.isEmpty()) {
                    return resources.plusUpButton();
                } else {
                    return resources.emptyIcon();
                }
            }
        };
        column.setFieldUpdater((index, model, value) -> {
            final String stacktrace = ((DiagnosticModel) model).getStacktrace();
            if (stacktrace != null && !stacktrace.isEmpty()) {
                Window.alert(((DiagnosticModel) model).getStacktrace());
            }
        });
        return column;

    }


    /**
     * This method constructs the Item column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Item column
     */
    Column constructItemColumn() {
        return new TextColumn<ItemModel>() {
            @Override
            public String getValue(ItemModel model) {
                return getTexts().text_Item() + " " + model.getItemNumber();
            }
        };
    }

    /**
     * This method constructs the RecordId column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed RecordId column
     */
    Column constructRecordIdColumn() {
        return new TextColumn<ItemModel>() {
            @Override
            public String getValue(ItemModel model) {
                return model.getRecordId();
            }
        };
    }

    /**
     * This method constructs the Status column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Status column
     */
    Column constructStatusColumn() {
        return new TextColumn<ItemModel>() {
            @Override
            public String getValue(ItemModel model) {
                return formatStatus(model.getStatus());
            }
        };
    }

    /**
     * This method constructs the Tracking ID column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Tracking ID column
     */
    Column constructTrackingIdColumn() {
        Column<ItemModel, String> column = new Column<ItemModel, String>(new TrackingButtonCell()) {
            @Override
            public String getValue(ItemModel model) {
                return getTexts().button_Trace();
            }
        };
        column.setFieldUpdater((index, config, buttonText) -> presenter.traceItem(config.getTrackingId()));
        return column;
    }

    /**
     * This method constructs a double click event handler. On double click event, the method calls
     * the presenter with the selection model selected value.
     *
     * @return the double click handler
     */
    Column constructFixedColumn() {
        CheckboxCell checkboxCell = new CheckboxCell(true, false);
        return new Column<ItemModel, Boolean>(checkboxCell) {
            @Override
            public Boolean getValue(ItemModel itemModel) {
                return itemModel.getWorkflowNoteModel().isProcessed();
            }

            @Override
            public void onBrowserEvent(Cell.Context context, Element elem, ItemModel itemModel, NativeEvent event) {
                if (Event.as(event).getTypeInt() == Event.ONCHANGE) {
                    presenter.setWorkflowNoteModel(itemModel, ((InputElement) elem.getFirstChild()).isChecked());
                }
                super.onBrowserEvent(context, elem, itemModel, event);
            }

            @Override
            public String getCellStyleNames(Cell.Context context, ItemModel model) {
                return fixedColumnVisible ? "visible center" : "invisible";
            }
        };
    }

    private String formatStatus(ItemModel.LifeCycle lifeCycle) {
        switch (lifeCycle) {
            case PARTITIONING:
                return getTexts().lifecycle_Partitioning();
            case PROCESSING:
                return getTexts().lifecycle_Processing();
            case DELIVERING:
                return getTexts().lifecycle_Delivering();
            case DONE:
                return getTexts().lifecycle_Done();
            case PARTITIONING_FAILED:
                return getTexts().lifecycle_PartitioningFailed();
            case PARTITIONING_IGNORED:
                return getTexts().lifecycle_PartitioningIgnored();
            case PROCESSING_FAILED:
                return getTexts().lifecycle_ProcessingFailed();
            case PROCESSING_IGNORED:
                return getTexts().lifecycle_ProcessingIgnored();
            case DELIVERING_FAILED:
                return getTexts().lifecycle_DeliveringFailed();
            case DELIVERING_IGNORED:
                return getTexts().lifecycle_DeliveringIgnored();
            default:
                return getTexts().lifecycle_Unknown();
        }
    }

    Texts getTexts() {
        return viewInjector.getTexts();
    }

    /*
     * Private classes
     */

    /**
     * Selection Handler class - to take action upon selection of a row
     */
    class SelectionChangeHandlerClass implements SelectionChangeEvent.Handler {
        public SelectionChangeHandlerClass() {
            super();
        }

        public void onSelectionChange(SelectionChangeEvent event) {
            ItemModel selected = selectionModel.getSelectedObject();
            if (selected != null) {
                presenter.itemSelected(itemsListView, selected);
            }
        }
    }

    /**
     * Normal Column Header class (to be hidden upon request)
     */
    class HidableColumnHeader extends Header<String> {
        private String headerText;

        public HidableColumnHeader(String text) {
            super(new TextCell());
            headerText = text;
        }

        @Override
        public String getValue() {
            return headerText;
        }

        @Override
        public String getHeaderStyleNames() {
            return fixedColumnVisible ? "visible" : "invisible";
        }
    }

    /**
     * Specialized version of a ButtonCell, sets up a Mouseover text with the tracking id
     */
    class TrackingButtonCell extends ButtonCell {
        @Override
        public void render(Context context, SafeHtml data, SafeHtmlBuilder sb) {
            ItemModel model = (ItemModel) context.getKey();
            sb.appendHtmlConstant("<span title='" + getTexts().text_TrackingId() + " " + model.getTrackingId() + "'>");
            super.render(context, data, sb);
            sb.appendHtmlConstant("</span>");
        }
    }
}
