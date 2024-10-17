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

import jakarta.json.Json;
import jakarta.json.JsonValue;
import jakarta.json.bind.JsonbException;
import jakarta.json.bind.annotation.JsonbTypeDeserializer;
import jakarta.json.bind.annotation.JsonbTypeSerializer;
import jakarta.json.bind.serializer.DeserializationContext;
import jakarta.json.bind.serializer.JsonbDeserializer;
import jakarta.json.bind.serializer.JsonbSerializer;
import jakarta.json.bind.serializer.SerializationContext;
import jakarta.json.stream.JsonGenerator;
import jakarta.json.stream.JsonParser;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * A class that serializes/deserializes as a JSON RPC.
 */
@JsonbTypeSerializer(JsonRpc.Serializer.class)
@JsonbTypeDeserializer(JsonRpc.Deserializer.class)
public class JsonRpc {

    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final String JSONRPC_VERSION = "2.0";
    private static Map<Class<? extends JsonRpc>,
            Map<String, List<Type>>> paramTypes = new ConcurrentHashMap<>();
    private JsonValue id;
    private String method;
    private List<Object> params = new ArrayList<>();

    /**
     * Instantiates a new json rpc.
     */
    public JsonRpc() {
        // Default constructor
    }

    /**
     * Instantiates a new json rpc.
     *
     * @param method the method
     */
    public JsonRpc(String method) {
        this.method = method;
    }

    /**
     * Specify the types of the parameters for specific methods.
     * Used for deserialization.
     *
     * @param clazz the clazz
     * @param types the param types
     */
    public static void setParamTypes(Class<? extends JsonRpc> clazz,
            Map<String, List<Type>> types) {
        paramTypes.put(clazz, new HashMap<>(types));
    }

    /**
     * Adds the parameter types for the given method.
     * Used for deserialization.
     *
     * @param clazz the clazz
     * @param method the method
     * @param types the types
     */
    public static void addParamTypes(Class<? extends JsonRpc> clazz,
            String method, List<Type> types) {
        paramTypes.computeIfAbsent(clazz, k -> new HashMap<>())
            .put(method, types);
    }

    /**
     * An optional request id.
     * 
     * @return the id
     */
    @SuppressWarnings("PMD.ShortMethodName")
    public Optional<JsonValue> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Sets the id.
     *
     * @param id the id
     * @return the json rpc
     */
    public JsonRpc setId(Number id) {
        this.id = Json.createValue(new BigDecimal(id.toString()));
        return this;
    }

    /**
     * Sets the id.
     *
     * @param id the id
     * @return the json rpc
     */
    public JsonRpc setId(String id) {
        this.id = Json.createValue(id);
        return this;
    }

    /**
     * The invoked method.
     * 
     * @return the method
     */
    public String method() {
        return method;
    }

    /**
     * The parameters.
     * 
     * @return the params
     */
    public Object[] params() {
        return params.toArray();
    }

    /**
     * Sets the params.
     *
     * @param params the params
     * @return the json rpc
     */
    @SuppressWarnings("PMD.LinguisticNaming")
    public JsonRpc setParams(Object... params) {
        this.params = new ArrayList<>(Arrays.asList(params));
        return this;
    }

    /**
     * Adds the param.
     *
     * @param param the param
     * @return the json rpc
     */
    public JsonRpc addParam(Object param) {
        params.add(param);
        return this;
    }

    /**
     * Returns the parameter's value as string.
     *
     * @param index the index
     * @return the string
     */
    public String asString(int index) {
        return params.get(index).toString();
    }

    /**
     * Returns the parameter's value as string.
     *
     * @param index the index
     * @return the string
     */
    @SuppressWarnings("PMD.ShortMethodName")
    public <T> T as(Class<T> cls, int index) {
        return cls.cast(params.get(index));
    }

    /**
     * Returns the parameter's value as stream.
     *
     * @param <T> the generic type
     * @param cls the class of the stream elements
     * @param index the index of the parameter
     * @return the stream
     */
    @SuppressWarnings("unchecked")
    public <T> Stream<T> streamOf(Class<T> cls, int index) {
        return Arrays.stream((T[]) params.get(index));
    }

    @Override
    public String toString() {
        return "JsonRpc [method=" + method + ", params=" + params + "]";
    }

    /**
     * A Serializer for a {@link JsonRpc}.
     */
    public static class Serializer implements JsonbSerializer<JsonRpc> {

        /**
         * Serialize.
         *
         * @param obj the obj
         * @param generator the generator
         * @param ctx the ctx
         */
        @Override
        public void serialize(JsonRpc obj, JsonGenerator generator,
                SerializationContext ctx) {
            ctx.serialize("jsonrpc", JSONRPC_VERSION, generator);
            ctx.serialize("method", obj.method, generator);
            ctx.serialize("params", obj.params, generator);
        }
    }

    /**
     * A Deserializer for a {@link JsonRpc}.
     */
    public static class Deserializer implements JsonbDeserializer<JsonRpc> {

        /**
         * Deserialize.
         *
         * @param parser the parser
         * @param ctx the ctx
         * @param rtType the rt type
         * @return the json rpc
         */
        @Override
        @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
        public JsonRpc deserialize(JsonParser parser,
                DeserializationContext ctx, Type rtType) {
            JsonRpc jsonRpc = null;
            while (parser.hasNext()) {
                var event = parser.next();
                if (event == JsonParser.Event.START_OBJECT) {
                    try {
                        @SuppressWarnings("unchecked")
                        var clazz = (Class<? extends JsonRpc>) rtType;
                        jsonRpc = clazz.getDeclaredConstructor().newInstance();
                    } catch (InstantiationException | IllegalAccessException
                            | IllegalArgumentException
                            | InvocationTargetException | NoSuchMethodException
                            | SecurityException e) {
                        throw new JsonbException("Failed to create "
                            + rtType.getTypeName(), e);
                    }
                } else if (event == JsonParser.Event.KEY_NAME) {
                    handleField(parser, ctx, rtType, jsonRpc);
                } else if (event == JsonParser.Event.END_OBJECT) {
                    break;
                }
            }
            return jsonRpc;
        }

        private void handleField(JsonParser parser, DeserializationContext ctx,
                Type rtType, JsonRpc jsonRpc) {
            var key = parser.getString();
            switch (key) {
            case "id":
                parser.next();
                jsonRpc.id = parser.getValue();
                break;
            case "method":
                parser.next();
                jsonRpc.method = parser.getString();
                break;
            case "params":
                jsonRpc.params = handleParams(parser, ctx, rtType, jsonRpc);
                break;
            default:
                ctx.deserialize(Object.class, parser);
                break;
            }
        }

        private List<Object> handleParams(JsonParser parser,
                DeserializationContext ctx, Type rtType, JsonRpc jsonRpc) {
            List<Object> params = new ArrayList<>();
            var typeIter = Optional.ofNullable(paramTypes.get(rtType))
                .map(m -> m.get(jsonRpc.method))
                .map(List::iterator).orElse(Collections.emptyIterator());

            // Skip start Array
            parser.next();
            while (true) {
                Type paramType
                    = typeIter.hasNext() ? typeIter.next() : Object.class;
                var item = ctx.deserialize(paramType, parser);
                if (item.equals(JsonValue.EMPTY_JSON_ARRAY)) {
                    break;
                }
                params.add(item);
            }
            return params;
        }
    }
}