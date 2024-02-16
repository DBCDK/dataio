package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.distributed.tools.KeySetJSONBConverter;
import dk.dbc.dataio.jobstore.service.dependencytracking.TrackingKey;
import org.junit.jupiter.api.Test;
import org.postgresql.util.PGobject;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * Created by ja7 on 09-04-16.
 * <p>
 * Test for JSON converter.
 */
public class KeySetJSONBConverterTest {
    @Test
    public void encodeSmallList() throws Exception {

        KeySetJSONBConverter converter = new KeySetJSONBConverter();
        Set<TrackingKey> input = new HashSet<>();

        input.add(new TrackingKey(1, 2));
        input.add(new TrackingKey(3, 4));

        PGobject pgObject = converter.convertToDatabaseColumn(input);

        Set<TrackingKey> res = converter.convertToEntityAttribute(pgObject);

        assertThat(res, containsInAnyOrder(new TrackingKey(3, 4), new TrackingKey(1, 2)));
    }


}
