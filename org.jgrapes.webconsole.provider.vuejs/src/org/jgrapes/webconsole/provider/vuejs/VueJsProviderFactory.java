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

package org.jgrapes.webconsole.provider.vuejs;

import java.util.Map;
import java.util.Optional;
import org.jgrapes.core.Channel;
import org.jgrapes.core.ComponentType;
import org.jgrapes.webconsole.base.PageResourceProviderFactory;

/**
 * A factory for creating {@link VueJsProvider}s.
 */
public class VueJsProviderFactory implements PageResourceProviderFactory {

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.core.ComponentFactory#componentType()
     */
    @Override
    public Class<? extends ComponentType> componentType() {
        return VueJsProvider.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jgrapes.core.ComponentFactory#create(org.jgrapes.core.Channel,
     * java.util.Map)
     */
    @Override
    public Optional<ComponentType> create(
            Channel componentChannel, Map<?, ?> properties) {
        return Optional.of(new VueJsProvider(componentChannel));
    }

}
