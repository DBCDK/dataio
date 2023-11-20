package dk.dbc.buildstuff;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GitHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHelper.class);

    public static Git checkOut(Path dir, String repository, String branch, String token) throws GitAPIException {
        LOGGER.info("Cloning repository {} and setting branch {}", repository, branch);
        return Git.cloneRepository()
                .setURI(repository)
                .setCredentialsProvider(makeCredentials(token))
                .setBranch("refs/heads/" + branch)
                .setDirectory(dir.toFile())
                .call();
    }

    public static void checkIn(Git git, String branch, String token) throws GitAPIException {
        Status status = git.status().call();
        LOGGER.info("Pushing files to {}", branch);
        Set<String> changeset = Stream.concat(status.getUntracked().stream(), status.getModified().stream()).collect(Collectors.toSet());
        if(!changeset.isEmpty()) {
            AddCommand add = git.add();
            changeset.forEach(add::addFilepattern);
            add.call();
        }
        git.commit().setAuthor("Buildstuff Versioning", "").setMessage("Auto generated").call();
        git.push().setCredentialsProvider(makeCredentials(token)).call();
    }

    private static CredentialsProvider makeCredentials(String token) {
        return new UsernamePasswordCredentialsProvider("PRIVATE-TOKEN", token);
    }
}
