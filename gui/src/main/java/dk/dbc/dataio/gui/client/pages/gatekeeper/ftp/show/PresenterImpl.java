package dk.dbc.dataio.gui.client.pages.gatekeeper.ftp.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.FilteredAsyncCallback;
import dk.dbc.dataio.gui.client.exceptions.ProxyErrorTranslator;
import dk.dbc.dataio.gui.client.model.FtpFileModel;
import dk.dbc.dataio.gui.client.util.CommonGinjector;

import java.util.List;
import java.util.logging.Logger;

public class PresenterImpl extends AbstractActivity implements Presenter {
    private final Logger logger = Logger.getLogger(PresenterImpl.class.getName());
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);
    @SuppressWarnings("unused")
    private PlaceController placeController;
    private String header;

    /**
     * Default constructor
     *
     * @param placeController The Placecontroller
     * @param header          breadcrumb Header text
     */
    public PresenterImpl(PlaceController placeController, String header) {
        logger.info("Presenter:"+header);
        this.placeController = placeController;
        this.header = header;
    }

    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        getView().setHeader(this.header);
        getView().setPresenter(this);
        containerWidget.setWidget(getView().asWidget());
        getFtpOverview();
    }

    @Override
    public void getFtpOverview() {
        commonInjector.getFtpProxyAsync().ftpFiles(new GetFtpListAsyncCallback());
    }

    class GetFtpListAsyncCallback extends FilteredAsyncCallback<List<FtpFileModel>> {

        @Override
        public void onFilteredFailure(Throwable caught) {
            getView().setErrorText(ProxyErrorTranslator.toClientErrorFromFtpProxy(caught, commonInjector.getProxyErrorTexts(), null));
        }

        @Override
        public void onSuccess(List<FtpFileModel> ftpFileModels) {
            getView().setFtpShowTable(ftpFileModels);

        }
    }

    private View getView() {
        return viewInjector.getView();
    }
}
