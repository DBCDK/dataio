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

package dk.dbc.dataio.gui.client.pages.ftp.show;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.util.CommonGinjector;


/**
 * This class represents the show ftp's presenter implementation
 */
public class PresenterImpl extends AbstractActivity implements Presenter {
    ViewGinjector viewInjector = GWT.create(ViewGinjector.class);
    CommonGinjector commonInjector = GWT.create(CommonGinjector.class);


    /**
     * Default constructor
     */
    public PresenterImpl() {
        commonInjector.getUrlResolverProxyAsync().getUrl("FTP_URL",
                new AsyncCallback<String>() {
                    @Override
                    public void onFailure(Throwable throwable) {
                        viewInjector.getView().setErrorText(viewInjector.getTexts().error_JndiFetchError());
                    }
                    @Override
                    public void onSuccess(String jndiUrl) {
                        viewInjector.getView().ftpFrame.setUrl(jndiUrl);
                    }
                });
    }


    /**
     * start method
     * Is called by PlaceManager, whenever the Place is being invoked
     * This method is the start signal for the presenter
     *
     * @param containerWidget the widget to use
     * @param eventBus        the eventBus to use
     */
    @Override
    public void start(AcceptsOneWidget containerWidget, EventBus eventBus) {
        View view = viewInjector.getView();
        view.setPresenter(this);
        containerWidget.setWidget(view.asWidget());
    }

}
