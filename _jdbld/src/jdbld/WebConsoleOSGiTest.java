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

import static org.jdrupes.builder.api.Intent.*;

import java.nio.file.Path;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.api.ResourceType;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.core.FileTreeBuilder;
import org.jdrupes.builder.core.FileTreeBuilder.Source;
import org.jdrupes.builder.java.JavaExecutor;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.java.LibraryJarFile;
import static org.jdrupes.builder.java.JavaTypes.*;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;
import static org.jdrupes.builder.mvnrepo.MvnRepoTypes.*;

public class WebConsoleOSGiTest extends AbstractProject
        implements JavaProject, Unpublishable {

    public WebConsoleOSGiTest() {
        super(directory(Path.of("WebConsoleOSGiTest")));
        dependency(Reveal, project(WebConsoleTest.class));

        // Provides a OSGi repository
        dependency(Consume, FileTreeBuilder::new)
            .into(rootProject().buildDirectory().resolve("runRepo"))
            .add(project(WebConsoleTest.class)
                .resources(of(LibraryJarFileType).usingAll())
                .map(l -> Source.of(FileTree.of(this,
                    l.path().getParent(), l.path().getFileName().toString()))))
            .add(new MvnRepoLookup().resolve(
                "org.apache.felix:org.apache.felix.framework:7.0.5",
                "org.apache.aries.spifly:org.apache.aries.spifly.dynamic.bundle:1.3.7",
                "org.glassfish.hk2:osgi-resource-locator:2.4.0",
                "org.apache.felix:org.apache.felix.gogo.command:1.1.2",
                "org.apache.felix:org.apache.felix.gogo.runtime:1.1.4",
                "org.apache.felix:org.apache.felix.gogo.shell:1.1.4")
                .resources(of(MvnRepoLibraryJarFileType))
                .map(l -> Source.of(FileTree.of(this,
                    l.path().getParent(), l.path().getFileName().toString()))))
            .provideResources(of(new ResourceType<RunRepo>() {}));

        var bndProvider
            = new MvnRepoLookup().resolve("biz.aQute.bnd:biz.aQute.bnd:7.2.1");

        dependency(Supply, JavaExecutor::new).name("GoGo")
            .addFrom(bndProvider)
            .mainClass("aQute.bnd.main.bnd")
            .args("gogo.bndrun");
    }

    public static interface RunRepo extends FileTree<LibraryJarFile> {
    }
}
