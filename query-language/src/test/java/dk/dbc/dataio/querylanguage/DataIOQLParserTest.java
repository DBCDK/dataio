/*
 * DataIO - Data IO
 *
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

package dk.dbc.dataio.querylanguage;

import org.junit.Test;

public class DataIOQLParserTest {
    private final DataIOQLParser ioqlParser = new DataIOQLParser();

    @Test
    public void equalsOperator() throws ParseException {
        ioqlParser.parse("job:id = 42");
    }

    @Test
    public void greaterThanOperator() throws ParseException {
        ioqlParser.parse("job:id > 42");
    }

    @Test
    public void greaterThanOrEqualToOperator() throws ParseException {
        ioqlParser.parse("job:id >= 42");
    }

    @Test
    public void lessThanOperator() throws ParseException {
        ioqlParser.parse("job:id < 42");
    }

    @Test
    public void lessThanOrEqualToOperator() throws ParseException {
        ioqlParser.parse("job:id <= 42");
    }

    @Test
    public void notEqualsOperator() throws ParseException {
        ioqlParser.parse("job:id != 42");
    }

    @Test
    public void quotedValue() throws ParseException {
        ioqlParser.parse("job:timeofcreation > \"2017-09-06\"");
    }

    @Test
    public void multipleTerms() throws ParseException {
        ioqlParser.parse("job:id = 42 OR job:id = 43 AND job:timeofcreation > \"2017-09-06\"");
    }
    
    @Test
    public void logicalGroupings() throws ParseException {
        ioqlParser.parse("job:id = 42 OR (job:id = 43 AND job:timeofcreation > \"2017-09-06\")");
    }
}
