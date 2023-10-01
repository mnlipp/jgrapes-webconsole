/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.webconsole.provider.markdownit;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateNotFoundException;
import java.io.IOException;
import java.util.List;
import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.webconsole.base.ConsoleConnection;
import org.jgrapes.webconsole.base.PageResourceProvider;
import org.jgrapes.webconsole.base.events.AddPageResources;
import org.jgrapes.webconsole.base.events.AddPageResources.ScriptResource;
import org.jgrapes.webconsole.base.events.ConsoleReady;

/**
 * A provider for the [Markdownit](https://github.com/markdown-it/markdown-it)
 * library.
 */
public class MarkdownItProvider extends PageResourceProvider {

    /**
     * Creates a new component with its channel set to the given 
     * channel.
     * 
     * @param componentChannel the channel that the component's 
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to 
     */
    public MarkdownItProvider(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * On {@link ConsoleReady}, fire the appropriate {@link AddPageResources}.
     *
     * @param event the event
     * @param connection the web console connection
     * @throws TemplateNotFoundException the template not found exception
     * @throws MalformedTemplateNameException the malformed template name exception
     * @throws ParseException the parse exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler(priority = 100)
    @SuppressWarnings({ "PMD.AvoidDuplicateLiterals",
        "PMD.AvoidInstantiatingObjectsInLoops" })
    public void onConsoleReady(ConsoleReady event,
            ConsoleConnection connection)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        String minExt = event.renderSupport()
            .useMinifiedResources() ? ".min" : "";
        var pageResources = new AddPageResources()
            .addScriptResource(new ScriptResource()
                .setProvides(new String[] { "markdown-it" })
                .setScriptUri(event.renderSupport().pageResource(
                    "markdown-it/markdown-it" + minExt + ".js")));
        for (var pkg : List.of("abbr", "container", "deflist", "emoji",
            "footnote", "ins", "mark", "sub", "sup")) {
            pageResources.addScriptResource(new ScriptResource()
                .setProvides(new String[] { "markdown-it-" + pkg })
                .setScriptUri(event.renderSupport().pageResource(
                    "markdown-it/markdown-it-" + pkg + minExt + ".js")));
        }
        connection.respond(pageResources);
    }
}
