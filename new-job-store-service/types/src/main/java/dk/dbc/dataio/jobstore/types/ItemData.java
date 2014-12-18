package dk.dbc.dataio.jobstore.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.nio.charset.Charset;

public class ItemData {

    private final String data;
    private final Charset encoding;

    /**
     * Class constructor
     * @param data saved
     * @param encoding used on data
     * @throws NullPointerException if given null-valued argument
     * @throws IllegalArgumentException if data is an empty String
     */
    @JsonCreator
    public ItemData (@JsonProperty("data") String data,
                     @JsonProperty("encoding") Charset encoding) throws NullPointerException, IllegalArgumentException {

        this.data = InvariantUtil.checkNotNullNotEmptyOrThrow(data, "data");
        this.encoding = InvariantUtil.checkNotNullOrThrow(encoding, "encoding");
    }

    public String getData() {
        return data;
    }

    public Charset getEncoding() {
        return encoding;
    }
}
