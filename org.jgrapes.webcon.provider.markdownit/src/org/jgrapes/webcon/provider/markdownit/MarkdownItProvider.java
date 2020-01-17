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

package org.jgrapes.webcon.provider.markdownit;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateNotFoundException;

import java.io.IOException;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.webcon.base.ConsoleSession;
import org.jgrapes.webcon.base.PageResourceProvider;
import org.jgrapes.webcon.base.events.AddPageResources;
import org.jgrapes.webcon.base.events.AddPageResources.ScriptResource;
import org.jgrapes.webcon.base.events.ConsoleReady;

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
     * @param portalSession the portal session
     * @throws TemplateNotFoundException the template not found exception
     * @throws MalformedTemplateNameException the malformed template name exception
     * @throws ParseException the parse exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler(priority = 100)
    @SuppressWarnings("PMD.AvoidDuplicateLiterals")
    public void onPortalReady(ConsoleReady event, ConsoleSession portalSession)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        String minExt = event.renderSupport()
            .useMinifiedResources() ? ".min" : "";
        portalSession.respond(new AddPageResources()
            .addScriptResource(new ScriptResource()
                .setProvides(new String[] { "markdown-it.github.io" })
                .setScriptUri(event.renderSupport().pageResource(
                    "markdown-it-8.0.4" + minExt + ".js")))
            .addScriptResource(new ScriptResource()
                .setRequires(new String[] { "markdown-it.github.io" })
                .setProvides(
                    new String[] { "github.com/markdown-it/markdown-it-abbr" })
                .setScriptUri(event.renderSupport().pageResource(
                    "markdown-it-abbr-1.0.4" + minExt + ".js")))
            .addScriptResource(new ScriptResource()
                .setRequires(new String[] { "markdown-it.github.io" })
                .setProvides(new String[] {
                    "github.com/markdown-it/markdown-it-container" })
                .setScriptUri(event.renderSupport().pageResource(
                    "markdown-it-container-2.0.0" + minExt + ".js")))
            .addScriptResource(new ScriptResource()
                .setRequires(new String[] { "markdown-it.github.io" })
                .setProvides(new String[] {
                    "github.com/markdown-it/markdown-it-deflist" })
                .setScriptUri(event.renderSupport().pageResource(
                    "markdown-it-deflist-2.0.3" + minExt + ".js")))
            .addScriptResource(new ScriptResource()
                .setRequires(new String[] { "markdown-it.github.io" })
                .setProvides(
                    new String[] { "github.com/markdown-it/markdown-it-emoji" })
                .setScriptUri(event.renderSupport().pageResource(
                    "markdown-it-emoji-1.4.0" + minExt + ".js")))
            .addScriptResource(new ScriptResource()
                .setRequires(new String[] { "markdown-it.github.io" })
                .setProvides(new String[] {
                    "github.com/markdown-it/markdown-it-footnote" })
                .setScriptUri(event.renderSupport().pageResource(
                    "markdown-it-footnote-3.0.1" + minExt + ".js")))
            .addScriptResource(new ScriptResource()
                .setRequires(new String[] { "markdown-it.github.io" })
                .setProvides(
                    new String[] { "github.com/markdown-it/markdown-it-ins" })
                .setScriptUri(event.renderSupport().pageResource(
                    "markdown-it-ins-2.0.0" + minExt + ".js")))
            .addScriptResource(new ScriptResource()
                .setRequires(new String[] { "markdown-it.github.io" })
                .setProvides(
                    new String[] { "github.com/markdown-it/markdown-it-mark" })
                .setScriptUri(event.renderSupport().pageResource(
                    "markdown-it-mark-2.0.0" + minExt + ".js")))
            .addScriptResource(new ScriptResource()
                .setRequires(new String[] { "markdown-it.github.io" })
                .setProvides(
                    new String[] { "github.com/markdown-it/markdown-it-sub" })
                .setScriptUri(event.renderSupport().pageResource(
                    "markdown-it-sub-1.0.0" + minExt + ".js")))
            .addScriptResource(new ScriptResource()
                .setRequires(new String[] { "markdown-it.github.io" })
                .setProvides(
                    new String[] { "github.com/markdown-it/markdown-it-sup" })
                .setScriptUri(event.renderSupport().pageResource(
                    "markdown-it-sup-1.0.0" + minExt + ".js"))));
    }
}
