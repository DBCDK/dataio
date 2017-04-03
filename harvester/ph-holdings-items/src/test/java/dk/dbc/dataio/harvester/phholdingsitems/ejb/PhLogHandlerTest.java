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

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class PhLogHandlerTest {

    @Test
    public void test_recordIsDeleted_true() {
        Map<String, Integer> statusMap = new HashMap<>();
        statusMap.put("Decommissioned", 21);
        statusMap.put("OnShelf", 0);
        statusMap.put("OnLoan", 0);
        assertThat(PhLogHandler.recordIsDeleted(statusMap), is(true));
    }

    @Test
    public void test_recordIsDeleted_false() {
        Map<String, Integer> statusMap = new HashMap<>();
        statusMap.put("Decommissioned", 21);
        statusMap.put("OnShelf", 12);
        statusMap.put("OnLoan", 3);
        assertThat(PhLogHandler.recordIsDeleted(statusMap), is(false));
    }

    @Test
    public void test_recordIsDeleted_noDecommissioned() {
        Map<String, Integer> statusMap = new HashMap<>();
        statusMap.put("OnShelf", 23);
        assertThat(PhLogHandler.recordIsDeleted(statusMap), is(false));
    }
}
