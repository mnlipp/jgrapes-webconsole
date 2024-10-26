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

package org.jgrapes.webcon.base.test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jgrapes.webconsole.base.JsonRpc;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

public class JsonRpcTests {

    @Test
    public void testMultiple()
            throws StreamReadException, DatabindException, IOException {
        var parser = new JsonFactory().createParser(new StringReader("{}{}"));
        ObjectMapper mapper = new ObjectMapper();
        // mapper.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        assertNotNull(mapper.readValue(parser, Object.class));
        assertNotNull(mapper.readValue(parser, Object.class));
    }

    public static class TestType {
        public int value;
    }

    public static class TestJsonRpc extends JsonRpc {

        static {
            setParamTypes(TestJsonRpc.class,
                Map.of("call1",
                    List.of(Integer.class, String.class, TestType.class)));
        }
    }

    @Test
    public void testSerialize() throws URISyntaxException, JsonMappingException,
            JsonProcessingException {
        JsonRpc rpc = new JsonRpc("call1");
        rpc.addParam(1);
        rpc.addParam("hello");
        var value = new TestType();
        value.value = 42;
        rpc.addParam(value);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        String json = mapper.writeValueAsString(rpc);
        assertEquals("{\"jsonrpc\":\"2.0\",\"method\":\"call1\","
            + "\"params\":[1,\"hello\",{\"value\":42}]}", json);
    }

    @Test
    public void testCall1() throws URISyntaxException, JsonMappingException,
            JsonProcessingException {
        String json = "{\"jsonrpc\":\"2.0\",\"method\":\"call1\","
            + "\"params\":[1,\"hello\",{\"value\":42}],\"id\":1}";
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        var rpc = mapper.readValue(json, TestJsonRpc.class);
        assertEquals(Integer.class, rpc.params()[0].getClass());
        assertEquals(String.class, rpc.params()[1].getClass());
        assertEquals(TestType.class, rpc.params()[2].getClass());
    }

    @Test
    public void testJdk8Types() throws JsonProcessingException {
        JsonRpc rpc = new JsonRpc("call1");
        rpc.addParam(1);
        rpc.addParam(Optional.of("hello"));
        var value = new TestType();
        value.value = 42;
        rpc.addParam(value);
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        String json = mapper.writeValueAsString(rpc);
        assertEquals("{\"jsonrpc\":\"2.0\",\"method\":\"call1\","
            + "\"params\":[1,\"hello\",{\"value\":42}]}", json);
    }
}
