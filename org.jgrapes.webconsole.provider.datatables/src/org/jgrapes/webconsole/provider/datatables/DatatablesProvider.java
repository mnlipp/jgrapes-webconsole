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

package org.jgrapes.webconsole.provider.datatables;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateNotFoundException;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.webconsole.base.ConsoleSession;
import org.jgrapes.webconsole.base.PageResourceProvider;
import org.jgrapes.webconsole.base.StylingInfo;
import org.jgrapes.webconsole.base.events.AddPageResources;
import org.jgrapes.webconsole.base.events.AddPageResources.ScriptResource;
import org.jgrapes.webconsole.base.events.ConsoleReady;

/**
 * Provider for the [Datatables](https://datatables.net/) library.
 */
public class DatatablesProvider extends PageResourceProvider {

    private final StylingInfo stylingInfo;

    /**
     * Creates a new component with its channel set to the given 
     * channel.
     * 
     * @param componentChannel the channel that the component's 
     * handlers listen on by default and that 
     * {@link Manager#fire(Event, Channel...)} sends the event to 
     */
    public DatatablesProvider(Channel componentChannel,
            Map<Object, Object> properties) {
        super(componentChannel);
        stylingInfo = new StylingInfo(this, properties);
    }

    /**
     * Provides a resource bundle for localization.
     * The default implementation looks up a bundle using the
     * package name plus "l10n" as base name.
     * 
     * @return the resource bundle
     */
    protected ResourceBundle resourceBundle(Locale locale) {
        return ResourceBundle.getBundle(
            getClass().getPackage().getName() + ".l10n", locale,
            getClass().getClassLoader(),
            ResourceBundle.Control.getNoFallbackControl(
                ResourceBundle.Control.FORMAT_DEFAULT));
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
    public void onPortalReady(ConsoleReady event, ConsoleSession portalSession)
            throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException {
        String minExt = event.renderSupport()
            .useMinifiedResources() ? ".min" : "";
        ResourceBundle bundle = resourceBundle(portalSession.locale());
        String script = "$.fn.dataTable.defaults.oLanguage._hungarianMap"
            + "[\"lengthAll\"] = \"sLengthAll\";\n"
            + "$.extend( $.fn.dataTable.defaults.oLanguage, {\n"
            + "	'sLengthAll': 'all',\n"
            + "} );\n"
            + "$.extend( $.fn.dataTable.defaults.oLanguage, "
            + bundle.getString("DataTablesL10n") + ");\n";
        String baseDir = "datatables-20180804";
        String styling = stylingInfo.get();
        if (!"jqueryui".equals(styling) && !"bootstrap4".equals(styling)) {
            styling = "standard";
        }
        AddPageResources addRequest = new AddPageResources()
            .addCss(event.renderSupport()
                .pageResource(baseDir + "/" + styling + "/datatables"
                    + minExt + ".css"))
            .addScriptResource(new ScriptResource()
                .setProvides(new String[] { "datatables.net" })
                .setScriptUri(event.renderSupport().pageResource(
                    baseDir + "/" + styling + "/datatables" + minExt + ".js")))
            .addScriptResource(new ScriptResource()
                .setRequires(new String[] { "datatables.net" })
                .setScriptUri(event.renderSupport().pageResource(
                    baseDir + "/processing().js")))
            .addScriptResource(new ScriptResource()
                .setRequires(new String[] { "datatables.net" })
                .setScriptSource(script));
        if ("jqueryui".equals(styling)) {
            addRequest.addCss(event.renderSupport().pageResource(
                "jqueryui-overrides-1.0.0.css"));
        }
        portalSession.respond(addRequest);
    }

}
