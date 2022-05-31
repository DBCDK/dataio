package dk.dbc.dataio.gui.client.components;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This class waits for the arrival of multiple objects
 */
public class WaitContainer {
    private Consumer<Map<String, Object>> callback = null;
    private Map<String, Object> waitingList = new HashMap<>();


    /**
     * Constructor taking the callback lambda and the list of Object identifiers, each identifying an object to observe
     *
     * @param callback The callback lambda to be called, when all objects are ready
     * @param keys     A list of strings, identifying a list of Objects to observe
     */
    public WaitContainer(Consumer<Map<String, Object>> callback, String... keys) {
        this.callback = callback;
        if (keys != null) {
            for (String key : keys) {
                waitingList.put(key, null);
            }
        }
    }

    /**
     * This method is called whenever an object is delivered and ready<br>
     * When all objects are collected, the callback lambda delivered in the constructor is activated<br>
     * Please note, that multiple calls to put() on the same variable is possible - still, whenever all variables are present, this results in an activation of the lambda method
     *
     * @param key    A string, identifying the observed object
     * @param object The object, that is delivered to the Await class
     */
    public void put(String key, Object object) {
        if (waitingList.containsKey(key)) {
            waitingList.put(key, object);
        }
        if (areAllReady() && callback != null) {
            callback.accept(waitingList);
        }
    }

    /**
     * This method checks, whether all objects are collected
     *
     * @return True if all objects are collected, and ready, false if not
     */
    private boolean areAllReady() {
        for (String key : waitingList.keySet()) {
            if (waitingList.get(key) == null) {
                return false;
            }
        }
        return true;
    }

}
