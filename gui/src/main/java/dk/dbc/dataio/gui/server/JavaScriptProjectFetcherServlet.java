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
import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherError;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcher;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class JavaScriptProjectFetcherServlet extends RemoteServiceServlet implements JavaScriptProjectFetcher {
    private static final long serialVersionUID = 6080138003266070829L;
    private transient JavaScriptProjectFetcher javaScriptProjectFetcher;

    @Override
    public void init() throws ServletException {
        super.init();
        final String subversionScmEndpoint = ServletUtil.getSubversionScmEndpoint();
        javaScriptProjectFetcher = new JavaScriptProjectFetcherImpl(subversionScmEndpoint);
    }

    @Override
    public List<RevisionInfo> fetchRevisions(String projectName) throws JavaScriptProjectFetcherException {
        List<RevisionInfo> hackedRevisionList = deployHackForRetrievingASingleTestedRevision(); // To be removed when hack is removed
        if (!hackedRevisionList.isEmpty()) { // To be removed when hack is removed
            return hackedRevisionList; // To be removed when hack is removed
        } // To be removed when hack is removed
        return javaScriptProjectFetcher.fetchRevisions(projectName);
    }

    @Override
    public List<String> fetchJavaScriptFileNames(String projectName, long revision) throws JavaScriptProjectFetcherException {
        return javaScriptProjectFetcher.fetchJavaScriptFileNames(projectName, revision);
    }

    @Override
    public List<String> fetchJavaScriptInvocationMethods(String projectName, long revision, String javaScriptFileName) throws JavaScriptProjectFetcherException {
        return javaScriptProjectFetcher.fetchJavaScriptInvocationMethods(projectName, revision, javaScriptFileName);
    }

    @Override
    public fetchRequiredJavaScriptResult fetchRequiredJavaScript(String projectName, long revision, String javaScriptFileName, String javaScriptFunction) throws JavaScriptProjectFetcherException {
        return javaScriptProjectFetcher.fetchRequiredJavaScript(projectName, revision, javaScriptFileName, javaScriptFunction);
    }

    // Remove this method and the obsolete lines in fetchRevisions when hack is removed.
    @SuppressWarnings("PMD.EmptyCatchBlock")
    private List<RevisionInfo> deployHackForRetrievingASingleTestedRevision() throws JavaScriptProjectFetcherException {
        List<RevisionInfo> hackedRevisionList = new ArrayList<RevisionInfo>();
        try {
            String revision = ServiceUtil.getStringValueFromResource("dataioGuiSubversionScmRevision");
            hackedRevisionList.add(new RevisionInfo(Long.parseLong(revision), "", new Date(), "", Collections.EMPTY_LIST));
        } catch(NumberFormatException e) {
            // throw in case the retrivied revision can not be parsed as a long
            throw new JavaScriptProjectFetcherException(JavaScriptProjectFetcherError.UNKNOWN, e);
        } catch(NamingException e) {
            // Ignoring this will let this method return an empty list.
        }
        return hackedRevisionList;
    }
}
