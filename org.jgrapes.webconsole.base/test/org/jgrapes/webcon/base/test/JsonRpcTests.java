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

import jakarta.json.bind.JsonbBuilder;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import org.jgrapes.webconsole.base.JsonRpc;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class JsonRpcTests {

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
    public void testCall1() throws URISyntaxException {
        String json = "{\"jsonrpc\":\"2.0\",\"method\":\"call1\","
            + "\"params\":[1,\"hello\",{\"value\":42}],\"id\":1}";
        var rpc = JsonbBuilder.create().fromJson(json, TestJsonRpc.class);
        assertEquals(Integer.class, rpc.params()[0].getClass());
        assertEquals(String.class, rpc.params()[1].getClass());
        assertEquals(TestType.class, rpc.params()[2].getClass());
    }
}
