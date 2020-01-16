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

package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.Constants;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;

@Singleton
public class HealthBean {
    private boolean terminallyIll = false;
    private Throwable cause;
    private String shardId;

    @PostConstruct
    public void initialize() {
        shardId = System.getenv().get(Constants.PROCESSOR_SHARD_ENV_VARIABLE);
    }

    @Lock(LockType.READ)
    public boolean isTerminallyIll() {
        return terminallyIll;
    }

    @Lock(LockType.READ)
    public String getShardId() {
        return shardId;
    }

    @Lock(LockType.READ)
    public Throwable getCause() {
        return cause;
    }

    @Lock(LockType.WRITE)
    public void signalTerminallyIll() {
        signalTerminallyIll(null);
    }

    @Lock(LockType.WRITE)
    public void signalTerminallyIll(Throwable cause) {
        this.cause = cause;
        terminallyIll = true;
    }
}
