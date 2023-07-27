package dk.dbc.dataio.gui.client.pages.job.show;

import com.google.gwt.user.client.rpc.AsyncCallback;

import java.util.function.Consumer;

public class ACallback<T> implements AsyncCallback<T> {
    private Consumer<T> onSuccess = null;
    private Consumer<Throwable> onFailure = null;

    public ACallback() {
    }

    public ACallback(Consumer<T> onSuccess, Consumer<Throwable> onFailure) {
        this.onSuccess = onSuccess;
        this.onFailure = onFailure;
    }

    @Override
    public void onFailure(Throwable throwable) {
        if (onFailure != null) onFailure.accept(throwable);
    }

    @Override
    public void onSuccess(T t) {
        if (onSuccess != null) onSuccess.accept(t);
    }
}
