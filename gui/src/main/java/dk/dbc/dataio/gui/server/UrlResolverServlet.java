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

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class UrlResolverServlet extends HttpServlet {

    private static final long serialVersionUID = -6885510844881237998L;

    private static final Logger log = LoggerFactory.getLogger(UrlResolverServlet.class);
    private final JSONBContext jsonbContext = new JSONBContext();

    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            response.getWriter().print(jsonbContext.marshall(getUrls()));
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (JSONBException | IOException e) {
            log.info("getUrls() failed with exception" + e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private Map<String, String> getUrls() {
        Map<String, String> urls = new HashMap<>();
        urls.put(JndiConstants.URL_RESOURCE_JOBSTORE_RS, resolve(ServiceUtil::getJobStoreServiceEndpoint, JndiConstants.URL_RESOURCE_JOBSTORE_RS));
        urls.put(JndiConstants.FLOW_STORE_SERVICE_ENDPOINT_RESOURCE, resolve(ServiceUtil::getFlowStoreServiceEndpoint, JndiConstants.FLOW_STORE_SERVICE_ENDPOINT_RESOURCE));
        urls.put(JndiConstants.SUBVERSION_SCM_ENDPOINT_RESOURCE, resolve(ServiceUtil::getSubversionScmEndpoint, JndiConstants.SUBVERSION_SCM_ENDPOINT_RESOURCE));
        urls.put(JndiConstants.URL_RESOURCE_FILESTORE_RS, resolve(ServiceUtil::getFileStoreServiceEndpoint, JndiConstants.URL_RESOURCE_FILESTORE_RS));
        urls.put(JndiConstants.URL_RESOURCE_LOGSTORE_RS, resolve(ServiceUtil::getLogStoreServiceEndpoint, JndiConstants.URL_RESOURCE_LOGSTORE_RS));
        urls.put(JndiConstants.URL_RESOURCE_FBS_WS, resolve(ServiceUtil::getFbsEndpoint, JndiConstants.URL_RESOURCE_FBS_WS));
        urls.put(JndiConstants.URL_RESOURCE_GUI_FTP, resolve(ServiceUtil::getGuiFtpEndpoint, JndiConstants.URL_RESOURCE_GUI_FTP));
        urls.put(JndiConstants.URL_RESOURCE_OPEN_AGENCY, resolve(ServiceUtil::getOpenAgencyEndpoint, JndiConstants.URL_RESOURCE_OPEN_AGENCY));
        urls.put(JndiConstants.URL_RESOURCE_ELK, resolve(ServiceUtil::getElkEndpoint, JndiConstants.URL_RESOURCE_ELK));
        return urls;
    }

    @FunctionalInterface
    interface JndiNameResolver<T> {
        T get() throws NamingException;
    }

    private String resolve(JndiNameResolver<String> resolver, String jndiName) {
        String value = null;
        try {
            value = resolver.get();
        } catch (NamingException e) {
            log.info("{} not found", jndiName);
        }
        return value;
    }
}
