/*
 * DataIO - Data IO
 *
 * Copyright (C) 2018 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
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

package dk.dbc.dataio.sink.marcconv;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.marc.DanMarc2Charset;
import dk.dbc.marc.Marc8Charset;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Optional;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversionParam {
    @JsonProperty
    private String encoding;

    @JsonProperty
    private Integer submitter;

    @JsonIgnore
    public Optional<Charset> getEncoding() throws ConversionException {
        if (encoding == null) {
            return Optional.empty();
        }
        return Optional.of(ConversionParam.charsetForName(encoding));
    }

    public ConversionParam withEncoding(String encoding) {
        this.encoding = encoding;
        return this;
    }

    @JsonIgnore
    public Optional<Integer> getSubmitter() {
        return Optional.ofNullable(submitter);
    }

    public ConversionParam withSubmitter(Integer submitter) {
        this.submitter = submitter;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ConversionParam param = (ConversionParam) o;

        if (encoding != null ? !encoding.equals(param.encoding) : param.encoding != null) {
            return false;
        }
        return submitter != null ? submitter.equals(param.submitter) : param.submitter == null;
    }

    @Override
    public int hashCode() {
        int result = encoding != null ? encoding.hashCode() : 0;
        result = 31 * result + (submitter != null ? submitter.hashCode() : 0);
        return result;
    }

    private static Charset charsetForName(String name) {
        String trimmed = name.toLowerCase().trim();
        try {
            switch (trimmed) {
                case "danmarc2":
                case "latin1":
                    return new DanMarc2Charset();
                case "marc8":
                    return new Marc8Charset();
                default:
                    return Charset.forName(trimmed);
            }
        } catch (UnsupportedCharsetException e) {
            throw new ConversionException("Unknown encoding", e);
        }
    }
}
