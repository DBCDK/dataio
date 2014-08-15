package dk.dbc.dataio.gui.client.pages.submittersshow;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.gui.client.views.GenericView;

import java.util.List;

public interface SubmittersShowGenericView extends IsWidget, GenericView<SubmittersShowGenericPresenter> {
    void setSubmitters(List<Submitter> submitter);
}
