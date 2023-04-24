package dk.dbc.dataio.gui.client.pages.flowbinder.status;
import dk.dbc.dataio.gui.client.presenters.GenericPresenter;

public interface Presenter extends GenericPresenter {
    void showFlowBinder(long id);
    void getFlowBindersUsage();
    void getFlowBindersUsageCached();
}
