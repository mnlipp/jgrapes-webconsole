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

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.security.auth.Subject;
import org.jgrapes.http.Session;

/**
 * 
 */
public final class WebConsoleUtils {

    private WebConsoleUtils() {
    }

    /**
     * Convenience method for retrieving the console user from
     * a {@link Subject} associated with the session.
     *
     * @param session the session
     * @return the user principal
     */
    public static Optional<ConsoleUser> userFromSession(Session session) {
        return Optional.ofNullable((Subject) session.get(Subject.class))
            .flatMap(subject -> subject.getPrincipals(ConsoleUser.class)
                .stream().findFirst());
    }

    /**
     * Create a {@link URI} from a path. This is similar to calling
     * `new URI(null, null, path, null)` with the {@link URISyntaxException}
     * converted to a {@link IllegalArgumentException}.
     * 
     * @param path the path
     * @return the uri
     * @throws IllegalArgumentException if the string violates 
     * RFC 2396
     */
    @SuppressWarnings("PMD.AvoidUncheckedExceptionsInSignatures")
    public static URI uriFromPath(String path) throws IllegalArgumentException {
        try {
            return new URI(null, null, path, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns the query part of a URI as map. Note that query parts
     * can have multiple entries with the same key.
     *
     * @param uri the uri
     * @return the map
     */
    public static Map<String, List<String>> queryAsMap(URI uri) {
        if (uri.getQuery() == null) {
            return new HashMap<>();
        }
        @SuppressWarnings("PMD.UseConcurrentHashMap")
        Map<String, List<String>> result = new HashMap<>();
        Arrays.stream(uri.getQuery().split("&")).forEach(item -> {
            String[] pair = item.split("=");
            result.computeIfAbsent(pair[0], key -> new ArrayList<>())
                .add(pair[1]);
        });
        return result;
    }

    /**
     * Merge query parameters into an existing URI.
     *
     * @param uri the URI
     * @param parameters the parameters
     * @return the new URI
     */
    @SuppressWarnings({ "PMD.AvoidInstantiatingObjectsInLoops",
        "PMD.UselessParentheses" })
    public static URI mergeQuery(URI uri, Map<String, String> parameters) {
        Map<String, List<String>> oldQuery = queryAsMap(uri);
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            oldQuery.computeIfAbsent(entry.getKey(), key -> new ArrayList<>())
                .add(entry.getValue());
        }
        String newQuery = oldQuery.entrySet().stream()
            .map(entry -> entry.getValue().stream()
                .map(
                    value -> WebConsoleUtils.isoEncode(entry.getKey()) + "="
                        + WebConsoleUtils.isoEncode(value))
                .collect(Collectors.joining("&")))
            .collect(Collectors.joining("&"));
        // When constructing the new URI, we cannot pass the newQuery
        // to the constructor because it would be encoded once more.
        // So we build the most basic parseable URI with the newQuery
        // and resolve it against the remaining information.
        try {
            return new URI(uri.getScheme(), uri.getAuthority(), null, null,
                null).resolve(
                    uri.getRawPath() + "?" + newQuery
                        + (uri.getFragment() == null ? ""
                            : ("#" + uri.getRawFragment())));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Returns `URLEncoder.encode(value, "ISO-8859-1")`.
     *
     * @param value the value
     * @return the result
     */
    public static String isoEncode(String value) {
        try {
            return URLEncoder.encode(value, "ISO-8859-1");
        } catch (UnsupportedEncodingException e) {
            // Cannot happen, ISO-8859-1 is specified to be supported
            throw new IllegalStateException(e);
        }
    }
}
