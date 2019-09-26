/*
 * DataIO - Data IO
 * Copyright (C) 2017 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.harvester.phholdingsitems.ejb;

import dk.dbc.phlog.PhLog;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PhLogHandlerIT extends PhHarvesterIntegrationTest {

    @Test
    public void test_updatePhLogEntry() {
        PhLogHandler phLogHandler = getPhLogHandler();
        Map<String, Integer> statusMap = new HashMap<>();
        statusMap.put("Decommissioned", 21);
        statusMap.put("OnShelf", 2);
        String bibliographicRecordId = "6284764";
        // check that state remains the same when handling the same input
        for(int i = 0; i < 2; i++) {
            updatePhLogEntry(phLogHandler, PHAGENCYID, bibliographicRecordId,
                    statusMap);
            assertThat("count when adding an entry",
                getCount(phLogEntityManager), is(1L));
            assertThat("add entry with active holdings", runSqlCmdSingleResult(
                phLogEntityManager, "select deleted from entry"), is(false));
        }
        // Check that empty statusMap leads to decomissioned:1
        statusMap.clear();
        updatePhLogEntry(phLogHandler, PHAGENCYID, bibliographicRecordId,
                statusMap);
        assertThat("Status map contains decommisioned:1", statusMap.get("Decommissioned"),
                is(1));
        // check that deleted changes to true
        statusMap.put("OnShelf", 0);
        updatePhLogEntry(phLogHandler, PHAGENCYID, bibliographicRecordId,
            statusMap);
        assertThat("count when adding an already existing entry", getCount(
            phLogEntityManager), is(1L));
        assertThat("delete status has changed", runSqlCmdSingleResult(
            phLogEntityManager, "select deleted from entry"), is(true));
    }

    private void updatePhLogEntry(PhLogHandler phLogHandler, int agencyId,
            String bibliographicRecordId, Map<String, Integer> statusMap) {
        phLogEntityManager.getTransaction().begin();
        phLogHandler.updatePhLogEntry(agencyId, bibliographicRecordId,
                statusMap);
        phLogEntityManager.getTransaction().commit();
    }

    private PhLogHandler getPhLogHandler() {
        PhLogHandler phLogHandler = new PhLogHandler();
        phLogHandler.phLog = new PhLog(phLogEntityManager);
        return phLogHandler;
    }
}
