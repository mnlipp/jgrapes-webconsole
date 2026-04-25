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
import org.jdrupes.builder.java.JavaLibraryProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;

import jdbld.conlet.FormTest;
import jdbld.conlet.HelloSolid;
import jdbld.conlet.HelloWorld;
import jdbld.conlet.JmxBrowser;
import jdbld.conlet.LocalLogin;
import jdbld.conlet.Logviewer;
import jdbld.conlet.MarkdownDisplay;
import jdbld.conlet.OidcLogin;
import jdbld.conlet.StyleTest;
import jdbld.conlet.SysInfo;
import jdbld.console.Bootstrap4Console;
import jdbld.console.JqueryUiConsole;
import jdbld.console.VueJsConsole;
import jdbld.provider.ChartJs;
import jdbld.provider.Jquery;
import jdbld.provider.MarkdownIt;
import jdbld.provider.Vue;

public class WebConsoleTest extends AbstractProject
        implements JavaProject, JavaLibraryProject, Unpublishable {

    public WebConsoleTest() {
        super(directory(Path.of("WebConsoleTest")));
        dependency(Consume, project(Rbac.class));
        dependency(Consume, project(VueJsConsole.class));
        dependency(Consume, project(Bootstrap4Console.class));
        dependency(Consume, project(JqueryUiConsole.class));
        dependency(Consume, project(MarkdownDisplay.class));
        dependency(Consume, project(LocalLogin.class));
        dependency(Consume, project(OidcLogin.class));
        dependency(Consume,
            new MvnRepoLookup().resolve("org.osgi:osgi.core:6.0.0"));

        dependency(Reveal, project(Jquery.class));
        dependency(Reveal, project(ChartJs.class));
        dependency(Reveal, project(MarkdownIt.class));
        dependency(Reveal, project(Vue.class));
        dependency(Reveal, project(HelloSolid.class));
        dependency(Reveal, project(HelloWorld.class));
        dependency(Reveal, project(FormTest.class));
        dependency(Reveal, project(StyleTest.class));
        dependency(Reveal, project(SysInfo.class));
        dependency(Reveal, project(Logviewer.class));
        dependency(Reveal, project(JmxBrowser.class));
        dependency(Reveal, new MvnRepoLookup().resolve(
            "com.electronwill.night-config:yaml:[3.6.7,3.7)",
            "org.eclipse.angus:angus-activation:[1.0.0,2.0.0)"));
    }
}
