package dk.dbc.dataio.gui.client.pages.item.show;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.TextArea;

public class WorkflowNoteTabContent extends Composite {
    interface WorkflowNoteTabContentUiBinder extends UiBinder<HTMLPanel, WorkflowNoteTabContent> {
    }

    private static WorkflowNoteTabContentUiBinder ourUiBinder = GWT.create(WorkflowNoteTabContentUiBinder.class);

    public WorkflowNoteTabContent() {
        initWidget(ourUiBinder.createAndBindUi(this));
    }

    @UiField
    TextArea note;
}
