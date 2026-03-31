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
import java.util.stream.Stream;

import org.jdrupes.builder.api.DocumentationDirectory;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.ext.nodejs.NpmExecutor;
import org.jdrupes.builder.java.JavaLibraryProject;
import org.jdrupes.builder.java.JavaProject;

public class Bootstrap4Console extends AbstractProject
        implements JavaProject, JavaLibraryProject {

    public Bootstrap4Console() {
        super(name("org.jgrapes.webconsole.vuejs"));
        dependency(Expose, project(Base.class));
        dependency(Reveal, project(Jquery.class));
        dependency(Reveal, project(Forkawesome.class));
        dependency(Reveal, project(Gridstack.class));

        Root.addNpmResourcesBuilder(
            Root.prepareNpm(dependency(Supply, NpmExecutor::new)),
            Path.of("org/jgrapes/webconsole/bootstrap4/lib/bootstrap"),
            FileTree.of(this, Path.of("node_modules/bootstrap/dist"), "**/*"));

        // jsdoc
        Root.prepareNpm(dependency(Supply, NpmExecutor::new)).name("apidocs")
            .provideResources(of(DocumentationDirectory.class))
            .args("run", "jsdoc")
            .required(Path.of("resoures/org/jgrapes/webconsole/jqueryui"),
                "**/*")
            .output(p -> Stream.of(DocumentationDirectory.of(p,
                p.rootProject().buildDirectory().resolve("javadoc/jsdoc"))));
    }
}
