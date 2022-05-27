/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2022 Michael N. Lipp
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

package org.jgrapes.webconlet.examples.login;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.webconsole.base.Conlet.RenderMode;
import org.jgrapes.webconsole.base.ConletBaseModel;
import org.jgrapes.webconsole.base.ConsoleSession;
import org.jgrapes.webconsole.base.WebConsoleUtils;
import org.jgrapes.webconsole.base.events.AddConletRequest;
import org.jgrapes.webconsole.base.events.AddConletType;
import org.jgrapes.webconsole.base.events.AddPageResources.ScriptResource;
import org.jgrapes.webconsole.base.events.ConsolePrepared;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.OpenModalDialog;
import org.jgrapes.webconsole.base.events.RenderConletRequestBase;
import org.jgrapes.webconsole.base.events.SetLocale;
import org.jgrapes.webconsole.base.freemarker.FreeMarkerConlet;

/**
 * A conlet for poll administration.
 */
public class LoginConlet extends FreeMarkerConlet<ConletBaseModel> {

    /**
     * Creates a new component with its channel set to the given channel.
     * 
     * @param componentChannel the channel that the component's handlers listen
     *            on by default and that {@link Manager#fire(Event, Channel...)}
     *            sends the event to
     */
    public LoginConlet(Channel componentChannel) {
        super(componentChannel);
    }

    /**
     * Register conlet.
     *
     * @param event the event
     * @param channel the channel
     * @throws TemplateNotFoundException the template not found exception
     * @throws MalformedTemplateNameException the malformed template name exception
     * @throws ParseException the parse exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler
    public void onConsoleReady(ConsoleReady event, ConsoleSession channel)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        // Add conlet resources to page
        channel.respond(new AddConletType(type())
            .addScript(new ScriptResource()
                .setScriptUri(event.renderSupport().conletResource(
                    type(), "Login-functions.js"))
                .setScriptType("module"))
            .addCss(event.renderSupport(), WebConsoleUtils.uriFromPath(
                "Login-style.css")));
    }

    @Override
    protected String generateInstanceId(AddConletRequest event,
            ConsoleSession session) {
        return "Singleton";
    }

    /**
     * Handle web console page loaded.
     *
     * @param event the event
     * @param channel the channel
     * @throws IOException 
     * @throws ParseException 
     * @throws MalformedTemplateNameException 
     * @throws TemplateNotFoundException 
     */
    @Handler(priority = 1000)
    public void onConsolePrepared(ConsolePrepared event, ConsoleSession channel)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        event.stop();
        Template tpl = freemarkerConfig()
            .getTemplate("Login-dialog.ftl.html");
        final Map<String, Object> model
            = fmSessionModel(channel.browserSession());
        model.put("locale", channel.locale());
        var bundle = resourceBundle(channel.locale());
        channel.respond(new OpenModalDialog(processTemplate(event, tpl,
            model)).addOption("title", bundle.getString("title"))
                .addOption("cancelable", false).addOption("okayLabel", "")
                .addOption("applyLabel", bundle.getString("Submit")));
    }

    private Future<String> processTemplate(
            Event<?> request, Template template,
            Object dataModel) {
        return request.processedBy().map(procBy -> procBy.executorService())
            .orElse(Components.defaultExecutorService()).submit(() -> {
                StringWriter out = new StringWriter();
                try {
                    template.process(dataModel, out);
                } catch (TemplateException | IOException e) {
                    throw new IllegalArgumentException(e);
                }
                return out.toString();

            });
    }

    @Override
    protected Set<RenderMode> doRenderConlet(RenderConletRequestBase<?> event,
            ConsoleSession channel, String conletId,
            ConletBaseModel conletState) throws Exception {
        return Collections.emptySet();
    }

    @Override
    protected boolean doSetLocale(SetLocale event, ConsoleSession channel,
            String conletId) throws Exception {
        return true;
    }

}
