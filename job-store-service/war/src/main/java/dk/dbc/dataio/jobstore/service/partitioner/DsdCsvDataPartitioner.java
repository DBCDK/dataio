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

import dk.dbc.dataio.jobstore.types.InvalidEncodingException;
import org.jsoup.Jsoup;

import javax.xml.stream.events.Characters;
import java.io.InputStream;

/**
 * Data partitioner for "Den Store Danske" encyclopedia CSV format
 */
public class DsdCsvDataPartitioner extends CsvDataPartitioner {
    /**
     * Creates new instance of DataPartitioner for CSV data
     * @param inputStream stream from which csv records can be read
     * @param encodingName encoding specified in job specification
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if given empty valued encoding argument
     * @throws InvalidEncodingException if encoding can not be deduced from given encoding name
     * @return new instance of DsdCsvDataPartitioner
     */
    public static DsdCsvDataPartitioner newInstance(InputStream inputStream, String encodingName)
            throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
        return new DsdCsvDataPartitioner(inputStream, encodingName);
    }

    private DsdCsvDataPartitioner(InputStream inputStream, String encodingName)
            throws NullPointerException, IllegalArgumentException, InvalidEncodingException {
        super(inputStream, encodingName);
    }

    @Override
    Characters newCharactersEvent(String value) {
        return xmlEventFactory.createCharacters(Jsoup.parse(value).text());
    }
}
