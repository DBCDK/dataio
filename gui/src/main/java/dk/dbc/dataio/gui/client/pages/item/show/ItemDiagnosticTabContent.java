package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

public class ItemDiagnosticTabContent extends Composite {
    interface ItemDiagnosticTabContentUiBinder extends UiBinder<HTMLPanel, ItemDiagnosticTabContent> {
    }

    private static ItemDiagnosticTabContentUiBinder ourUiBinder = GWT.create(ItemDiagnosticTabContentUiBinder.class);

    public ItemDiagnosticTabContent() {
        initWidget(ourUiBinder.createAndBindUi(this));
        itemDiagnosticTable.getElement().getStyle().setWhiteSpace(Style.WhiteSpace.PRE);
        stacktraceTable.getElement().getStyle().setWhiteSpace(Style.WhiteSpace.PRE);
    }

    // UI Fields
    @UiField
    CellTable itemDiagnosticTable;
    @UiField
    CellTable stacktraceTable;
}
