package dk.dbc.dataio.commons.utils.test.rest;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MockedResponse<T> extends Response {
    private final int mockedStatus;
    private final T mockedEntity;
    private final T mockedGenericType;
    private final MultivaluedMap<String, Object> headers;

    public MockedResponse(int status, T entity) {
        mockedStatus = status;
        mockedEntity = entity;
        mockedGenericType = entity;
        headers = new MultivaluedHashMap<>();
    }

    @Override
    public int getStatus() {
        return mockedStatus;
    }

    @Override
    public StatusType getStatusInfo() {
        return null;
    }

    @Override
    public Object getEntity() {
        return null;
    }

    @Override
    public <T> T readEntity(Class<T> tClass) {
        return (T) mockedEntity;
    }

    @Override
    public <T> T readEntity(GenericType<T> tGenericType) {
        return (T) mockedGenericType;
    }

    @Override
    public <T> T readEntity(Class<T> tClass, Annotation[] annotations) {
        return null;
    }

    @Override
    public <T> T readEntity(GenericType<T> tGenericType, Annotation[] annotations) {
        return null;
    }

    @Override
    public boolean hasEntity() {
        return mockedEntity != null;
    }

    @Override
    public boolean bufferEntity() {
        return false;
    }

    @Override
    public void close() {
    }

    @Override
    public MediaType getMediaType() {
        return null;
    }

    @Override
    public Locale getLanguage() {
        return null;
    }

    @Override
    public int getLength() {
        return 0;
    }

    @Override
    public Set<String> getAllowedMethods() {
        return null;
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return null;
    }

    @Override
    public EntityTag getEntityTag() {
        return null;
    }

    @Override
    public Date getDate() {
        return null;
    }

    @Override
    public Date getLastModified() {
        return null;
    }

    @Override
    public URI getLocation() {
        return URI.create("123");
    }

    @Override
    public Set<Link> getLinks() {
        return null;
    }

    @Override
    public boolean hasLink(String s) {
        return false;
    }

    @Override
    public Link getLink(String s) {
        return null;
    }

    @Override
    public Link.Builder getLinkBuilder(String s) {
        return null;
    }

    @Override
    public MultivaluedMap<String, Object> getMetadata() {
        return null;
    }

    @Override
    public MultivaluedMap<String, String> getStringHeaders() {
        return null;
    }

    @Override
    public String getHeaderString(String s) {
        return null;
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return headers;
    }

    public MockedResponse addHeaderValue(String name, Object value) {
        if (!headers.containsKey(name)) {
            headers.put(name, new ArrayList<>());
        }
        headers.get(name).add(value);
        return this;
    }
}
