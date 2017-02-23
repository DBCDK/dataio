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

package dk.dbc.dataio.commons.utils.lang;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/*
 * This class handles reading of resources.
 */
public class ResourceReader {
    public static InputStream getResourceAsStream(Class _class, String resourceName) {
        return _class.getResourceAsStream(resourceName);
    }

    public static byte[] getResourceAsByteArray(Class _class, String resourceName) {
        return readTestRecord(_class, resourceName);
    }

    public static String readTestRecordAsString(Class _class, String resourceName) {
        return StringUtil.asString(readTestRecord(_class, resourceName), StandardCharsets.UTF_8);
    }

    public static byte[] readTestRecord(Class _class, String resourceName) {
        try {
            final URL url = _class.getResource(resourceName);
            final Path resPath;
            resPath = Paths.get(url.toURI());
            return Files.readAllBytes(resPath);
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
