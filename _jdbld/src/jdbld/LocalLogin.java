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
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.ext.nodejs.NpmExecutor;
import org.jdrupes.builder.java.JavaLibraryProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.java.JavaResourceTree;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;

public class LocalLogin extends AbstractProject
        implements JavaProject, JavaLibraryProject, Unpublishable {

    public LocalLogin() {
        super(name("org.jgrapes.webconlet.locallogin"));
        dependency(Expose, project(Base.class));
        dependency(Reveal, project(Vue.class));
        dependency(Reveal, project(JgwcVueComponents.class));
        dependency(Consume, new MvnRepoLookup().resolve(
            "at.favre.lib:bcrypt:[0.10.2,0.11)"));

        Root.prepareNpm(dependency(Supply, NpmExecutor::new))
            .args("run", "build").required(Path.of("src"), "**/*.ts")
            .required(project(Base.class)
                .resources(of(JavaResourceTree.class).using(Supply)))
            .required(project(Vue.class)
                .resources(of(JavaResourceTree.class).using(Supply)))
            .required(project(JgwcVueComponents.class)
                .resources(of(JavaResourceTree.class).using(Supply)))
            .required(Path.of("tsconfig.json"))
            .required(Path.of("rollup.config.mjs"))
            .output(p -> Stream.of(JavaResourceTree.of(p,
                p.buildDirectory().resolve("generated/resources"), "**/*")))
            .provideResources(of(JavaResourceTree.class));
    }
}
