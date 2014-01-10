package dk.dbc.dataio.gui.client.views;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.gui.client.presenters.SubmittersShowPresenter;
import java.util.List;

public interface SubmittersShowView extends IsWidget, View<SubmittersShowPresenter> {
    void setSubmitters(List<Submitter> submitter);
}
