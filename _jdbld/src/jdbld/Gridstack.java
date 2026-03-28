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
import org.jdrupes.builder.api.ExecResult;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.core.FileTreeBuilder;
import org.jdrupes.builder.core.FileTreeBuilder.Source;
import org.jdrupes.builder.ext.nodejs.NpmExecutor;
import org.jdrupes.builder.java.JavaLibraryProject;
import org.jdrupes.builder.java.JavaProject;
import static org.jdrupes.builder.java.JavaTypes.*;

public class Gridstack extends AbstractProject
        implements JavaProject, JavaLibraryProject {

    public Gridstack() {
        super(name("org.jgrapes.webconsole.provider.gridstack"));
        dependency(Expose, project(Base.class));
        Root.prepareNpm(dependency(Consume, NpmExecutor::new));
        dependency(Supply, FileTreeBuilder::new)
            .into(buildDirectory().resolve("generated/resources"))
            // "dummy" resource query to trigger npmInstall maps to Sources
            .add(resources(
                of(ExecResult.class).using(Consume).withName("npmInstall"))
                    .map(_ -> nodeModuleSource()).flatMap(s -> s))
            .provideResources(of(JavaResourceTreeType));
    }

    private Stream<Source> nodeModuleSource() {
        Path nodeSrc = Path.of("node_modules/gridstack/dist");
        Path target
            = Path.of("org/jgrapes/webconsole/provider/gridstack/gridstack");
        return Stream.of(
            // Copy non JS files unmodified
            Source.of(FileTree.of(this, nodeSrc, "**/*")
                .exclude("src/**").exclude("**/*.js"))
                .rename(p -> target.resolve(p)),
            // Copy JS files with modified imports
            Source.of(FileTree.of(this, nodeSrc, "**/*.js").exclude("src/**"))
                .rename(p -> target.resolve(p))
                .replaceAll(" from '(.*)'", " from '$1.js'"));
    }
}
