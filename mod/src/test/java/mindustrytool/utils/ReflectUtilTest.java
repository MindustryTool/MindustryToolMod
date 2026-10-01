package mindustrytool.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReflectUtilTest {

    static class GrandParent {
        private String grandParentField = "grandparent";

        private String secretMessage() {
            return "from grandparent";
        }
    }

    static class Parent extends GrandParent {
        private String parentField = "parent";
        protected int protectedCount = 42;

        public String greet(String name) {
            return "hello " + name;
        }
    }

    static class Child extends Parent {
        private String childField = "child";
    }

    @BeforeEach
    void setUp() {
        ReflectUtil.clearCache();
    }

    @Test
    void testFindFieldDirectAndInherited() {
        // Direct field
        Field fChild = ReflectUtil.findField(Child.class, "childField");
        assertNotNull(fChild);
        assertEquals("childField", fChild.getName());

        // Inherited from Parent
        Field fParent = ReflectUtil.findField(Child.class, "parentField");
        assertNotNull(fParent);
        assertEquals("parentField", fParent.getName());

        // Inherited from GrandParent
        Field fGrand = ReflectUtil.findField(Child.class, "grandParentField");
        assertNotNull(fGrand);
        assertEquals("grandParentField", fGrand.getName());

        // Protected field
        Field fProtected = ReflectUtil.findField(Child.class, "protectedCount");
        assertNotNull(fProtected);

        // Non-existent field returns null, never throws
        Field fNone = ReflectUtil.findField(Child.class, "doesNotExist");
        assertNull(fNone);
    }

    @Test
    void testGetOrNullDirectAndInherited() {
        Child child = new Child();

        assertEquals("child", ReflectUtil.getOrNull(child, "childField"));
        assertEquals("parent", ReflectUtil.getOrNull(child, "parentField"));
        assertEquals("grandparent", ReflectUtil.getOrNull(child, "grandParentField"));
        assertEquals(42, (Integer) ReflectUtil.getOrNull(child, "protectedCount"));

        // Non-existent returns null
        assertNull(ReflectUtil.getOrNull(child, "missing"));

        // Null target returns null
        assertNull(ReflectUtil.getOrNull(null, "childField"));
        assertNull(ReflectUtil.getOrNull(child, null));
    }

    @Test
    void testSetSafeDirectAndInherited() {
        Child child = new Child();

        assertTrue(ReflectUtil.setSafe(child, "childField", "newChild"));
        assertEquals("newChild", ReflectUtil.getOrNull(child, "childField"));

        assertTrue(ReflectUtil.setSafe(child, "parentField", "newParent"));
        assertEquals("newParent", ReflectUtil.getOrNull(child, "parentField"));

        assertTrue(ReflectUtil.setSafe(child, "grandParentField", "newGrand"));
        assertEquals("newGrand", ReflectUtil.getOrNull(child, "grandParentField"));

        // Non-existent field returns false without exception
        assertFalse(ReflectUtil.setSafe(child, "missingField", "value"));

        // Null handling
        assertFalse(ReflectUtil.setSafe(null, "childField", "val"));
        assertFalse(ReflectUtil.setSafe(child, null, "val"));
    }

    @Test
    void testFindAndInvokeMethodInherited() {
        Child child = new Child();

        Method mGreet = ReflectUtil.findMethod(Child.class, "greet", String.class);
        assertNotNull(mGreet);

        String result = ReflectUtil.invokeOrNull(child, "greet", new Object[]{"world"}, String.class);
        assertEquals("hello world", result);

        String secret = ReflectUtil.invokeOrNull(child, "secretMessage", new Object[]{});
        assertEquals("from grandparent", secret);

        // Non-existent method
        assertNull(ReflectUtil.findMethod(Child.class, "nonExistentMethod"));
        assertNull(ReflectUtil.invokeOrNull(child, "nonExistentMethod", new Object[]{}));
    }

    @Test
    void testCachingPerformanceAndConsistency() {
        Child child = new Child();

        // Initial lookup populates cache
        assertEquals("parent", ReflectUtil.getOrNull(child, "parentField"));

        // Subsequent lookup should be identical and retrieved from cache
        assertEquals("parent", ReflectUtil.getOrNull(child, "parentField"));

        // Negative caching (non-existent field)
        assertNull(ReflectUtil.getOrNull(child, "noSuchField"));
        assertNull(ReflectUtil.getOrNull(child, "noSuchField"));
    }
}
