package org.jgrapes.webcon.base.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import org.jgrapes.core.Components;
import org.jgrapes.webconsole.base.WebConsoleUtils;
import static org.junit.Assert.*;
import org.junit.Test;

public class QueryUtilTests {

    @Test
    public void testAsMap() throws URISyntaxException {
        URI uri = new URI("/test?p1=hello&p2=world&p2=all&p3=%21");
        Map<String, List<String>> map = WebConsoleUtils.queryAsMap(uri);
        assertEquals(3, map.size());
        assertEquals("hello", map.get("p1").get(0));
        assertEquals(2, map.get("p2").size());
        assertTrue(map.get("p2").contains("world"));
        assertTrue(map.get("p2").contains("all"));
        assertEquals("!", map.get("p3").get(0));
    }

    @Test
    public void testMergeAbs() throws URISyntaxException {
        URI uri = new URI("/test/it?p2=world&p2=all");
        uri = WebConsoleUtils.mergeQuery(uri,
            Components.mapOf("p1", "hello", "p3", "!"));
        Map<String, List<String>> map = WebConsoleUtils.queryAsMap(uri);
        assertEquals("/test/it", uri.getPath());
        assertEquals(3, map.size());
        assertEquals("hello", map.get("p1").get(0));
        assertEquals(2, map.get("p2").size());
        assertTrue(map.get("p2").contains("world"));
        assertTrue(map.get("p2").contains("all"));
        assertEquals("!", map.get("p3").get(0));
    }

    @Test
    public void testMergeRel() throws URISyntaxException {
        URI uri = new URI("test/it?p2=world&p2=all");
        uri = WebConsoleUtils.mergeQuery(uri,
            Components.mapOf("p1", "hello", "p3", "!"));
        Map<String, List<String>> map = WebConsoleUtils.queryAsMap(uri);
        assertEquals("test/it", uri.getPath());
        assertEquals(3, map.size());
        assertEquals("hello", map.get("p1").get(0));
        assertEquals(2, map.get("p2").size());
        assertTrue(map.get("p2").contains("world"));
        assertTrue(map.get("p2").contains("all"));
        assertEquals("!", map.get("p3").get(0));
    }

    @Test
    public void testMergeFrag() throws URISyntaxException {
        URI uri = new URI("test/it?p2=world&p2=all#frag");
        uri = WebConsoleUtils.mergeQuery(uri,
            Components.mapOf("p1", "hello", "p3", "!"));
        Map<String, List<String>> map = WebConsoleUtils.queryAsMap(uri);
        assertEquals("test/it", uri.getPath());
        assertEquals("frag", uri.getFragment());
        assertEquals(3, map.size());
        assertEquals("hello", map.get("p1").get(0));
        assertEquals(2, map.get("p2").size());
        assertTrue(map.get("p2").contains("world"));
        assertTrue(map.get("p2").contains("all"));
        assertEquals("!", map.get("p3").get(0));
    }
}
