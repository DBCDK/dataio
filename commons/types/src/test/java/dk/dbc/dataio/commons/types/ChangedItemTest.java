package dk.dbc.dataio.commons.types;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * ChangedItem unit tests
 * <p>
 * The test methods of this class uses the following naming convention:
 * <p>
 * unitOfWork_stateUnderTest_expectedBehavior
 */

public class ChangedItemTest {
    private final String path = "path";
    private final String type = "type";

    @Test
    public void constructor_pathArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new RevisionInfo.ChangedItem(null, type));
    }

    @Test
    public void constructor_pathArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new RevisionInfo.ChangedItem("", type));
    }

    @Test
    public void constructor_typeArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new RevisionInfo.ChangedItem(path, null));
    }

    @Test
    public void constructor_typeArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new RevisionInfo.ChangedItem(path, ""));
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        RevisionInfo.ChangedItem instance = new RevisionInfo.ChangedItem(path, type);
        assertThat(instance, is(notNullValue()));
    }
}
