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

package dk.dbc.dataio.jobstore.types;

import org.junit.Test;

import java.nio.charset.Charset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class ItemDataTest {

    @Test(expected = NullPointerException.class)
    public void constructor_dataArgIsNull_throws() {
        new ItemData(null, Charset.forName("UTF-8"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_emptyData_throws() {
        new ItemData("", Charset.forName("UTF-8"));
    }

    @Test(expected = NullPointerException.class)
    public void constructor_encodingArgIdNull_throws() {
        new ItemData("test", null);
    }

    @Test
    public void setItemData_inputIsValid_ItemDataCreated() {
        final String DATA = "this is some test data";
        ItemData itemData = new ItemData(DATA, Charset.forName("UTF-8"));
        assertThat("itemData.data", itemData.getData(), is(DATA));
        assertThat("itemData.encoding", itemData.getEncoding(), is(Charset.forName("UTF-8")));
    }
}
