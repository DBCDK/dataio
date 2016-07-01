/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.gui.client.pages.iotraffic;


import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.util.CommonGinjector;

import java.util.List;

/**
 * Concrete Presenter Implementation Class for Io Traffic
 */
public class PresenterImpl extends AbstractActivity implements Presenter {

    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);

    FlowStoreProxyAsync flowStoreProxy = commonInjector.getFlowStoreProxyAsync();

    protected String header;

    String submitter = "";
    String packaging = "";
    String format = "";
    String destination = "";
    Boolean copy = false;
    Boolean notify = false;


    public PresenterImpl(String header) {
        this.header = header;
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
        getView().setHeader(this.header);
        getView().setPresenter(this);
        initializeData();
    }

    /*
     * Indications from the View
     */
    @Override
    public void submitterChanged(String submitter) {
        this.submitter = submitter;
    }


    @Override
    public void packagingChanged(String packaging) {
        this.packaging = packaging;
    }

    @Override
    public void formatChanged(String format) {
        this.format = format;
    }

    @Override
    public void destinationChanged(String destination) {
        this.destination = destination;
    }

    @Override
    public void copyChanged(Boolean copy) {
        this.copy = copy;
        if (copy) {
            getView().notify.setEnabled(true);
        } else {
            this.notify = false;
            getView().notify.setEnabled(false);
            getView().notify.setValue(false);
        }
    }

    @Override
    public void notifyChanged(Boolean notify) {
        this.notify = notify;
    }

    @Override
    public void addButtonPressed() {
        if (submitter.isEmpty() || packaging.isEmpty() || format.isEmpty() || destination.isEmpty()) {
            getView().displayWarning(getTexts().error_InputFieldValidationError());
        } else {
            flowStoreProxy.createGatekeeperDestination(
                    new GatekeeperDestination(0L, submitter, destination, packaging, format, copy, notify),
                    new CreateGatekeeperDestinationCallback()
            );
        }
    }

    @Override
    public void deleteButtonPressed(long gatekeeperId) {
        flowStoreProxy.deleteGatekeeperDestination(gatekeeperId, new DeleteGatekeeperDestinationCallback());
    }

    @Override
    public void updateGatekeeperDestination(long id, String submitterNumber, String destination, String packaging, String format, boolean copyToPosthus, boolean notifyFromPosthus) {
        flowStoreProxy.updateGatekeeperDestination(
                new GatekeeperDestination(id, submitterNumber, destination, packaging, format, copyToPosthus, notifyFromPosthus),
                new UpdateGatekeeperDestinationCallback()
        );
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
        submitter = "";
        getView().submitter.clearText();
        packaging = "";
        getView().packaging.clearText();
        format = "";
        getView().format.clearText();
        destination = "";
        getView().destination.clearText();
        copy = false;
        getView().copy.setValue(false);
        notify = false;
        getView().notify.setValue(false);
        getView().notify.setEnabled(false);
        flowStoreProxy.findAllGatekeeperDestinations(new FindAllGateKeeperDestinationsCallback());
    }


    /*
     * Local classes
     */
    class CreateGatekeeperDestinationCallback implements AsyncCallback<GatekeeperDestination> {
        @Override
        public void onFailure(Throwable throwable) {
            getView().displayWarning(getTexts().error_CannotCreateGatekeeperDestination());
        }
        @Override
        public void onSuccess(GatekeeperDestination destination) {
            initializeData();
        }
    }

    class UpdateGatekeeperDestinationCallback implements AsyncCallback<GatekeeperDestination> {
        @Override
        public void onFailure(Throwable throwable) {
            getView().displayWarning(getTexts().error_CannotUpdateGatekeeperDestination());
        }
        @Override
        public void onSuccess(GatekeeperDestination destination) {
            initializeData();
        }
    }

    class FindAllGateKeeperDestinationsCallback implements AsyncCallback<List<GatekeeperDestination>> {
        @Override
        public void onFailure(Throwable throwable) {
            getView().displayWarning(getTexts().error_CannotFetchGatekeeperDestinations());
        }
        @Override
        public void onSuccess(List<GatekeeperDestination> gatekeeperDestinations) {
            getView().setGatekeepers(gatekeeperDestinations);
        }
    }

    class DeleteGatekeeperDestinationCallback implements AsyncCallback<Void> {
        @Override
        public void onFailure(Throwable caught) {
            getView().displayWarning(getTexts().error_CannotDeleteGatekeeperDestination());
        }
        @Override
        public void onSuccess(Void result) {
            initializeData();
        }
    }

}


