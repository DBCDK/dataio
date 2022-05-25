package dk.dbc.dataio.gui.client.pages.flowbinder.show;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ButtonCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.util.Format;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Flow Binders Table
 */
public class FlowBindersTable extends CellTable {
    private ViewGinjector viewGinjector = GWT.create(ViewGinjector.class);
    Texts texts = viewGinjector.getTexts();
    private View view;
    private Presenter presenter = null;
    ListDataProvider<FlowBinderModel> dataProvider;


    public FlowBindersTable(View view) {
        this.view = view;
        dataProvider = new ListDataProvider<>();
        dataProvider.addDataDisplay(this);

        Column nameColumn = constructColumn(FlowBinderModel::getName);
        Column descriptionColumn = constructColumn(FlowBinderModel::getDescription);
        Column packagingColumn = constructColumn(FlowBinderModel::getPackaging);
        Column formatColumn = constructColumn(FlowBinderModel::getFormat);
        Column charsetColumn = constructColumn(FlowBinderModel::getCharset);
        Column destinationColumn = constructColumn(FlowBinderModel::getDestination);
        Column recordSplitterColumn = constructColumn(FlowBinderModel::getRecordSplitter);
        Column submitterColumn = new SubmitterColumn();
        Column flowColumn = constructColumn(model -> model.getFlowModel().getFlowName());
        Column sinkColumn = constructColumn(model -> model.getSinkModel().getSinkName());
        Column queueProviderColumn = constructColumn(FlowBinderModel::getQueueProvider);
        Column actionColumn = constructActionColumn();

        addColumn(nameColumn, texts.columnHeader_Name());
        addColumn(descriptionColumn, texts.columnHeader_Description());
        addColumn(packagingColumn, textWithToolTip(texts.columnHeader_Packaging(), texts.help_Packaging()));
        addColumn(formatColumn, textWithToolTip(texts.columnHeader_Format(), texts.help_Format()));
        addColumn(charsetColumn, textWithToolTip(texts.columnHeader_Charset(), texts.help_Charset()));
        addColumn(destinationColumn, textWithToolTip(texts.columnHeader_Destination(), texts.help_Destination()));
        addColumn(recordSplitterColumn, texts.columnHeader_RecordSplitter());
        addColumn(submitterColumn, texts.columnHeader_Submitters());
        addColumn(flowColumn, texts.columnHeader_Flow());
        addColumn(sinkColumn, texts.columnHeader_Sink());
        addColumn(queueProviderColumn, texts.columnHeader_QueueProvider());
        addColumn(actionColumn, texts.columnHeader_Action());

        addColumnSortHandler(constructStringSortHandler(nameColumn, FlowBinderModel::getName));
        addColumnSortHandler(constructStringSortHandler(descriptionColumn, FlowBinderModel::getDescription));
        addColumnSortHandler(constructStringSortHandler(packagingColumn, FlowBinderModel::getPackaging));
        addColumnSortHandler(constructStringSortHandler(formatColumn, FlowBinderModel::getFormat));
        addColumnSortHandler(constructStringSortHandler(charsetColumn, FlowBinderModel::getCharset));
        addColumnSortHandler(constructStringSortHandler(destinationColumn, FlowBinderModel::getDestination));
        addColumnSortHandler(constructStringSortHandler(recordSplitterColumn, FlowBinderModel::getRecordSplitter));
        addColumnSortHandler(constructStringSortHandler(flowColumn, (model) -> model.getFlowModel().getFlowName()));
        addColumnSortHandler(constructStringSortHandler(sinkColumn, (model) -> model.getSinkModel().getSinkName()));
        addColumnSortHandler(constructStringSortHandler(queueProviderColumn, FlowBinderModel::getQueueProvider));

        getColumnSortList().push(nameColumn);  // Default sorting is chosen here

        setSelectionModel(new SingleSelectionModel<FlowBinderModel>());
        addDomHandler(getDoubleClickHandler(), DoubleClickEvent.getType());
    }

    /**
     * Sets the presenter to allow communication back to the presenter
     *
     * @param presenter The presenter to set
     */
    public void setPresenter(Presenter presenter) {
        this.presenter = presenter;
    }


    /**
     * This method is used to put data into the view
     *
     * @param flowBinderModels The list of flowbinders to put into the view
     */
    public void setFlowBinders(List<FlowBinderModel> flowBinderModels) {
        dataProvider.getList().clear();
        dataProvider.getList().addAll(flowBinderModels);
    }

    public void clear() {
        dataProvider.getList().clear();
    }

    /**
     * This method constructs a column
     *
     * @return the constructed column
     */
    private Column constructColumn(Function<FlowBinderModel, String> extractor) {
        return new TextColumn<FlowBinderModel>() {
            @Override
            public String getValue(FlowBinderModel model) {
                return extractor.apply(model);
            }
        };
    }

