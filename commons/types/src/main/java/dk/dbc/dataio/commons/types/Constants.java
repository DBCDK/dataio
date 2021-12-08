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

package dk.dbc.dataio.commons.types;

public class Constants {
    public static final short CHUNK_MAX_SIZE = 10;

    public static final String SINK_ID_ENV_VARIABLE = "SINK_ID";
    public static final String PROCESSOR_SHARD_ENV_VARIABLE = "PROCESSOR_SHARD";
    public static final String MISSING_FIELD_VALUE = "__MISSING__";
    public static final String CALL_OPEN_AGENCY = "__CALL_OPEN_AGENCY__";
    public static final String UPDATE_VALIDATE_ONLY_FLAG = "UPDATE_VALIDATE_ONLY_FLAG";
    public static final String JOBTYPE_PERSISTENT = "PERSISTENT";
    public static final String JOBTYPE_TRANSIENT = "TRANSIENT";

    // I'm introducing the invariant that submitter number 1 indicates missing value.
    public static final long MISSING_SUBMITTER_VALUE            = 1;
    public static final long PERSISTENCE_ID_LOWER_BOUND         = 1L;
    public static final long PERSISTENCE_VERSION_LOWER_BOUND    = 1L;
    public static final long CHUNK_ITEM_ID_LOWER_BOUND          = -1L;
}
