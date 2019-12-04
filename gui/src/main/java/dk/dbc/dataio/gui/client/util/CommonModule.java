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
package dk.dbc.dataio.gui.client.util;

import com.google.gwt.inject.client.AbstractGinModule;
import com.google.inject.Singleton;
import dk.dbc.dataio.gui.client.exceptions.texts.ProxyErrorTexts;
import dk.dbc.dataio.gui.client.pages.navigation.Texts;
import dk.dbc.dataio.gui.client.proxies.ConfigProxyAsync;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.FtpProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcherAsync;
import dk.dbc.dataio.gui.client.proxies.PeriodicJobsHarvesterProxyAsync;
import dk.dbc.dataio.gui.client.proxies.UrlResolverProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JobRerunProxyAsync;
import dk.dbc.dataio.gui.client.proxies.JobStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxyAsync;
import dk.dbc.dataio.gui.client.proxies.TickleHarvesterProxyAsync;
import dk.dbc.dataio.gui.client.resources.Resources;

public class CommonModule extends AbstractGinModule {

    @Override
    protected void configure() {
        bind(Texts.class).in(Singleton.class);
        bind(ProxyErrorTexts.class).in(Singleton.class);
        bind(FlowStoreProxyAsync.class).in(Singleton.class);
        bind(PeriodicJobsHarvesterProxyAsync.class).in(Singleton.class);
        bind(JavaScriptProjectFetcherAsync.class).in(Singleton.class);
        bind(JobStoreProxyAsync.class).in(Singleton.class);
        bind(LogStoreProxyAsync.class).in(Singleton.class);
        bind(UrlResolverProxyAsync.class).in(Singleton.class);
        bind(FtpProxyAsync.class).in(Singleton.class);
        bind(TickleHarvesterProxyAsync.class).in(Singleton.class);
        bind(JobRerunProxyAsync.class).in(Singleton.class);
        bind(ConfigProxyAsync.class).in(Singleton.class);
        bind(Resources.class).in(Singleton.class);
    }
}
