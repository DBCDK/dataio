package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

public class JobDiagnosticTabContent extends Composite {
    interface JobDiagnosticTabContentUiBinder extends UiBinder<HTMLPanel, JobDiagnosticTabContent> {
    }

    private static JobDiagnosticTabContentUiBinder ourUiBinder = GWT.create(JobDiagnosticTabContentUiBinder.class);

    public JobDiagnosticTabContent() {
        initWidget(ourUiBinder.createAndBindUi(this));
        jobDiagnosticTable.getElement().getStyle().setWhiteSpace(Style.WhiteSpace.PRE);
    }

    protected Texts texts;

    // UI Fields
    @UiField
    CellTable jobDiagnosticTable;
}
