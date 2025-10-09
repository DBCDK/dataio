package dk.dbc.dataio.harvester.utils.datafileverifier;

import java.util.function.Consumer;

public class BeanBuilder<T> {
    private T t;

    public BeanBuilder(T t) {
        this.t = t;
    }

    public BeanBuilder<T> set(Consumer<T> c) {
        c.accept(t);
        return this;
    }

    public T build() {
        return t;
    }
}
