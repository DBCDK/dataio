package dk.dbc.dataio.engine;

import java.util.Collections;
import java.util.List;

public class ModelUtil {
    private ModelUtil() { }

    public static <T> List<T> asUnmodifiableList(List<T> list) {
        if (list != null) {
            return Collections.unmodifiableList(list);
        }
        return null;
    }
}
