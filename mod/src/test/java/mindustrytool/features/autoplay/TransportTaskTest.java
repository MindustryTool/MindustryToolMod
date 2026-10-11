package mindustrytool.features.autoplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.struct.Seq;
import mindustrytool.features.autoplay.tasks.TransportNode;
import mindustrytool.features.autoplay.tasks.TransportStorageSerializer;
import mindustrytool.features.autoplay.tasks.TransportTask;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.Test;

class TransportTaskTest extends MindustryTestEnv {

    @Test
    void testTransportNodeItemToggles() {
        TransportNode node = new TransportNode(10, 20);
        assertEquals(10, node.x);
        assertEquals(20, node.y);

        node.isTarget.set(true);
        assertTrue(node.isConfiguredTarget());
        assertFalse(node.isConfiguredDestination());

        node.toggleTransferItem("silicon", true);
        assertTrue(node.allowsTransfer("silicon"));
        assertFalse(node.allowsTransfer("copper"));

        node.toggleTransferItem("silicon", false);
        assertFalse(node.allowsTransfer("silicon"));

        node.isDestination.set(true);
        assertTrue(node.isConfiguredDestination());

        node.toggleReceiveItem("graphite", true);
        assertTrue(node.allowsReceive("graphite"));
    }

    @Test
    void testSerializationSingleSlot() {
        String mapKey = "testMap:100x100:5";
        Seq<TransportNode> nodes = new Seq<>();

        TransportNode n1 = new TransportNode(5, 8);
        n1.isTarget.set(true);
        n1.toggleTransferItem("lead", true);
        n1.toggleTransferItem("titanium", true);

        TransportNode n2 = new TransportNode(12, 15);
        n2.isDestination.set(true);
        n2.toggleReceiveItem("lead", true);
        n2.expanded.set(true);

        nodes.add(n1);
        nodes.add(n2);

        String serialized = TransportStorageSerializer.serialize(mapKey, nodes);
        assertNotNull(serialized);
        assertTrue(serialized.startsWith(mapKey));

        Seq<TransportNode> deserialized = TransportStorageSerializer.deserialize(serialized, mapKey);
        assertNotNull(deserialized);
        assertEquals(2, deserialized.size);

        TransportNode restored1 = deserialized.get(0);
        assertEquals(5, restored1.x);
        assertEquals(8, restored1.y);
        assertTrue(restored1.isConfiguredTarget());
        assertFalse(restored1.isConfiguredDestination());
        assertTrue(restored1.allowsTransfer("lead"));
        assertTrue(restored1.allowsTransfer("titanium"));

        TransportNode restored2 = deserialized.get(1);
        assertEquals(12, restored2.x);
        assertEquals(15, restored2.y);
        assertTrue(restored2.isConfiguredDestination());
        assertTrue(restored2.allowsReceive("lead"));
        assertTrue(Boolean.TRUE.equals(restored2.expanded.peek()));

        // Different map key should yield null
        assertNull(TransportStorageSerializer.deserialize(serialized, "otherMap:200x200:1"));
    }

    @Test
    void testTransportTaskRegistrationInAutoplay() {
        AutoplayFeature feature = new AutoplayFeature();
        try {
            assertTrue(AutoplayFeature.DEFAULT_ORDER.contains(TransportTask.ID));
            assertNotNull(feature.getTask(TransportTask.ID));
            assertTrue(feature.getTask(TransportTask.ID) instanceof TransportTask);
        } finally {
            feature.disable();
            flushEffects();
        }
    }
}
