package dk.dbc.dataio.gui.client.pages.submitter.show;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.gui.client.views.GenericView;

import java.util.List;

public interface SubmittersShowView extends IsWidget, GenericView<SubmittersShowPresenter> {
    void clearFields();
    void setSubmitters(List<Submitter> submitter);
}
