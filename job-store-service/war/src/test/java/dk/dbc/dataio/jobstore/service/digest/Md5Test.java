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

package dk.dbc.dataio.jobstore.service.digest;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class Md5Test {
    @Test(expected=NullPointerException.class)
    public void getMd5HexDigest_dataArgIsNull_throws() {
        Md5.asHex(null);
    }

    @Test
    public void getMd5HexDigest_dataArgIsEmpty_returnsDigestAsHexString() {
        // Expected value procured doing %> echo -n "" | md5sum
        final String expectedHex = "d41d8cd98f00b204e9800998ecf8427e";
        String hex = Md5.asHex(new byte[]{});
        assertThat(hex, is(expectedHex));
    }

    @Test
    public void getMd5HexDigest_dataArgIsBasedOnPureAscii_returnsDigestAsHexString() {
        // Expected value procured doing %> echo -n "test data" | md5sum
        final String expectedHex = "eb733a00c0c9d336e65691a37ab54293";
        String hex = Md5.asHex("test data".getBytes(StandardCharsets.UTF_8));
        assertThat(hex, is(expectedHex));
    }

    @Test
    public void getMd5HexDigest_dataArgIsBasedOnUtf8Data_returnsDigestAsHexString() throws Exception {
        // Expected value procured doing %> echo -n "test æøå" | md5sum
        final String expectedHex = "e7a5a6d6f045c0b0dde36d1adf89e60a";
        String hex = Md5.asHex("test æøå".getBytes(StandardCharsets.UTF_8));
        assertThat(hex, is(expectedHex));
    }
}