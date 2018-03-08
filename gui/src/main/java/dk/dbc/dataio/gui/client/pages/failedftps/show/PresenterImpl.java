/*
 * DataIO - Data IO
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gui.client.pages.failedftps.show;


import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
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
     * @param containerWidget the widget to use
     * @param eventBus the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        containerWidget.setWidget(getView().asWidget());
        getView().setHeader(commonInjector.getMenuTexts().menu_FailedFtps());
        getView().setPresenter(this);
        initializeData();
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

}


