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
import org.jdrupes.builder.api.MergedTestProject;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.ext.nodejs.NpmExecutor;
import org.jdrupes.builder.java.JavaLibraryProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;

public class Base extends AbstractProject
        implements JavaProject, JavaLibraryProject {

    public Base() {
        super(name("org.jgrapes.webconsole.base"));
        dependency(Expose, new MvnRepoLookup()
            .resolve("org.jgrapes:org.jgrapes.core:[1.22.1,2)",
                "org.jgrapes:org.jgrapes.util:[1.38.0,2)",
                "org.jgrapes:org.jgrapes.io:[2.12.1,3)",
                "org.jgrapes:org.jgrapes.http:[3.5.0,4)",
                "org.freemarker:freemarker:[2.3.31,2.4)",
                "com.fasterxml.jackson.core:jackson-databind:2.18.0",
                "jakarta.mail:jakarta.mail-api:[2.1.3,3)"));
        dependency(Reveal, new MvnRepoLookup().resolve(
            "com.fasterxml.jackson.module:jackson-module-parameter-names:2.18.0",
            "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.0",
            "com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.18.0"));
        Root.asProjectBundleBuilder(dependency(Supply, NpmExecutor::new));

        // tsdoc
        Root.prepareNpm(dependency(Supply, NpmExecutor::new)).name("apidocs")
            .provideResources(of(DocumentationDirectory.class))
            .args("run", "typedoc")
            .required(Path.of("src"), "**/*.ts")
            .output(p -> Stream.of(DocumentationDirectory.of(p,
                p.rootProject().buildDirectory()
                    .resolve("javadoc/org/jgrapes/webconsole/base/jsdoc"))));
    }

    public static class BaseTest extends AbstractProject
            implements JavaProject, MergedTestProject {
        public BaseTest() {
            super(parent(Base.class));
            dependency(Consume, project(Base.class));
        }
    }
}
