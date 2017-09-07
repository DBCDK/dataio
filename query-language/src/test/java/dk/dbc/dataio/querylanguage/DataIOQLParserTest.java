/*
 * DataIO - Data IO
 *
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

package dk.dbc.dataio.querylanguage;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class DataIOQLParserTest {
    private final DataIOQLParser ioqlParser = new DataIOQLParser();

    @Test
    public void equalsOperator() throws ParseException {
        final String query = ioqlParser.parse("job:id = 42");
        assertThat(query, is("SELECT * FROM job WHERE id = 42"));
    }

    @Test
    public void greaterThanOperator() throws ParseException {
        final String query = ioqlParser.parse("job:id > 42");
        assertThat(query, is("SELECT * FROM job WHERE id > 42"));
    }

    @Test
    public void greaterThanOrEqualToOperator() throws ParseException {
        final String query = ioqlParser.parse("job:id >= 42");
        assertThat(query, is("SELECT * FROM job WHERE id >= 42"));
    }

    @Test
    public void lessThanOperator() throws ParseException {
        final String query = ioqlParser.parse("job:id < 42");
        assertThat(query, is("SELECT * FROM job WHERE id < 42"));
    }

    @Test
    public void lessThanOrEqualToOperator() throws ParseException {
        final String query = ioqlParser.parse("job:id <= 42");
        assertThat(query, is("SELECT * FROM job WHERE id <= 42"));
    }

    @Test
    public void notEqualsOperator() throws ParseException {
        final String query = ioqlParser.parse("job:id != 42");
        assertThat(query, is("SELECT * FROM job WHERE id != 42"));
    }

    @Test
    public void quotedValue() throws ParseException {
        final String query = ioqlParser.parse("job:timeofcreation > \"2017-09-06\"");
        assertThat(query, is("SELECT * FROM job WHERE timeofcreation > '2017-09-06'"));
    }

    @Test
    public void multipleTerms() throws ParseException {
        final String query = ioqlParser.parse("job:id = 42 OR job:id = 43 AND job:timeofcreation > \"2017-09-06\"");
        assertThat(query, is("SELECT * FROM job WHERE id = 42 OR id = 43 AND timeofcreation > '2017-09-06'"));
    }
    
    @Test
    public void logicalGroupings() throws ParseException {
        final String query = ioqlParser.parse("job:id = 42 OR (job:id = 43 AND job:timeofcreation > \"2017-09-06\")");
        assertThat(query, is("SELECT * FROM job WHERE id = 42 OR (id = 43 AND timeofcreation > '2017-09-06')"));
    }

    @Test
    public void characterEscaping() throws ParseException {
        final String query = ioqlParser.parse("chapter:quote = \"\\\"The difference between stupidity and genius is that genius has its limits\\\"\"");
        assertThat(query, is("SELECT * FROM chapter WHERE quote = '\"The difference between stupidity and genius is that genius has its limits\"'"));
    }
}
