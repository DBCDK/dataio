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

/**
 * Simple class used to coordinate VM shutdown sequence between two threads
 */
public class ShutdownManager {
    private boolean shutdownInProgress = false;
    private boolean readyToExit = true;

    /**
     * Sets internal shutdown-in-progress state to true
     */
    public synchronized void signalShutdownInProgress() {
        shutdownInProgress = true;
    }

    /**
     * Sets internal ready-to-exit state to false unless
     * shutdown-in-progress is already true
     * @return true if ready-to-exit state was changed, otherwise false
     */
    public synchronized boolean signalBusy() {
        if (!shutdownInProgress) {
            readyToExit = false;
        }
        return !shutdownInProgress;
    }

    /**
     * Sets internal ready-to-exit state to true
     */
    public synchronized void signalReadyToExit() {
        readyToExit = true;
    }

    /**
     * @return true if internal shutdown-in-progress state is true, otherwise false
     */
    public synchronized boolean isShutdownInProgress() {
        return shutdownInProgress;
    }

    /**
     * @return true if internal ready-to-exit state is true, otherwise false
     */
    public synchronized boolean isReadyToExit() {
        return readyToExit;
    }
}
