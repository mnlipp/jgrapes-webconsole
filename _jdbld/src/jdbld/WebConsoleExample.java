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
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;
import org.jdrupes.builder.uberjar.UberJarBuilder;

public class WebConsoleExample extends AbstractProject
        implements JavaProject, Unpublishable {

    public WebConsoleExample() {
        super(directory(Path.of("WebConsoleExample")));
        dependency(Reveal, project(ConsoleApp.class));
        dependency(Reveal, project(SysInfo.class));
        dependency(Reveal, new MvnRepoLookup()
            .resolve("org.eclipse.angus:angus-activation:[1.0.0,2.0.0)"));

        generator(new UberJarBuilder(this)
            .addFrom(providers().select(Expose, Reveal))
            .mainClass(
                "org.jgrapes.webconsole.examples.consoleapp.ConsoleApp"));
    }
}
