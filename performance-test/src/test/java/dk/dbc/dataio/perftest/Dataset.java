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
