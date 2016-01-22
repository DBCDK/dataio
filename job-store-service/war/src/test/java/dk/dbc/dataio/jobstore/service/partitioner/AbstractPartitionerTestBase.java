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

package dk.dbc.dataio.jobstore.service.partitioner;

import dk.dbc.dataio.commons.utils.lang.StringUtil;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class AbstractPartitionerTestBase {

    private static final String DATACONTAINERS_WITH_TRACKING_ID_XML = "/datacontainers-with-tracking-id.xml";
    private static final InputStream EMPTY_INPUT_STREAM = StringUtil.asInputStream("");
    private static final Charset UTF_8 = StandardCharsets.UTF_8;


    protected static String getDataContainerXmlWithMarcExchangeAndTrackingIds() {
        return readTestRecordAsString(DATACONTAINERS_WITH_TRACKING_ID_XML);
    }

    protected static InputStream getEmptyInputStream() {
        return EMPTY_INPUT_STREAM;
    }

    protected static String getUft8Encoding() {
        return UTF_8.name();
    }

    protected InputStream asInputStream(String xml) {
        return asInputStream(xml, UTF_8);
    }

    protected InputStream asInputStream(String xml, Charset encoding) {
        return new ByteArrayInputStream(xml.getBytes(encoding));
    }

    private static String readTestRecordAsString(String resourceName) {
        return StringUtil.asString(readTestRecord(resourceName), UTF_8);
    }

    private static byte[] readTestRecord(String resourceName) {
        try {
            final URL url = AbstractPartitionerTestBase.class.getResource(resourceName);
            final Path resPath;
            resPath = Paths.get(url.toURI());
            return Files.readAllBytes(resPath);
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
