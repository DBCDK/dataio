package dk.dbc.dataio.commons.utils.lang;

import java.util.function.Supplier;

public class Require {
    public static <R, T extends RuntimeException> R nonNull(R object, Supplier<T> supplier) throws T {
        if(object == null) throw supplier.get();
        return object;
    }
}
