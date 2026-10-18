/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2026 Michael N. Lipp
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package jdbld;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.DocumentationDirectory;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.Intent;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import org.jdrupes.builder.api.ResourceRequest;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.core.AbstractGenerator;

/// The Class GhPagesPublisher.
///
public class GhPagesPublisher extends AbstractGenerator {

    /**
     * Instantiates a new gh pages publisher.
     *
     * @param project the project
     */
    public GhPagesPublisher(Project project) {
        super(project);
    }

    @Override
    protected <T extends Resource> Collection<T>
            doProvide(ResourceRequest<T> request) {
        if (!request.accepts(new ResourceType<GhPagesPublication>() {})) {
            return Collections.emptyList();
        }

        // Prepare docs
        var docDirs = project().rootProject().projects("**")
            .map(p -> p.resources(of(
                new ResourceType<DocumentationDirectory>() {})
                    .using(Intent.Supply)))
            .flatMap(s -> s);

        // Clone, copy, publish
        var workDir = project().buildDirectory().resolve("gh-pages");
        try {
            var git = cloneRepository(workDir);
            var toBeRemoved = copyDocs(docDirs, workDir);
            var rmCmd = git.rm().setCached(false);
            for (var path : toBeRemoved) {
                rmCmd.addFilepattern(path.toString());
            }
            rmCmd.call();
            git.add().addFilepattern(".").call();
            git.commit().setMessage("Update JGrapes Web Console Docs").call();
            git.push().setCredentialsProvider(
                new UsernamePasswordCredentialsProvider("mnlipp",
                    context().property("jgrapes.repo.access.token", "")))
                .setProgressMonitor(new TextProgressMonitor(
                    context().statusLine().writer(this.toString() + ": ")))
                .call();
        } catch (GitAPIException | IOException e) {
            throw new BuildException().from(this).cause(e);
        }

        @SuppressWarnings("unchecked")
        var result = (T) GhPagesPublication.create();
        return List.of(result);
    }

    private Git cloneRepository(Path workDir)
            throws GitAPIException, InvalidRemoteException, TransportException {
        // Start with a clean Clone
        workDir.toFile().mkdir();
        var files = FileTree.of(project(), workDir, "**/*");
        context().statusLine().update("%s cleaning %s", this, workDir);
        files.cleanup();
        var repoUri = "https://github.com/mnlipp/jgrapes.git";
        var branch = "gh-pages";
        var git = Git.cloneRepository().setURI(repoUri).setBranch(branch)
            .setDirectory(workDir.toFile()).setDepth(1)
            .setProgressMonitor(new TextProgressMonitor(
                context().statusLine().writer(this.toString() + ": ")))
            .call();
        return git;
    }

    private Set<Path> copyDocs(Stream<DocumentationDirectory> docDirs,
            Path workDir) throws IOException {
        // Copy docs
        context().statusLine().update("%s copying docs", this);
        var toBeRemoved = new HashSet<Path>();
        for (var docDir : docDirs.map(d -> d.root())
            .collect(Collectors.toSet())) {
            if (!docDir.startsWith(project().rootProject().buildDirectory())) {
                System.err.println("Skipping: " + docDir);
            }
            var targetDir = workDir.resolve(project().rootProject()
                .buildDirectory().relativize(docDir));
            var existingSet = FileTree.of(project(), targetDir, "**/*")
                .paths().collect(Collectors.toSet());
            var docFiles = FileTree.of(project(), docDir, "**/*");
            for (var iter = docFiles.paths().iterator(); iter.hasNext();) {
                var entry = iter.next();
                existingSet.remove(entry);
                Path srcFile = docFiles.root().resolve(entry);
                Path dstFile = targetDir.resolve(entry);
                dstFile.getParent().toFile().mkdirs();
                Files.copy(srcFile, dstFile,
                    StandardCopyOption.REPLACE_EXISTING);
            }
            existingSet.stream()
                .map(p -> workDir.relativize(targetDir.resolve(p)))
                .forEach(toBeRemoved::add);
        }
        return toBeRemoved;
    }

}
