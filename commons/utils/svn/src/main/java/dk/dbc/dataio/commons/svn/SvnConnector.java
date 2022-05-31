package dk.dbc.dataio.commons.svn;

import dk.dbc.dataio.commons.types.RevisionInfo;
import dk.dbc.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc2.SvnExport;
import org.tmatesoft.svn.core.wc2.SvnLog;
import org.tmatesoft.svn.core.wc2.SvnOperationFactory;
import org.tmatesoft.svn.core.wc2.SvnRevisionRange;
import org.tmatesoft.svn.core.wc2.SvnTarget;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * This utility class provides convenience methods for communication with a
 * Subversion source control management system.
 * <p>
 * Currently only file:// and http(s):// URL schemes are supported.
 */
public class SvnConnector {
    private static final Logger log = LoggerFactory.getLogger(SvnConnector.class);

    static {
        FSRepositoryFactory.setup();        // file
        DAVRepositoryFactory.setup();       // http, https
        //SVNRepositoryFactoryImpl.setup();   // svn, svn+xxx
    }

    private SvnConnector() {
    }

    /**
     * Retrieves information from Subversion source control management system for
     * all available commited revisions for project pointed to by given URL
     *
     * @param projectUrl unencoded project URL on the form http(s)://... or file://...
     * @return list of available revisions ordered from latest to earliest
     * @throws NullPointerException     if given null-valued project URL
     * @throws IllegalArgumentException if given empty-valued project URL
     * @throws SVNException             on failure to retrieve revisions from SVN server
     * @throws URISyntaxException       if given project URL could not be parsed as a URI reference
     */
    public static List<RevisionInfo> listAvailableRevisions(final String projectUrl)
            throws NullPointerException, IllegalArgumentException, SVNException, URISyntaxException {
        log.debug("Listing available revisions for URL '{}'", projectUrl);
        InvariantUtil.checkNotNullNotEmptyOrThrow(projectUrl, "projectUrl");

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        List<RevisionInfo> revisions;
        try {
            final SvnLog svnLog = svnOperationFactory.createLog();
            svnLog.setSingleTarget(SvnTarget.fromURL(asSvnUrl(projectUrl)));
            svnLog.setDiscoverChangedPaths(true);
            svnLog.addRange(SvnRevisionRange.create(
                    SVNRevision.create(0), SVNRevision.create(getLatestRevisionNumber(projectUrl))));

            // We get the log entries back from the server ordered earliest->latest.
            // We therefore use a LinkedList taking advantage of the fact that it
            // provides a descendingIterator, allowing us to return in latest->earliest
            // order.
            final Collection<SVNLogEntry> logEntries = new LinkedList<>();
            svnLog.run(logEntries);
            if (logEntries.isEmpty()) {
                log.warn("No log entries returned for URL '{}'", projectUrl);
            }

            // This cast is safe since we know for a fact that the data structure
            // underlying this collection is a LinkedList.
            revisions = processSvnLogEntries((LinkedList<SVNLogEntry>) logEntries);
        } finally {
            svnOperationFactory.dispose();
        }
        return revisions;
    }

