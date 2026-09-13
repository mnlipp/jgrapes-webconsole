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

package jdbld.provider;

import java.nio.file.Path;
import java.util.stream.Stream;
import jdbld.AashLitComponents;
import jdbld.Root;
import jdbld.console.Base;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import static org.jdrupes.builder.api.Intent.*;
import static org.jdrupes.builder.api.ResourceType.BaseFileTreeType;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.core.FileTreeBuilder;
import org.jdrupes.builder.core.FileTreeBuilder.Source;
import org.jdrupes.builder.ext.nodejs.NpmExecutor;
import org.jdrupes.builder.java.JavaLibraryProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.java.JavaResourceTree;
import static org.jdrupes.builder.java.JavaTypes.JavaResourceTreeType;

public class AashLit extends AbstractProject
        implements JavaProject, JavaLibraryProject {

    public AashLit() {
        super(name("org.jgrapes.webconsole.provider.aashlit"));
        dependency(Expose, project(Base.class));
        dependency(Reveal, project(Lit.class));
        var aashPrj = project(AashLitComponents.class);
        var npmExec = Root.prepareNpm(dependency(Supply, NpmExecutor::new));
        npmExec.args("run", "build").required(Path.of("src"), "**/*")
            .required(Path.of("rollup.config.mjs"))
            .required(aashPrj.resources(of(BaseFileTreeType).using(Supply)))
            .provideResources(of(JavaResourceTreeType),
                p -> Stream.of(JavaResourceTree.of(p, p.buildDirectory()
                    .resolve("generated/resources/js"), "**/*")));
        dependency(Supply, FileTreeBuilder::new)
            .into(buildDirectory().resolve("generated/resources/src"))
            .add(Source.of(FileTree.of(aashPrj, aashPrj.directory(),
                FileResource.class, "src/**/*")).rename(
                    p -> Path.of(name().replace('.', '/'))
                        .resolve(Path.of("aash-lit"))
                        .resolve(Path.of("src").relativize(p))))
            .provideResources(of(JavaResourceTreeType));
    }
}
