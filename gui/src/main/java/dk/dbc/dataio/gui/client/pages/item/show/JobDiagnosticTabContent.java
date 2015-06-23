package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;

public class JobDiagnosticTabContent extends Composite {
    interface JobDiagnosticTabContentUiBinder extends UiBinder<HTMLPanel, JobDiagnosticTabContent> {
    }

    private static JobDiagnosticTabContentUiBinder ourUiBinder = GWT.create(JobDiagnosticTabContentUiBinder.class);

    public JobDiagnosticTabContent() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    protected Texts texts;

    // UI Fields
    @UiField CellTable jobDiagnosticTable;

    /**
     * Ui Handler to catch click events on the Back button
     * @param event Clicked event
     */
    @UiHandler("backButton")
    void backButtonPressed(ClickEvent event) {
        History.back();
    }
}