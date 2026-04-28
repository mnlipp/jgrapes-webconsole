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
import jdbld.Root;
import jdbld.console.Base;
import org.jdrupes.builder.api.FileTree;
import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.core.AbstractProject;
import org.jdrupes.builder.ext.nodejs.NpmExecutor;
import org.jdrupes.builder.java.JavaLibraryProject;
import org.jdrupes.builder.java.JavaProject;

public class MarkdownIt extends AbstractProject
        implements JavaProject, JavaLibraryProject {

    public MarkdownIt() {
        super(name("org.jgrapes.webconsole.provider.markdownit"));
        dependency(Expose, project(Base.class));
        Root.addNpmResourcesBuilder(
            Root.prepareNpm(dependency(Supply, NpmExecutor::new)),
            Path.of("org/jgrapes/webconsole/provider/markdownit/markdown-it"),
            FileTree.of(this, Path.of("node_modules/markdown-it/dist")),
            FileTree.of(this, Path.of("node_modules/markdown-it-abbr/dist")),
            FileTree.of(this,
                Path.of("node_modules/markdown-it-container/dist")),
            FileTree.of(this, Path.of("node_modules/markdown-it-deflist/dist")),
            FileTree.of(this, Path.of("node_modules/markdown-it-emoji/dist")),
            FileTree.of(this,
                Path.of("node_modules/markdown-it-footnote/dist")),
            FileTree.of(this, Path.of("node_modules/markdown-it-ins/dist")),
            FileTree.of(this, Path.of("node_modules/markdown-it-mark/dist")),
            FileTree.of(this, Path.of("node_modules/markdown-it-sub/dist")),
            FileTree.of(this, Path.of("node_modules/markdown-it-sup/dist")));
    }
}
