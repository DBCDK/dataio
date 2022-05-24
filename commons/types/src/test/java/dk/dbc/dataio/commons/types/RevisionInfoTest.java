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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * RevisionInfo unit tests
 *
 * The test methods of this class uses the following naming convention:
 *
 *  unitOfWork_stateUnderTest_expectedBehavior
 */
public class RevisionInfoTest {
    private final long revision = 42L;
    private final String author = "author";
    private final String message = "message";
    private final Date date = new Date();
    private final List<RevisionInfo.ChangedItem> changedItems = new ArrayList<>(0);

    @Test(expected = NullPointerException.class)
    public void constructor_authorArgIsNull_throws() {
        new RevisionInfo(revision, null, date, message, changedItems);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_dateArgIsNull_throws() {
        new RevisionInfo(revision, author, null, message, changedItems);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_messageArgIsNull_throws() {
        new RevisionInfo(revision, author, date, null, changedItems);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_changedItemsArgIsNull_throws() {
        new RevisionInfo(revision, author, date, message, null);
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final RevisionInfo instance = new RevisionInfo(revision, author, date, message, changedItems);
        assertThat(instance, is(notNullValue()));
    }

    @Test
    public void verify_defensiveCopyingOfChangedItemList() {
        final List<RevisionInfo.ChangedItem> changedItems = new ArrayList<>();
        changedItems.add(null);
        final RevisionInfo instance = new RevisionInfo(revision, author, date, message, changedItems);
        assertThat(instance.getChangedItems().size(), is(1));
        changedItems.add(null);
        final List<RevisionInfo.ChangedItem> returnedItems = instance.getChangedItems();
        assertThat(returnedItems.size(), is(1));
        returnedItems.add(null);
        assertThat(instance.getChangedItems().size(), is(1));
    }

    @Test
    public void verify_defensiveCopyingOfDate() {
        final Date datestamp = new Date();
        final long expectedTime = datestamp.getTime();
        final RevisionInfo instance = new RevisionInfo(revision, author, datestamp, message, changedItems);
        datestamp.setTime(42);
        final Date returnedDate = instance.getDate();
        assertThat(returnedDate.getTime(), is(expectedTime));
        returnedDate.setTime(42);
        assertThat(instance.getDate().getTime(), is(expectedTime));
    }
}
