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

package dk.dbc.dataio.filestore.service.entity;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class FileAttributesTest {
    private final Date creationTime = new Date();
    private final Path path = Paths.get("path/to/file");

    @Test(expected = NullPointerException.class)
    public void constructor_creationTimeArgIsNull_throws() {
        new FileAttributes(null, path);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_pathArgIsNull_throws() {
        new FileAttributes(creationTime, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_pathArgIsEmpty_throws() {
        new FileAttributes(creationTime, Paths.get(""));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final FileAttributes fileAttributes = new FileAttributes(creationTime, path);
        assertThat(fileAttributes, is(notNullValue()));
        assertThat(fileAttributes.getCreationTime(), is(creationTime));
        assertThat(fileAttributes.getLocation(), is(path));
        assertThat(fileAttributes.getByteSize(), is(0L));
    }

    @Test
    public void constructor_creationTime_isDefensivelyCopied() {
        Date dateToBeModified = new Date();
        final FileAttributes fileAttributes = new FileAttributes(dateToBeModified, path);
        dateToBeModified.setTime(42);
        assertThat(fileAttributes.getCreationTime(), is(not(dateToBeModified)));
    }

    @Test
    public void getCreationTime_returnValue_isDefensivelyCopied() {
        final FileAttributes fileAttributes = new FileAttributes(creationTime, path);
        final Date creationTimeToBeModified = fileAttributes.getCreationTime();
        creationTimeToBeModified.setTime(42);
        assertThat(fileAttributes.getCreationTime(), is(not(creationTimeToBeModified)));
        assertThat(fileAttributes.getCreationTime(), is(creationTime));
    }

    @Test public void setBytesRead_bytesReadIsSet_bytesReadIsReturnedWithUpdatedValue() {
        final FileAttributes fileAttributes = new FileAttributes(creationTime, path);
        assertThat(fileAttributes.getByteSize(), is(0L));
        fileAttributes.setByteSize(42);
        assertThat(fileAttributes.getByteSize(), is(42L));
    }
}
