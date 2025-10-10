package dk.dbc.dataio.harvester.utils.datafileverifier;

import java.util.function.Consumer;

public class BeanBuilder<T> {
    protected T bean;

    public BeanBuilder(T bean) {
        this.bean = bean;
    }

    public BeanBuilder<T> set(Consumer<T> c) {
        c.accept(bean);
        return this;
    }

    public T build() {
        return bean;
    }
}
