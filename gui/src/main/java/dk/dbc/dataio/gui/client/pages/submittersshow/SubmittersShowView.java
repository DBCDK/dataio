package dk.dbc.dataio.gui.client.pages.submittersshow;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.gui.client.views.View;
import java.util.List;

public interface SubmittersShowView extends IsWidget, View<SubmittersShowPresenter> {
    void setSubmitters(List<Submitter> submitter);
}
