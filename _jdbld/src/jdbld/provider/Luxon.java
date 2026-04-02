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
import java.nio.file.Path;
import org.jdrupes.builder.api.FileTree;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.ext.nodejs.NpmExecutor;
import org.jdrupes.builder.java.JavaLibraryProject;
import org.jdrupes.builder.java.JavaProject;

import jdbld.Root;
import jdbld.console.Base;

public class Luxon extends AbstractProject
        implements JavaProject, JavaLibraryProject {

    public Luxon() {
        super(name("org.jgrapes.webconsole.provider.luxon"));
        dependency(Expose, project(Base.class));
        var npmExec = Root.prepareNpm(dependency(Supply, NpmExecutor::new));
        Root.addNpmResourcesBuilder(npmExec,
            Path.of("org/jgrapes/webconsole/provider/luxon/luxon"),
            FileTree.of(this, Path.of("node_modules/luxon"),
                "build/es6/**", "src/**"));
        Root.addNpmResourcesBuilder(npmExec,
            Path.of("org/jgrapes/webconsole/provider/luxon/luxon/types"),
            FileTree.of(this, Path.of("node_modules/@types/luxon"), "**/*"));
    }
}
