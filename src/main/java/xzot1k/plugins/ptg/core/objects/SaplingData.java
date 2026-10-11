/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg.core.objects;

import org.bukkit.Material;
import xzot1k.plugins.ptg.core.enums.TreeType;

public class SaplingData {
    private byte dataValue;
    private Material material;

    public SaplingData(TreeType treeType) {

        Material material = Material.getMaterial(treeType.name() + "_SAPLING");
        if (material == null) {
            if (treeType == TreeType.MANGROVE) material = Material.getMaterial("MANGROVE_PROPAGULE");
            else if (treeType == TreeType.CRIMSON) material = Material.getMaterial("CRIMSON_FUNGUS");
            else if (treeType == TreeType.WARPED) material = Material.getMaterial("WARPED_FUNGUS");
            if (material == null) material = Material.getMaterial("SAPLING");
            if (material == null) material = Material.OAK_SAPLING;
        }
        setMaterial(material);

        setDataValue((getMaterial() != null && !getMaterial().name().startsWith(treeType.name())) ? treeType.getDataValue() : -1);
    }

    public Material getMaterial() {
        return material;
    }

    private void setMaterial(Material material) {
        this.material = material;
    }

    public byte getDataValue() {
        return dataValue;
    }

    private void setDataValue(byte dataValue) {
        this.dataValue = dataValue;
    }
}
