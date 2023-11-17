package dk.dbc.buildstuff.model;

import dk.dbc.buildstuff.GitHelper;
import dk.dbc.buildstuff.Main;
import jakarta.xml.bind.annotation.XmlAttribute;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class Namespace {
    @XmlAttribute(name = "short", required = true)
    private String shortName;
    @XmlAttribute(required = true)
    private String namespace;
    @XmlAttribute(name = "git-repo")
    private String gitRepo;
    @XmlAttribute
    private String path;
    @XmlAttribute
    private String branch;
    private Path repoTempPath;

    public String getShortName() {
        return shortName;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getGitRepo() {
        if(gitRepo != null) {
            Objects.requireNonNull(branch, "When targeting a git repo you MUST set the branch");
        }
        return gitRepo;
    }

    public String getPath() {
        return path == null ? namespace : path;
    }

    public String getBranch() {
        return branch;
    }

    public Path getTargetPath() {
        Path base = repoTempPath == null ? Main.getBasePath() : repoTempPath;
        return base.resolve(getPath());
    }

    public Git checkout(String token) throws IOException, GitAPIException {
        if(getGitRepo() == null) return null;
        repoTempPath = Files.createTempDirectory("buildstuff-" + namespace + "_");
        return GitHelper.checkOut(repoTempPath, gitRepo, branch, token);
    }

    public void checkin(Git git, String token) throws GitAPIException {
        if(git != null)
            GitHelper.checkIn(git, branch, token);
    }

    @Override
    public String toString() {
        return "Namespace{" +
                "shortName='" + shortName + '\'' +
                ", namespace='" + namespace + '\'' +
                '}';
    }
}
