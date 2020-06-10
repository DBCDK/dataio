/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.periodicjobs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.conversion.ConversionException;
import dk.dbc.dataio.commons.conversion.ConversionParam;

import java.util.Optional;

public class PeriodicJobsConversionParam extends ConversionParam {
    @JsonProperty
    private String sortkey;

    @JsonProperty
    private String recordHeader;

    @JsonIgnore
    public Optional<String> getSortkey() throws ConversionException {
        return Optional.ofNullable(sortkey);
    }

    public PeriodicJobsConversionParam withSortkey(String sortkey) {
        this.sortkey = sortkey;
        return this;
    }

    @JsonIgnore
    public Optional<String> getRecordHeader() throws ConversionException {
        return Optional.ofNullable(recordHeader);
    }

    public PeriodicJobsConversionParam withRecordHeader(String recordHeader) {
        this.recordHeader = recordHeader;
        return this;
    }
}
