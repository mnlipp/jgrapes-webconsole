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

package org.jgrapes.webconsole.base;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

import org.jgrapes.core.Channel;
import org.jgrapes.core.Component;
import org.jgrapes.core.annotation.Handler;
import org.jgrapes.io.IOSubchannel;
import org.jgrapes.webconsole.base.events.AddPageResources;
import org.jgrapes.webconsole.base.events.ConsoleReady;
import org.jgrapes.webconsole.base.events.PageResourceRequest;

/**
 * Base class for implementing components that add resources to
 * the `<HEAD>` section of the web console page.
 * 
 * A derived class must implement a handler for {@link ConsoleReady}
 * that generates an {@link AddPageResources} event. This
 * will, in turn, result in a {@link PageResourceRequest} that must
 * be handled by the derived class' 
 * {@link #onResourceRequest(PageResourceRequest, IOSubchannel)} method.
 * 
 * @see AddPageResources
 */
public abstract class PageResourceProvider extends Component {

    /**
     * Creates a new component.
     * 
     * @param channel the channel to listen on
     */
    public PageResourceProvider(Channel channel) {
        super(channel);
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
     * A default handler for resource requests. Searches for
     * a file with the requested resource URI in the component's 
     * class path and sets its {@link URL} as result if found.
     * 
     * @param event the resource request event
     * @param channel the channel that the request was received on
     */
    @Handler
    public final void onResourceRequest(
            PageResourceRequest event, IOSubchannel channel) {
        URL resourceUrl = this.getClass().getResource(
            event.resourceUri().getPath());
        if (resourceUrl == null) {
            return;
        }
        event.setResult(new ResourceByUrl(event, resourceUrl));
        event.stop();
    }

}
