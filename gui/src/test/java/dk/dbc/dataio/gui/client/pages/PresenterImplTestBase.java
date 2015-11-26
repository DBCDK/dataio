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
package dk.dbc.dataio.gui.client.pages;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.util.CommonGinjector;
import org.mockito.Mock;

/**
 * Created by ThomasBerg on 10/11/15.
 */
public abstract class PresenterImplTestBase {
    @Mock protected FlowStoreProxyAsync mockedFlowStore;
    @Mock protected JobStoreProxyAsync mockedJobStore;
    @Mock protected PlaceController mockedPlaceController;
    @Mock protected AcceptsOneWidget mockedContainerWidget;
    @Mock protected EventBus mockedEventBus;
    @Mock protected ProxyErrorTexts mockedProxyErrorTexts;
    @Mock protected dk.dbc.dataio.gui.client.pages.navigation.Texts mockedMenuTexts;
    @Mock protected CommonGinjector mockedCommonGinjector;
    @Mock protected Exception mockedException;
    @Mock protected ProxyException mockedProxyException;

    protected final String header = "Header Text";
}
