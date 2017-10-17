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

package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.JobModel;
import dk.dbc.dataio.gui.client.proxies.JobRerunProxy;
import dk.dbc.dataio.gui.server.jobrerun.JobRerunScheme;

import javax.naming.NamingException;
import javax.servlet.ServletException;

public class JobRerunProxyServlet extends RemoteServiceServlet implements JobRerunProxy {
    private static final long serialVersionUID = 350209395900092220L;

    private transient JobRerunProxy jobRerunProxy = null;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            jobRerunProxy = new JobRerunProxyImpl();
        } catch (NamingException e){
            throw new ServletException(e);
        }
    }

    @Override
    public JobRerunScheme parse(JobModel jobModel) throws ProxyException {
        return jobRerunProxy.parse(jobModel);
    }

    @Override
    public void close() {
        if (jobRerunProxy != null) {
            jobRerunProxy.close();
            jobRerunProxy = null;
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        close();
    }
}
