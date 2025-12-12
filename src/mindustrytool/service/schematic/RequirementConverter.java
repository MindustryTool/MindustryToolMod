package mindustrytool.service.schematic;

import arc.struct.Seq;
import mindustry.type.*;
import mindustry.ctype.ContentType;
import mindustry.Vars;
import mindustrytool.core.model.SchematicDetailData.SchematicRequirement;

public class RequirementConverter {
    public static ItemSeq toItemSeq(Seq<SchematicRequirement> requirements) {
        ItemSeq result = new ItemSeq();
        if (requirements == null) return result;
        for (SchematicRequirement req : requirements) {
            if (req == null || req.name() == null || req.amount() == null) continue;
            Item item = (Item) Vars.content.getByName(ContentType.item, req.name());
            if (item != null) result.add(item, req.amount());
        }
        return result;
    }
}
