package dk.dbc.dataio.gui.client.gquery;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.query.client.Function;
import com.google.gwt.query.client.GQuery;
import com.google.gwt.query.client.LazyGQuery;
import com.google.gwt.query.client.Predicate;
import com.google.gwt.query.client.Promise;
import com.google.gwt.query.client.Properties;
import com.google.gwt.query.client.impl.SelectorEngine;
import com.google.gwt.query.client.plugins.Plugin;
import com.google.gwt.query.client.plugins.ajax.Ajax;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Widget;

import java.util.List;

/**
 * This is a wrapper for the GwtQuery library
 * The purpose is to make our usage of GwtQuery testable
 * GwtQuery is a static class, which makes it very difficult to unit test,
 * By wrapping it into a non-static class (this class), we can inject it into the
 * places, where it is being used. Thereby we can supply a mocked version in unit test.
 * <p>
 * All static methods in the GwtQuery implementation are wrapped in this class
 * <p>
 * Note:
 * The static methods returns the original GQuery object, and NOT the
 * GQuery wrapper object.
 * In other words: Only the static methods, that are implemented in this wrapper class
 * can be injected, and thereby mocked in unit tests. When chaining calls, the GQuery
 * object is called, and we have lost the ability to mock these calls.
 */
public class GQueryWrapper {

    public GQuery $() {
        return GQuery.$();
    }

    public GQuery $(Element element) {
        return GQuery.$(element);
    }

    public GQuery $(Event event) {
        return GQuery.$(event);
    }

    public GQuery $(Function f) {
        return GQuery.$(f);
    }

    public GQuery $(JavaScriptObject jso) {
        return GQuery.$(jso);
    }

    public GQuery $(Object o) {
        return GQuery.$(o);
    }

    public GQuery $(List<?> nodesOrWidgets) {
        return GQuery.$(nodesOrWidgets);
    }

    public GQuery $(Node n) {
        return GQuery.$(n);
    }

    public GQuery $(NodeList<Element> elms) {
        return GQuery.$(elms);
    }

    public GQuery $(String selectorOrHtml) {
        return GQuery.$(selectorOrHtml);
    }

    public <T extends GQuery> T $(String selector, Class<T> plugin) {
        return GQuery.$(selector, plugin);
    }

    public GQuery $(String selectorOrHtml, Node ctx) {
        return GQuery.$(selectorOrHtml, ctx);
    }

    public <T extends GQuery> T $(String selector, Node context, Class<T> plugin) {
        return GQuery.$(selector, context, plugin);
    }

    public GQuery $(String selectorOrHtml, Widget context) {
        return GQuery.$(selectorOrHtml, context);
    }

    public <T extends GQuery> T $(String selector, Widget context, Class<T> plugin) {
        return GQuery.$(selector, context, plugin);
    }

    public GQuery $(Widget... widgets) {
        return GQuery.$(widgets);
    }

    public GQuery $(Node... nodes) {
        return GQuery.$(nodes);
    }

    public Properties $$() {
        return GQuery.$$();
    }

    public Properties $$(String properties) {
        return GQuery.$$(properties);
    }

    public Promise ajax(Properties p) {
        return GQuery.ajax(p);
    }

    public Promise ajax(Ajax.Settings settings) {
        return GQuery.ajax(settings);
    }

    public Promise ajax(String url, Ajax.Settings settings) {
        return GQuery.ajax(url, settings);
    }

    public boolean contains(Element a, Element b) {
        return GQuery.contains(a, b);
    }

    public <T> T data(Element e, String key) {
        return GQuery.data(e, key);
    }

    public <T> T data(Element element, String key, T value) {
        return GQuery.data(element, key, value);
    }

    public void each(JsArrayMixed objects, Function f) {
        GQuery.each(objects, f);
    }

    public <T> void each(List<T> objects, Function f) {
        GQuery.each(objects, f);
    }

    public <T> void each(T[] objects, Function f) {
        GQuery.each(objects, f);
    }

    public Promise get(String url, Properties data, Function onSuccess) {
        return GQuery.get(url, data, onSuccess);
    }

    public Promise getJSON(String url, Properties data, Function onSuccess) {
        return GQuery.getJSON(url, data, onSuccess);
    }

    public Promise getJSONP(String url, Properties data, Function onSuccess) {
        return GQuery.getJSONP(url, data, onSuccess);
    }

    public <T> T[] grep(T[] objects, Predicate f) {
        return GQuery.grep(objects, f);
    }

    public LazyGQuery<?> lazy() {
        return GQuery.lazy();
    }

    public Promise post(String url, Properties data, Function onSuccess) {
        return GQuery.post(url, data, onSuccess);
    }

    public <T extends GQuery> Class<T> registerPlugin(Class<T> plugin, Plugin<T> pluginFactory) {
        return GQuery.registerPlugin(plugin, pluginFactory);
    }

    public Promise when(Object... subordinates) {
        return GQuery.when(subordinates);
    }

    public Promise.Deferred Deferred() {
        return GQuery.Deferred();
    }

    public SelectorEngine getSelectorEngine() {
        return GQuery.getSelectorEngine();
    }

}
