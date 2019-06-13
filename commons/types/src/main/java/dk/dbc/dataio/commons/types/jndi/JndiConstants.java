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

package dk.dbc.dataio.commons.types.jndi;

public class JndiConstants {
    public static final String FLOW_STORE_SERVICE_ENDPOINT_RESOURCE = "dataioGuiFlowStoreServiceEndpoint";
    public static final String SUBVERSION_SCM_ENDPOINT_RESOURCE = "dataioGuiSubversionScmEndpoint";

    public static final String JDBC_RESOURCE_JOBSTORE = "jdbc/dataio/jobstore";
    public static final String JDBC_RESOURCE_LOGSTORE = "jdbc/dataio/logstore";
    public static final String JDBC_RESOURCE_FILESTORE = "jdbc/dataio/fileStore";
    public static final String JDBC_RESOURCE_ES_INFLIGHT = "jdbc/dataio/sinks/esInFlight";
    public static final String JDBC_RESOURCE_SINK_DIFF = "jdbc/dataio/diff";

    public static final String URL_RESOURCE_FBS_WS = "url/dataio/fbs/ws";
    public static final String URL_RESOURCE_FILESTORE_RS = "url/dataio/filestore/rs";
    public static final String URL_RESOURCE_LOGSTORE_RS = "url/dataio/logstore/rs";
    public static final String URL_RESOURCE_JOBSTORE_RS = "url/dataio/jobstore/rs";
    public static final String URL_RESOURCE_HARVESTER_RR_RS = "url/dataio/harvester/rr/rs";
    public static final String URL_RESOURCE_HARVESTER_TICKLE_RS = "url/dataio/harvester/tickle/rs";

    public static final String URL_RESOURCE_GUI_FTP = "url/dataio/gui/ftp";
    public static final String URL_RESOURCE_OPEN_AGENCY = "url/dataio/openagency";
    public static final String URL_RESOURCE_ELK = "url/dataio/elk";

    public static final String MAIL_RESOURCE_JOBSTORE_NOTIFICATIONS = "mail/dataio/jobstore/notifications";

    private JndiConstants() { }
}
