package dk.dbc.dataio.gui.client.pages.flowbindersshow;

import com.google.gwt.user.client.ui.IsWidget;
import dk.dbc.dataio.gui.client.views.GenericView;
import dk.dbc.dataio.gui.types.FlowBinderContentViewData;
import java.util.List;

public interface FlowBindersShowView extends IsWidget, GenericView<FlowBindersShowPresenter> {
    void setFlowBinders(List<FlowBinderContentViewData> flowBinders);
}
