package dk.dbc.dataio.gui.client.pages.failedftps.show;


import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import dk.dbc.dataio.jobstore.types.InvalidTransfileNotificationContext;
import dk.dbc.dataio.jobstore.types.Notification;

import java.util.List;

/**
 * Concrete Presenter Implementation Class for Failed Ftps
 */
public class PresenterImpl extends AbstractActivity implements Presenter {

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    JobStoreProxyAsync jobStoreProxy = commonInjector.getJobStoreProxyAsync();

    protected PlaceController placeController;


    public PresenterImpl(PlaceController placeController) {
        this.placeController = placeController;
    }

    /**
     * start method
     * Is called by PlaceManager, whenever the PlaceCreate or PlaceEdit are being invoked
     * This method is the start signal for the presenter
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        containerWidget.setWidget(getView().asWidget());
        getView().setHeader(commonInjector.getMenuTexts().menu_FailedFtps());
        getView().setPresenter(this);
        initializeData();
    }


    /*
     * Public methods
     */

    /**
     * Shows a Popup window with an editable content of the transfile and a copy of the mail, sent to the user
     *
     * @param notification The notification, containing amongst other info - the transfile and the mail
     */
    @Override
    public void showTransFileContent(Notification notification) {
        InvalidTransfileNotificationContext context = (InvalidTransfileNotificationContext) notification.getContext();
        getView().showFailedFtp(context.getTransfileName(), context.getTransfileContent(), notification.getContent());
    }

    /**
     * Resends the transfile, with the transfile given as a parameter in the call to the method
     *
     * @param transFileContent The transfile content
     */
    @Override
    public void resendFtp(String transFileName, String transFileContent) {
        commonInjector.getFtpProxyAsync().put(transFileName, transFileContent, new PutFtpCallback());
    }


    /*
     * Local methods
     */

    protected View getView() {
        return viewInjector.getView();
    }

    protected Texts getTexts() {
        return viewInjector.getTexts();
    }

    private void initializeData() {
        jobStoreProxy.listInvalidTransfileNotifications(new ListInvalidTransfileNotificationsCallback());
    }


    /*
     * Local classes
     */

    /**
     * Callback class for fetching all failed transfile deliveries
     */
    class ListInvalidTransfileNotificationsCallback implements AsyncCallback<List<Notification>> {
        @Override
        public void onFailure(Throwable throwable) {
            getView().displayWarning(getTexts().error_CannotFetchNotifications());
        }

        @Override
        public void onSuccess(List<Notification> notifications) {
            getView().setNotifications(notifications);
        }
    }

    /**
     * Callback class for putting a transfile via ftp proxy
     */
    private class PutFtpCallback implements AsyncCallback<Void> {
        @Override
        public void onFailure(Throwable caught) {
            viewInjector.getView().setErrorText(viewInjector.getTexts().error_CannotMakeFtpRequest());
        }

        @Override
        public void onSuccess(Void result) {
            // Success - no alert box is shown here
        }
    }

}


