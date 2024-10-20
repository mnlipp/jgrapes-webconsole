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
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public 
 * License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.webconsole.base;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
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
@JsonDeserialize(using = JsonRpc.Deserializer.class)
@JsonSerialize(using = JsonRpc.Serializer.class)
public class JsonRpc {

    @SuppressWarnings("PMD.FieldNamingConventions")
    private static final String JSONRPC_VERSION = "2.0";
    private static Map<Class<? extends JsonRpc>,
            Map<String, List<Type>>> paramTypes = new ConcurrentHashMap<>();
    private JsonNode id;
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
    public Optional<JsonNode> id() {
        return Optional.ofNullable(id);
    }

    /**
     * Sets the id.
     *
     * @param id the id
     * @return the json rpc
     */
    public JsonRpc setId(Number id) {
        this.id = JsonNodeFactory.instance
            .numberNode(new BigDecimal(id.toString()));
        return this;
    }

    /**
     * Sets the id.
     *
     * @param id the id
     * @return the json rpc
     */
    public JsonRpc setId(String id) {
        this.id = JsonNodeFactory.instance.textNode(id);
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
     * Returns the parameter's value as the requested type.
     *
     * @param <T> the generic type
     * @param index the index
     * @return the string
     */
    @SuppressWarnings({ "unchecked", "PMD.AvoidDuplicateLiterals" })
    public <T> T param(int index) {
        return (T) params.get(index);
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

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "JsonRpc [method=" + method + ", params=" + params + "]";
    }

    /**
     * A Serializer for a {@link JsonRpc}.
     */
    @SuppressWarnings("serial")
    public static class Serializer extends StdSerializer<JsonRpc> {

        /**
         * Instantiates a new serializer.
         */
        public Serializer() {
            super((Class<JsonRpc>) null);
        }

        /**
         * Instantiates a new serializer.
         *
         * @param type the type
         */
        public Serializer(Class<JsonRpc> type) {
            super(type);
        }

        /**
         * Serialize.
         *
         * @param obj the obj
         * @param generator the generator
         * @param ctx the ctx
         * @throws IOException Signals that an I/O exception has occurred.
         */
        @Override
        public void serialize(JsonRpc obj, JsonGenerator generator,
                SerializerProvider ctx) throws IOException {
            generator.writeStartObject();
            generator.writeFieldName("jsonrpc");
            generator.writeString(JSONRPC_VERSION);
            generator.writeFieldName("method");
            generator.writeString(obj.method);
            generator.writeFieldName("params");
            ctx.defaultSerializeValue(obj.params, generator);
            generator.writeEndObject();
        }
    }

    /**
     * A Deserializer for a {@link JsonRpc}.
     */
    public static class Deserializer extends JsonDeserializer<JsonRpc>
            implements ContextualDeserializer {

        private JavaType type;

        /**
         * Creates a new instance for calling
         * {@link #createContextual(DeserializationContext, BeanProperty)},
         * which is the real deserializer.
         */
        public Deserializer() {
            // Default constructor
        }

        /**
         * Instantiates a new deserializer for the given type.
         *
         * @param type the type
         */
        public Deserializer(JavaType type) {
            this.type = type;
        }

        /**
         * Creates the contextual.
         *
         * @param deserializationContext the deserialization context
         * @param beanProperty the bean property
         * @return the json deserializer
         * @throws JsonMappingException the json mapping exception
         */
        @Override
        public JsonDeserializer<?> createContextual(
                DeserializationContext deserializationContext,
                BeanProperty beanProperty) throws JsonMappingException {
            // beanProperty is null when the type to deserialize is the
            // top-level type or a generic type, not a type of a bean property
            JavaType type = deserializationContext.getContextualType() != null
                ? deserializationContext.getContextualType()
                : beanProperty.getMember().getType();
            return new Deserializer(type);
        }

        /**
         * Deserialize.
         *
         * @param parser the parser
         * @param ctx the ctx
         * @return the json rpc
         * @throws IOException Signals that an I/O exception has occurred.
         */
        @Override
        public JsonRpc deserialize(JsonParser parser,
                DeserializationContext ctx) throws IOException {
            JsonRpc jsonRpc;
            @SuppressWarnings("unchecked")
            var clazz = (Class<? extends JsonRpc>) type.getRawClass();
            try {
                jsonRpc = clazz.getDeclaredConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException
                    | IllegalArgumentException | InvocationTargetException
                    | NoSuchMethodException | SecurityException e) {
                throw new IOException("Failed to create "
                    + type.getTypeName(), e);
            }
            while (true) {
                var token = parser.nextToken();
                if (token == null) {
                    return null;
                }
                if (token == JsonToken.END_OBJECT) {
                    return jsonRpc;
                }
                if (token == JsonToken.FIELD_NAME) {
                    handleField(parser, ctx, jsonRpc);
                }
            }
        }

        private void handleField(JsonParser parser, DeserializationContext ctx,
                JsonRpc jsonRpc) throws IOException {
            var key = parser.getValueAsString();
            parser.nextToken();
            switch (key) {
            case "id":
                jsonRpc.id = ctx.readTree(parser);
                break;
            case "method":
                jsonRpc.method = ctx.readValue(parser, String.class);
                break;
            case "params":
                jsonRpc.params = handleParams(parser, ctx, jsonRpc);
                break;
            default:
                ctx.readTree(parser);
                break;
            }
        }

        private List<Object> handleParams(JsonParser parser,
                DeserializationContext ctx, JsonRpc jsonRpc)
                throws IOException {
            List<Object> params = new ArrayList<>();
            @SuppressWarnings("unchecked")
            var clazz = (Class<? extends JsonRpc>) type.getRawClass();
            var typeIter = Optional.ofNullable(paramTypes.get(clazz))
                .map(m -> m.get(jsonRpc.method))
                .map(List::iterator).orElse(Collections.emptyIterator());

            // Process array elements
            while (true) {
                if (parser.nextToken() == JsonToken.END_ARRAY) {
                    break;
                }
                Type paramType = typeIter.hasNext() ? typeIter.next()
                    : ctx.getTypeFactory().constructType(Object.class);
                var item = ctx.readValue(parser,
                    ctx.getTypeFactory().constructType(paramType));
                params.add(item);
            }
            return params;
        }

    }
}