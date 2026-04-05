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
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.ext.nodejs.NpmExecutor;
import org.jdrupes.builder.java.JavaLibraryProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.java.JavaResourceTree;
import static org.jdrupes.builder.java.JavaTypes.*;

public class ChartJs extends AbstractProject
        implements JavaProject, JavaLibraryProject {

    public ChartJs() {
        super(name("org.jgrapes.webconsole.provider.chartjs"));
        dependency(Expose, project(Base.class));
        var npmExec = Root.prepareNpm(dependency(Supply, NpmExecutor::new));
        npmExec.args("run", "build").required(Path.of("src"), "**/*")
            .required(Path.of("tsconfig.json"))
            .required(Path.of("rollup.config.mjs"))
            .output(p -> Stream.of(JavaResourceTree.of(p,
                p.buildDirectory().resolve("generated/resources"),
                "**/*")))
            .provideResources(of(JavaResourceTreeType));
        Root.addNpmResourcesBuilder(npmExec,
            Path.of("org/jgrapes/webconsole/provider/chartjs/chart.js"),
            FileTree.of(this, Path.of("node_modules/chart.js"), "**/*.d.ts"));
    }
}
