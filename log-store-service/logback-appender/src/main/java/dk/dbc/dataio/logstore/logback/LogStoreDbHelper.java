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

package dk.dbc.dataio.logstore.logback;

public class LogStoreDbHelper {
    public static final int TIMESTAMP = 1;
    public static final int FORMATTED_MESSAGE = 2;
    public static final int LOGGER_NAME = 3;
    public static final int LEVEL_STRING = 4;
    public static final int THREAD_NAME = 5;
    public static final int CALLER_FILENAME = 6;
    public static final int CALLER_CLASS = 7;
    public static final int CALLER_METHOD = 8;
    public static final int CALLER_LINE = 9;
    public static final int STACK_TRACE = 10;
    public static final int MDC = 11;
    public static final int JOB_ID = 12;
    public static final int CHUNK_ID = 13;
    public static final int ITEM_ID = 14;

    private LogStoreDbHelper() {}

    public static String getInsertSQL() {
        return "INSERT INTO LOGENTRY(TIMESTAMP, FORMATTED_MESSAGE, LOGGER_NAME, LEVEL_STRING, THREAD_NAME, CALLER_FILENAME, CALLER_CLASS, CALLER_METHOD, CALLER_LINE, STACK_TRACE, MDC, JOB_ID, CHUNK_ID, ITEM_ID)" +
                " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    }
}
