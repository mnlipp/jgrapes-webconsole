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

package org.jgrapes.webconlet.oidclogin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * The Class OidcProviderData.
 */
@SuppressWarnings("PMD.DataClass")
public class JsonWebToken {

    private static ObjectMapper mapper = new ObjectMapper();
    private Map<String, Object> header;
    private Map<String, Object> payload;
    private byte[] signature;

    /**
     * Parses the given token as JWT.
     *
     * @param token the token
     * @return the json web token
     */
    public static JsonWebToken parse(String token) {
        try {
            var result = new JsonWebToken();
            var tokenizer = new StringTokenizer(token, ".");
            result.header = convertPart(tokenizer.nextToken());
            result.payload = convertPart(tokenizer.nextToken());
            result.signature
                = Base64.getUrlDecoder().decode(tokenizer.nextToken());
            return result;
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Map<String, Object> convertPart(String part)
            throws JsonMappingException, JsonProcessingException {
        return mapper.readValue(new String(Base64.getUrlDecoder().decode(part)),
            TypeFactory.defaultInstance()
                .constructMapType(HashMap.class, String.class, Object.class));
    }

    /**
     * Gets the header.
     *
     * @return the header
     */
    public Map<String, Object> header() {
        return header;
    }

    /**
     * Gets the payload.
     *
     * @return the payload
     */
    public Map<String, Object> payload() {
        return payload;
    }

    /**
     * Gets the signature.
     *
     * @return the signature
     */
    @SuppressWarnings("PMD.MethodReturnsInternalArray")
    public byte[] signature() {
        return signature;
    }

}
