/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg.core.hooks;

import me.angeschossen.lands.api.integration.LandsIntegration;
import xzot1k.plugins.ptg.PhysicsToGo;

public class LandsHook {

    private LandsIntegration landsIntegration;

    public LandsHook(PhysicsToGo pluginInstance) {
        try {
            java.lang.reflect.Method ofMethod = LandsIntegration.class.getMethod("of", org.bukkit.plugin.Plugin.class);
            setLandsIntegration((LandsIntegration) ofMethod.invoke(null, pluginInstance));
        } catch (Throwable t) {
            try {
                setLandsIntegration(new LandsIntegration(pluginInstance));
            } catch (Throwable ignored) {}
        }
    }

    public LandsIntegration getLandsIntegration() {
        return landsIntegration;
    }

    private void setLandsIntegration(LandsIntegration landsIntegration) {
        this.landsIntegration = landsIntegration;
    }
}