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

import org.junit.Test;

import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class FileStoreUrnTest {
    private final String fileId = "42";

    @Test(expected = NullPointerException.class)
    public void constructor_urnStringArgIsNull_throws() throws URISyntaxException {
        new FileStoreUrn(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_urnStringArgIsEmpty_throws() throws URISyntaxException {
        new FileStoreUrn("");
    }

    @Test(expected = URISyntaxException.class)
    public void constructor_urnStringArgIsInvalidUri_throws() throws URISyntaxException {
        new FileStoreUrn("1:2:3");
    }

    @Test(expected = URISyntaxException.class)
    public void constructor_urnStringArgHasInvalidScheme_throws() throws URISyntaxException {
        new FileStoreUrn("uri:type:42");
    }

    @Test(expected = URISyntaxException.class)
    public void constructor_urnStringArgHasInvalidType_throws() throws URISyntaxException {
        new FileStoreUrn(String.format("%s:type:42", FileStoreUrn.SCHEME));
    }

    @Test(expected = URISyntaxException.class)
    public void constructor_urnStringArgHasNullFileId_throws() throws URISyntaxException {
        new FileStoreUrn(String.format("%s:%s", FileStoreUrn.SCHEME, FileStoreUrn.TYPE));
    }

    @Test(expected = URISyntaxException.class)
    public void constructor_urnStringArgHasEmptyFileId_throws() throws URISyntaxException {
        new FileStoreUrn(String.format("%s:%s: ", FileStoreUrn.SCHEME, FileStoreUrn.TYPE));
    }

    @Test
    public void constructor_urnStringArgIsValid_returnsNewInstance() throws URISyntaxException {
        final FileStoreUrn fileStoreUrn = new FileStoreUrn(String.format("%s:%s:%s", FileStoreUrn.SCHEME, FileStoreUrn.TYPE, fileId));
        assertThat(fileStoreUrn, is(notNullValue()));
        assertThat(fileStoreUrn.getFileId(), is(fileId));
    }

    @Test(expected = NullPointerException.class)
    public void create_fileIdArgIsNull_throws() {
        FileStoreUrn.create(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_fileIdArgIsEmpty_throws() {
        FileStoreUrn.create("");
    }

    @Test
    public void create_fileIdArgIsValid_returnsNewInstance() {
        final FileStoreUrn fileStoreUrn = FileStoreUrn.create(fileId);
        assertThat(fileStoreUrn, is(notNullValue()));
        assertThat(fileStoreUrn.getFileId(), is(fileId));
    }
}