    /**
     * Creates a SVNRepository driver according to the protocol that is to be used to access a repository
     *
     * @param projectUrl unencoded project URL on the form http(s)://... or file://...
     * @return protocol specific {@link SVNRepository} driver
     * @throws URISyntaxException if given project URL could not be parsed as a URI reference
     * @throws SVNException       if there's no implementation for the specified protocol
     */
    public static SVNRepository getRepository(final String projectUrl) throws URISyntaxException, SVNException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(projectUrl, "projectUrl");
        return SVNRepositoryFactory.create(asSvnUrl(projectUrl));
    }

    /**
     * Tests if given directory exists in given repository
     *
     * @param svnRepository subversion repository
     * @param dir           directory name
     * @return true if directory exists, otherwise false
     * @throws SVNException if a failure occurs while connecting to a repository
     */
    public static boolean dirExists(final SVNRepository svnRepository, final String dir) throws SVNException {
        return dir != null && svnRepository != null
                && svnRepository.checkPath(dir, -1) == SVNNodeKind.DIR;
    }

    /**
     * Retrieves all available paths in given revision of project pointed to by
     * given URL from Subversion source control management system
     *
     * @param projectUrl unencoded project URL on the form http(s)://... or file://...
     * @param revision   project revision
     * @return list of available paths
     * @throws NullPointerException     if given null-valued project URL
     * @throws IllegalArgumentException if given empty-valued project URL
     * @throws SVNException             on failure to retrieve paths from given revision
     * @throws URISyntaxException       if given project URL could not be parsed as a URI reference
     */
    public static List<String> listAvailablePaths(final String projectUrl, final long revision)
            throws NullPointerException, IllegalArgumentException, SVNException, URISyntaxException {
        log.debug("Listing available paths for revision {} URL '{}'", revision, projectUrl);
        InvariantUtil.checkNotNullNotEmptyOrThrow(projectUrl, "projectUrl");

        final SVNRepository svnRepository = SVNRepositoryFactory.create(asSvnUrl(projectUrl));
        final List<String> availablePaths = new ArrayList<>();
        try {
            processPath(svnRepository, revision, "", availablePaths);
        } finally {
            svnRepository.closeSession();
        }
        return availablePaths;
    }

    /**
     * Executes export operation for project pointed to by given URL from Subversion
     * source control management system
     * <p>
     * Note that the specified revision is set as the Peg revision (as opposed to the
     * operative revision) meaning export the contents of whatever file or directory
     * occupied the URL at that point in history.
     *
     * @param projectUrl unencoded project URL on the form http(s)://... or file://...
     * @param revision   project revision
     * @param exportTo   file system path of exported copy
     * @throws NullPointerException     if given null-valued project URL or export path
     * @throws IllegalArgumentException if given empty-valued project URL or export path
     * @throws SVNException             on failure to export given revision
     * @throws URISyntaxException       if given project URL could not be parsed as a URI reference
     */
    public static void export(final String projectUrl, final long revision, final Path exportTo)
            throws NullPointerException, IllegalArgumentException, SVNException, URISyntaxException {
        log.debug("Exporting revision {} of URL '{}' to '{}'", revision, projectUrl, exportTo);
        InvariantUtil.checkNotNullNotEmptyOrThrow(projectUrl, "projectUrl");
        InvariantUtil.checkNotNullOrThrow(exportTo, "exportTo");

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory();
        try {
            final SvnExport export = svnOperationFactory.createExport();
            export.setSource(SvnTarget.fromURL(asSvnUrl(projectUrl), SVNRevision.create(revision)));
            export.setSingleTarget(SvnTarget.fromFile(exportTo.toFile()));
            export.setIgnoreExternals(true);
            export.run();
        } finally {
            svnOperationFactory.dispose();
        }
    }

    private static long getLatestRevisionNumber(final String projectUrl) throws SVNException, URISyntaxException {
        final SVNRepository svnRepository = SVNRepositoryFactory.create(asSvnUrl(projectUrl));
        long latestRevision;
        try {
            latestRevision = svnRepository.getLatestRevision();
        } finally {
            svnRepository.closeSession();
        }
        return latestRevision;
    }

    private static SVNURL asSvnUrl(final String url) throws SVNException, URISyntaxException {
        final URI uri = new URI(url);
        return SVNURL.parseURIEncoded(uri.toASCIIString());
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static List<RevisionInfo> processSvnLogEntries(final LinkedList<SVNLogEntry> logEntries) {
        log.debug("{} log entries to process", logEntries.size());
        final List<RevisionInfo> revisions = new ArrayList<>();
        for (final Iterator<SVNLogEntry> iterator = logEntries.descendingIterator(); iterator.hasNext(); ) {
            final SVNLogEntry logEntry = iterator.next();
            final RevisionInfo revision = new RevisionInfo(logEntry.getRevision(), logEntry.getAuthor(),
                    logEntry.getDate(), logEntry.getMessage(), processChangedPaths(logEntry.getChangedPaths().values()));
            revisions.add(revision);
        }
        return revisions;
    }

    private static List<RevisionInfo.ChangedItem> processChangedPaths(final Collection<SVNLogEntryPath> paths) {
        log.debug("{} changed paths to process", paths.size());
        final List<RevisionInfo.ChangedItem> changedItems = new ArrayList<>();
        for (SVNLogEntryPath changedPath : paths) {
            changedItems.add(new RevisionInfo.ChangedItem(
                    changedPath.getPath(), String.valueOf(changedPath.getType())));
        }
        return changedItems;
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private static void processPath(SVNRepository repository, long revision, String path, Collection<String> pathEntries) throws SVNException {
        final Collection<SVNDirEntry> dirEntries = new ArrayList<>();
        repository.getDir(path, revision, false, dirEntries);
        for (SVNDirEntry dirEntry : dirEntries) {
            String fullPath = String.format("%s/%s", path, dirEntry.getName());
            fullPath = fullPath.replaceFirst("^/", "");  // trim leading slash
            pathEntries.add(fullPath);
            if (dirEntry.getKind() == SVNNodeKind.DIR) {
                processPath(repository, revision, fullPath, pathEntries);
            }
        }
    }
}
