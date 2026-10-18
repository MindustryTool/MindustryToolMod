package solim.mcp.introspection;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;

public final class BindingInspector {
    private BindingInspector() {
    }

    public static ArrayNode bindings(List<ReactiveRef> refs) {
        ArrayNode out = JsonNodeFactory.instance.arrayNode();
        for (ReactiveRef ref : refs) {
            if (ref.kind == ReactiveKind.EFFECT) {
                out.add(snapshot(ref));
            }
        }
        return out;
    }

    public static ObjectNode snapshot(ReactiveRef ref) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.put("id", ref.id());
        node.put("kind", "binding");
        node.put("target", ref.sourceElement != null ? ref.sourceElement : "");
        node.put("field", ref.fieldName != null ? ref.fieldName : "");
        node.put("location", ref.location);
        SignalIntrospector.snapshot(ref, node);
        return node;
    }
}
