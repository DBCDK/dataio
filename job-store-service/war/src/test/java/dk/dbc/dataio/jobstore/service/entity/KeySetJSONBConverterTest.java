package dk.dbc.dataio.jobstore.service.entity;

import dk.dbc.dataio.jobstore.service.entity.DependencyTrackingEntity.Key;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.Test;
import org.postgresql.util.PGobject;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by ja7 on 09-04-16.
 *
 * Test for JSON converter.
 */
public class KeySetJSONBConverterTest {
    @Test
    public void encodeSmallList() throws Exception {

        KeySetJSONBConverter converter=new KeySetJSONBConverter();
        Set<Key> input= new HashSet<>();

        input.add(new Key(1,2) );
        input.add(new Key(3,4) );

        PGobject pgObject=converter.convertToDatabaseColumn( input);

        Set<Key> res=converter.convertToEntityAttribute(pgObject);

        assertThat(res, containsInAnyOrder( new Key( 3,4 ), new Key(1,2)));
    }




}
