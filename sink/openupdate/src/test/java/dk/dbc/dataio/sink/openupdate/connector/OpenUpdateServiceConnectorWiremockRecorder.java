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

package dk.dbc.dataio.sink.openupdate.connector;

public class OpenUpdateServiceConnectorWiremockRecorder {
    /*
        Steps to reproduce wiremock recording:

        * Start standalone runner
            java -jar wiremock-standalone.jar --proxy-all="http://fbstest:20080" --record-mappings --verbose

        * Run the main method of this class

        * Replace content of src/test/resources/{__files|mappings} with that produced by the standalone runner
     */

    public static void main(String[] args) {
        final OpenUpdateServiceConnectorIT openUpdateServiceConnectorIT = new OpenUpdateServiceConnectorIT();
        openUpdateServiceConnectorIT.openUpdateServiceConnector = new OpenUpdateServiceConnector("http://localhost:8080/UpdateService/2.0");
        openUpdateServiceConnectorIT.recordUpdateRecordRequests();
    }
}
