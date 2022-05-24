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

package dk.dbc.dataio.gatekeeper;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class ShutdownManagerTest {
    @Test
    public void constructor_setsInitialState() {
        final ShutdownManager shutdownManager = new ShutdownManager();
        assertThat("isShutdownInProgress", shutdownManager.isShutdownInProgress(), is(false));
        assertThat("isReadyToExit", shutdownManager.isReadyToExit(), is(true));
    }

    @Test
    public void signalShutdownInProgress_setsState() {
        final ShutdownManager shutdownManager = new ShutdownManager();
        shutdownManager.signalShutdownInProgress();
        assertThat("isShutdownInProgress", shutdownManager.isShutdownInProgress(), is(true));
        assertThat("isReadyToExit", shutdownManager.isReadyToExit(), is(true));
    }

    @Test
    public void signalBusy_setsStateAndReturnsTrue() {
        final ShutdownManager shutdownManager = new ShutdownManager();
        assertThat("signalBusy()", shutdownManager.signalBusy(), is(true));
        assertThat("isShutdownInProgress", shutdownManager.isShutdownInProgress(), is(false));
        assertThat("isReadyToExit", shutdownManager.isReadyToExit(), is(false));
    }

    @Test
    public void signalBusy_shutdownInProgress_returnsFalse() {
        final ShutdownManager shutdownManager = new ShutdownManager();
        shutdownManager.signalShutdownInProgress();
        assertThat("signalBusy()", shutdownManager.signalBusy(), is(false));
        assertThat("isShutdownInProgress", shutdownManager.isShutdownInProgress(), is(true));
        assertThat("isReadyToExit", shutdownManager.isReadyToExit(), is(true));
    }

    @Test
    public void signalReadyToExit_setsState() {
        final ShutdownManager shutdownManager = new ShutdownManager();
        shutdownManager.signalBusy();
        shutdownManager.signalReadyToExit();
        assertThat("isShutdownInProgress", shutdownManager.isShutdownInProgress(), is(false));
        assertThat("isReadyToExit", shutdownManager.isReadyToExit(), is(true));
    }
}
