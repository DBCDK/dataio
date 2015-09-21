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

package dk.dbc.dataio.perftest;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class Dataset {
    private ArrayList<DatasetValue> values;

    public Dataset() {
        this.values = new ArrayList<>();
    }

    public ArrayList<DatasetValue> getValues() {
        return values;
    }

    public void addValue(DatasetValue value) {
        values.add(value);
    }

    public static Dataset fromJsonFile(String file) throws IOException {
        final Dataset dataset;
        final File datasetFile = new File(file);
        if (datasetFile.exists()) {
            final ObjectMapper mapper = new ObjectMapper();
            dataset = mapper.readValue(datasetFile, Dataset.class);
        } else {
            dataset = new Dataset();
        }
        return dataset;
    }

    public static void toJsonFile(String file, Dataset dataset) throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(new File(file), dataset);
    }

    public static class DatasetValue {
        public Long timestamp;
        public Number highContentTiming;
        public Number lowContentTiming;
    }
}
