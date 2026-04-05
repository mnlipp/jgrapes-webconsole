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

import static org.jdrupes.builder.api.Intent.*;
import static org.jdrupes.builder.api.ResourceType.*;
import static org.jdrupes.builder.java.JavaTypes.JavaResourceTreeType;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.core.FileTreeBuilder;
import org.jdrupes.builder.core.FileTreeBuilder.Source;
import org.jdrupes.builder.ext.nodejs.NpmExecutor;
import org.jdrupes.builder.java.JavaLibraryProject;
import org.jdrupes.builder.java.JavaProject;

import jdbld.Root;
import jdbld.console.Base;

public class Vue extends AbstractProject
        implements JavaProject, JavaLibraryProject {

    public Vue() {
        super(name("org.jgrapes.webconsole.provider.vue"));
        dependency(Expose, project(Base.class));
        Root.prepareNpm(dependency(Consume, NpmExecutor::new));
        dependency(Supply, FileTreeBuilder::new)
            .into(buildDirectory().resolve("generated/resources"))
            // "dummy" resource query to trigger npmInit maps to Sources
            .add(resources(
                of(ExecResultType).using(Consume).withName("npmInstall"))
                    .map(_ -> nodeModuleSource()).flatMap(s -> s))
            .provideResources(of(JavaResourceTreeType));
    }

    private Stream<Source> nodeModuleSource() {
        Path target = Path.of("org/jgrapes/webconsole/provider/vue/vue");
        return Stream.of(
// Replaced by version in resources/...
//            Source.of(FileTree.of(this,
//                Path.of("node_modules/vue/dist"), "vue.d.ts"))
//                .replaceAll("@vue/", "./")
//                .rename(p -> target.resolve(p)),
            Source.of(FileTree.of(this,
                Path.of("node_modules/vue/dist"), "*-browser.*"))
                .rename(p -> target.resolve(p)),
            Source.of(FileTree.of(this,
                Path.of("node_modules/@vue/runtime-core/dist"), "*.d.ts"))
                .rename(p -> target.resolve(p)),
            Source.of(FileTree.of(this,
                Path.of("node_modules/@vue/runtime-dom/dist"), "*.d.ts"))
                .rename(p -> target.resolve(p)));
    }
}
