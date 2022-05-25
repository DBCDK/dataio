package dk.dbc.dataio.gui.client.pages.job.purge;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import dk.dbc.dataio.gui.client.views.ContentPanel;

public class View extends ContentPanel<Presenter> implements IsWidget {

    interface JobPurgeBinder extends UiBinder<HTMLPanel, View> {
    }

    private static JobPurgeBinder uiBinder = GWT.create(JobPurgeBinder.class);

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);

    @UiField
    Label header;
    @UiField
    HTML text;

    public View() {
        super("");
        add(uiBinder.createAndBindUi(this));
    }

    public void init() {
        text.setHTML(viewInjector.getTexts().text_JobPurge().replaceAll("\\n", "<br>"));
    }

}
