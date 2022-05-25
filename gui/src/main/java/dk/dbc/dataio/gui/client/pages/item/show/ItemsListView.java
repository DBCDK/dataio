package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DecoratedTabPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import dk.dbc.dataio.gui.client.model.DiagnosticModel;

public class ItemsListView extends Composite {
    protected static final String JAVASCRIPT_LOG_TAB_CONTENT = "JAVASCRIPT_LOG_TAB_CONTENT";
    protected static final String INPUT_POST_TAB_CONTENT = "INPUT_POST_TAB_CONTENT";
    protected static final String OUTPUT_POST_TAB_CONTENT = "OUTPUT_POST_TAB_CONTENT";
    protected static final String NEXT_OUTPUT_POST_TAB_CONTENT = "NEXT_OUTPUT_POST_TAB_CONTENT";
    protected static final String SINK_RESULT_TAB_CONTENT = "SINK_RESULT_TAB_CONTENT";
    protected static final String ITEM_DIAGNOSTIC_TAB_CONTENT = "ITEM_DIAGNOSTIC_TAB_CONTENT";

    interface ItemsListUiBinder extends UiBinder<HTMLPanel, ItemsListView> {
    }

    private static ItemsListUiBinder ourUiBinder = GWT.create(ItemsListUiBinder.class);

    @UiField
    CellTable itemsTable;
    @UiField
    DecoratedTabPanel detailedTabs;
    @UiField
    ItemDiagnosticTabContent itemDiagnosticTabContent;

    public ItemsListView() {
        initWidget(ourUiBinder.createAndBindUi(this));
        setupColumns(itemDiagnosticTabContent);
    }

    @SuppressWarnings("unchecked")
    private void setupColumns(final ItemDiagnosticTabContent itemDiagnosticTabContent) {
        itemDiagnosticTabContent.itemDiagnosticTable.addColumn(constructDiagnosticLevelColumn());
        itemDiagnosticTabContent.itemDiagnosticTable.addColumn(constructDiagnosticMessageColumn());
        itemDiagnosticTabContent.stacktraceTable.addColumn(constructDiagnosticStacktraceColumn());
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
        return new TextColumn<DiagnosticModel>() {
            @Override
            public String getValue(DiagnosticModel model) {
                return model.getStacktrace();
            }
        };
    }
}
