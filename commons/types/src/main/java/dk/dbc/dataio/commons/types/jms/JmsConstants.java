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

package dk.dbc.dataio.commons.types.jms;

/**
 * Constant values used be the dataIO JMS messaging protocol.
 */
public class JmsConstants {
    public static final String PAYLOAD_PROPERTY_NAME             = "payload";
    public static final String RESOURCE_PROPERTY_NAME            = "resource";
    public static final String SINK_ID_PROPERTY_NAME             = "id";
    public static final String SINK_VERSION_PROPERTY_NAME        = "version";
    public static final String FLOW_ID_PROPERTY_NAME             = "flowId";
    public static final String FLOW_VERSION_PROPERTY_NAME        = "flowVersion";
    public static final String FLOW_BINDER_ID_PROPERTY_NAME      = "flowBinderId";
    public static final String FLOW_BINDER_VERSION_PROPERTY_NAME = "flowBinderVersion";
    public static final String PROCESSOR_SHARD_PROPERTY_NAME     = "shard";

    public static final String CHUNK_PAYLOAD_TYPE         = "Chunk";
    public static final String JOB_STORE_SOURCE_VALUE     = "jobstore";

    public static final String ADDITIONAL_ARGS            = "additionalArgs";

    private JmsConstants() { }
}
