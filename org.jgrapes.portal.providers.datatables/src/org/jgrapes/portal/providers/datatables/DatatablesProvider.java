/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2018 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.portal.providers.datatables;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateNotFoundException;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.jgrapes.core.Channel;
import org.jgrapes.core.ComponentType;
import org.jgrapes.core.Components;
import org.jgrapes.core.Event;
import org.jgrapes.core.Manager;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.portal.base.PageResourceProvider;
import org.jgrapes.portal.base.PortalSession;
import org.jgrapes.portal.base.PortalWeblet;
import org.jgrapes.portal.base.events.AddPageResources;
import org.jgrapes.portal.base.events.AddPageResources.ScriptResource;
import org.jgrapes.portal.base.events.PortalReady;

/**
 * Provider for the [Datatables](https://datatables.net/) library.
 */
public class DatatablesProvider extends PageResourceProvider {

    private Map<Object, Object> properties;

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
        this.properties = properties;
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
     * On {@link PortalReady}, fire the appropriate {@link AddPageResources}.
     *
     * @param event the event
     * @param portalSession the portal session
     * @throws TemplateNotFoundException the template not found exception
     * @throws MalformedTemplateNameException the malformed template name exception
     * @throws ParseException the parse exception
     * @throws IOException Signals that an I/O exception has occurred.
     */
    @Handler(priority = 100)
    public void onPortalReady(PortalReady event, PortalSession portalSession)
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
        String styling = (String) properties.get("styling");
        if (styling == null) {
            // Try to get the information from the portal (weblet)
            ComponentType portal = this;
            while (true) {
                portal = Components.manager(portal).parent();
                if (portal == null) {
                    break;
                }
                if (portal instanceof PortalWeblet) {
                    styling = ((PortalWeblet) portal).styling();
                    break;
                }
            }
        }
        if (styling == null
            || (!styling.equals("jqueryui") && !styling.equals("bootstrap4"))) {
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
        if (styling.equals("jqueryui")) {
            addRequest.addCss(event.renderSupport().pageResource(
                "jqueryui-overrides-1.0.0.css"));
        }
        portalSession.respond(addRequest);
    }

}
