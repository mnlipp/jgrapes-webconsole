/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2024 Michael N. Lipp
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

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The RPCs for the web console.
 */
@SuppressWarnings("PMD.EmptyCatchBlock")
public class WcJsonRpc extends JsonRpc {

    @SuppressWarnings("unused")
    private static List<String> stringList;
    @SuppressWarnings({ "PMD.SingularField", "unused" })
    private static Type stringListType;

    public record ConletInfo(String conletId, List<String> modes,
            Map<?, ?> opts) {
    }

    static {
        try {
            stringListType = WcJsonRpc.class.getDeclaredField("stringList")
                .getGenericType();
        } catch (NoSuchFieldException | SecurityException e) {
            // Cannot happen
        }
        setParamTypes(WcJsonRpc.class,
            Map.of(
                "addConlet", List.of(String.class, String[].class, Map.class),
                "conletsDeleted", List.of(ConletInfo[].class),
                "consoleLayout",
                List.of(String[].class, String[].class, Object.class),
                "notifyConletModel",
                List.of(String.class, String.class, Object[].class),
                "prepareConsole", Collections.emptyList(),
                "setLocale", List.of(String.class, Boolean.class),
                "setTheme", List.of(String.class),
                "renderConlet", List.of(String.class, String[].class),
                "retrievedLocalData", List.of(String[][].class)));
    }

}
