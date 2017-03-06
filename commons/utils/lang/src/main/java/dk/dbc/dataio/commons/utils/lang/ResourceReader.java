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

import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/*
 * This class handles reading of resources.
 */
public class ResourceReader {
    public static InputStream getResourceAsStream(Class aClass, String resourceName) {
        return aClass.getClassLoader().getResourceAsStream(resourceName);
    }

    public static byte[] getResourceAsByteArray(Class aClass, String resourceName) {
        final byte[] byteBuffer = new byte[8192];
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (InputStream in = aClass.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new IllegalStateException("The resource '" + resourceName + "' could not be found on the classpath");
            }
            int bytesRead = in.read(byteBuffer);
            while (bytesRead != -1) {
                out.write(byteBuffer, 0, bytesRead);
                bytesRead = in.read(byteBuffer);
            }
        } catch (IOException e) {
            throw new IllegalStateException("The resource '" + resourceName + "' could not be read", e);
        }
        return out.toByteArray();
    }

    public static String getResourceAsString(Class aClass, String resourceName) {
        return StringUtil.asString(getResourceAsByteArray(aClass, resourceName));
    }

    public static String getResourceAsBase64(Class aClass, String resourceName) {
        return Base64.encodeBase64String(getResourceAsByteArray(aClass, resourceName));
    }
}
