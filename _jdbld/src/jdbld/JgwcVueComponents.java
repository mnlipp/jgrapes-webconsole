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
import static org.jdrupes.builder.java.JavaTypes.JavaResourceTreeType;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.jdrupes.builder.api.FileResource;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.core.FileTreeBuilder;
import org.jdrupes.builder.core.FileTreeBuilder.Source;
import org.jdrupes.builder.ext.nodejs.NpmExecutor;
import org.jdrupes.builder.java.JavaLibraryProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.java.JavaResourceTree;

public class JgwcVueComponents extends AbstractProject
        implements JavaProject, JavaLibraryProject {

    public JgwcVueComponents() {
        super(name("org.jgrapes.webconsole.provider.jgwcvuecomponents"));
        dependency(Expose, project(Base.class));
        dependency(Reveal, project(Vue.class));
        var aashPrj = project(AashVueComponents.class);
        Root.asBundleBuilder(dependency(Supply, NpmExecutor::new)
            .args("run", "build")
            .required(project(Base.class)
                .resources(of(JavaResourceTree.class).using(Supply)))
            .required(project(Vue.class)
                .resources(of(JavaResourceTree.class).using(Supply)))
            .required(aashPrj.resources(of(FileTree.class).using(Supply)))
            .required(Path.of("node_modules"), "**/*")
            .required(Path.of("src"), "**/*.ts")
            .required(Path.of("tsconfig.json"))
            .required(Path.of("rollup.config.mjs"))
            .generated(p -> Stream.of(JavaResourceTree.of(p,
                p.buildDirectory().resolve("generated/resources"),
                "**/*"))));
        dependency(Supply, FileTreeBuilder::new)
            .into(buildDirectory().resolve("generated/resources"))
            .add(aashPrj.resources(of(FileTree.class).using(Supply))
                .map(ft -> Source.of(ft).rename(p -> Path.of(
                    "org/jgrapes/webconsole/provider/jgwcvuecomponents"
                        + "/aash-vue-components/lib")
                    .resolve(p))))
            .add(Source.of(FileTree.of(aashPrj, aashPrj.directory(),
                FileResource.class, "src/**/*")).rename(
                    p -> Path.of("org/jgrapes/webconsole/provider"
                        + "/jgwcvuecomponents/aash-vue-components")
                        .resolve(p)))
            .provideResources(of(JavaResourceTreeType));
    }
}
