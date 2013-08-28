package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.dataio.engine.JavaScript;
import dk.dbc.dataio.gui.client.exceptions.JavaScriptProjectFetcherException;
import dk.dbc.dataio.gui.client.proxies.JavaScriptProjectFetcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class JavaScriptProjectFetcherImpl implements JavaScriptProjectFetcher {
    @Override
    public List<RevisionInfo> fetchRevisions(String projectUrl) throws JavaScriptProjectFetcherException {
        final Date now = new Date();
        final RevisionInfo.ChangedItem item1 = new RevisionInfo.ChangedItem("/path/file1", "D");
        final RevisionInfo.ChangedItem item2 = new RevisionInfo.ChangedItem("/path/file2", "A");
        final RevisionInfo.ChangedItem item3 = new RevisionInfo.ChangedItem("/path/file1", "A");
        final RevisionInfo rev42 = new RevisionInfo(42L, "bob", now, "message for revision 42", new ArrayList<RevisionInfo.ChangedItem>(Arrays.asList(item1, item2)));
        final RevisionInfo rev10 = new RevisionInfo(10L, "joe", new Date(now.getTime() - 1000000), "message for revision 10", new ArrayList<RevisionInfo.ChangedItem>(Arrays.asList(item3)));
        return new ArrayList<RevisionInfo>(Arrays.asList(rev42, rev10));
    }

    @Override
    public List<String> fetchJavaScriptFileNames(String projectUrl, long revision) throws JavaScriptProjectFetcherException {
        return new ArrayList<String>(Arrays.asList(
            "/path/a/file1",
            "/path/a/file2",
            "/path/b/file1"
        ));
    }

    @Override
    public List<String> fetchJavaScriptInvocationMethods(String projectUrl, long revision, String javaScriptFileName) throws JavaScriptProjectFetcherException {
        return new ArrayList<String>(Arrays.asList(
            "functionA",
            "functionB",
            "functionC"
        ));
    }

    @Override
    public List<JavaScript> fetchRequiredJavaScript(String projectUrl, long revision, String javaScriptFileName) throws JavaScriptProjectFetcherException {
        final JavaScript main = new JavaScript("main script", "");
        final JavaScript dependency = new JavaScript("dependency script", "Dependable");
        return new ArrayList<JavaScript>(Arrays.asList(main, dependency));
    }
}