    /**
     * This method constructs the Action column
     * Should have been private, but is package-private to enable unit test
     *
     * @return the constructed Action column
     */
    @SuppressWarnings("unchecked")
    private Column constructActionColumn() {
        Column column = new Column<FlowBinderModel, String>(new ButtonCell()) {
            @Override
            public String getValue(FlowBinderModel model) {
                // The value to display in the button.
                return texts.button_Edit();
            }
        };
        column.setFieldUpdater(new FieldUpdater<FlowBinderModel, String>() {
            @Override
            public void update(int index, FlowBinderModel model, String buttonText) {
                editFlowBinder(model);
            }
        });
        return column;
    }

    /**
     * This method constructs a sort handler for a String column
     *
     * @param column The column to sort
     * @return The list handler for the column
     */
    ColumnSortEvent.ListHandler constructStringSortHandler(Column column, Function<FlowBinderModel, String> keyExtractor) {
        ColumnSortEvent.ListHandler<FlowBinderModel> columnSortHandler = new ColumnSortEvent.ListHandler<>(dataProvider.getList());
        columnSortHandler.setComparator(column, Comparator.comparing(keyExtractor.andThen(s -> s == null ? "" : s)));
        column.setSortable(true);
        return columnSortHandler;
    }

    /**
     * This method constructs a double click event handler. On double click event, the method calls
     * the presenter with the selection model selected value.
     *
     * @return the double click handler
     */
    DoubleClickHandler getDoubleClickHandler() {
        return doubleClickEvent -> {
            editFlowBinder(((SingleSelectionModel<FlowBinderModel>) getSelectionModel()).getSelectedObject());
        };
    }

    /**
     * This method activates the edit flowbinder page
     *
     * @param model The model to edit
     */
    private void editFlowBinder(FlowBinderModel model) {
        if (presenter != null && model != null) {
            presenter.editFlowBinder(model);
        }
    }

    /**
     * This metods constructs a SafeHtml snippet, that constitutes a text with a popup mouseover help text
     *
     * @param headerText The headertext to be displayed
     * @param helpText   The popup help text
     * @return The SafeHtml snippet
     */
    SafeHtml textWithToolTip(String headerText, String helpText) {
        return SafeHtmlUtils.fromSafeConstant("<span title='" + helpText + "'>" + headerText + "</span>");
    }


    /*
     * Private classes
     */

    class SafeHtmlCell extends AbstractCell<SafeHtml> {
        public SafeHtmlCell() {
            super("click", "keydown");
        }

        @Override
        public void render(Cell.Context context, SafeHtml value, SafeHtmlBuilder sb) {
            if (value != null) {
                sb.append(value);
            }
        }
    }

    class SubmitterColumn extends Column<FlowBinderModel, SafeHtml> {
        public SubmitterColumn(Cell<SafeHtml> cell) {
            super(cell);
        }

        public SubmitterColumn() {
            this(new SafeHtmlCell());
        }

        @Override
        public SafeHtml getValue(FlowBinderModel model) {
            List<SubmitterModel> models = model.getSubmitterModels();
            SafeHtmlBuilder sb = new SafeHtmlBuilder();
            if (isClickableColumn(models)) {
                sb.appendHtmlConstant("<a href='javascript:;'>");
                sb.append(models.size()).appendEscaped(" ").appendEscaped(texts.text_Submitters());
                sb.appendHtmlConstant("</a>");
            } else {
                if (!models.isEmpty()) {
                    SubmitterModel submitter = models.get(0);
                    sb.appendEscaped(Format.inBracketsPairString(submitter.getNumber(), submitter.getName()));
                }
            }
            return sb.toSafeHtml();
        }

        @Override
        public void onBrowserEvent(Cell.Context context, Element elem, FlowBinderModel model, NativeEvent event) {
            super.onBrowserEvent(context, elem, model, event);
            if (isClickableColumn(model.getSubmitterModels()) && "click".equals(event.getType())) {
                showSubmittersInPopupList(model.getSubmitterModels());
            }
        }

        private boolean isClickableColumn(List<SubmitterModel> models) {
            return models != null && models.size() > 1;
        }

        private void showSubmittersInPopupList(List<SubmitterModel> submitters) {
            Collections.sort(submitters, (sm1, sm2) -> Long.valueOf(sm1.getNumber()).compareTo(Long.valueOf(sm2.getNumber())));
            view.popupList.clear();
            for (SubmitterModel model : submitters) {
                view.popupList.addItem(Format.inBracketsPairString(model.getNumber(), model.getName()), model.getNumber());
            }
            view.popupList.show();
        }
    }
}
