/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg.core.hooks;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;

public class WorldGuardHook {

    private static com.sk89q.worldguard.protection.flags.StateFlag PTG_ALLOW;
    private static StateFlag EXPLOSION_FLAG;
    private final com.sk89q.worldguard.bukkit.WorldGuardPlugin worldGuardPlugin;

    public WorldGuardHook() {
        com.sk89q.worldguard.protection.flags.registry.FlagRegistry registry = null;
        worldGuardPlugin = com.sk89q.worldguard.bukkit.WorldGuardPlugin.inst();
        if (worldGuardPlugin.getDescription().getVersion().startsWith("6")) {
            try {
                Method method = worldGuardPlugin.getClass().getMethod("getFlagRegistry");
                registry = (com.sk89q.worldguard.protection.flags.registry.FlagRegistry) method.invoke(worldGuardPlugin);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } else registry = com.sk89q.worldguard.WorldGuard.getInstance().getFlagRegistry();

        if (registry == null) return;
        try {
            com.sk89q.worldguard.protection.flags.StateFlag flag = new com.sk89q.worldguard.protection.flags.StateFlag("ptg-allow", false);
            StateFlag explosionFlag = new StateFlag("ptg-explosion",false);
            registry.register(flag);
            registry.register(explosionFlag);
            PTG_ALLOW = flag;
            EXPLOSION_FLAG=explosionFlag;
        } catch (com.sk89q.worldguard.protection.flags.registry.FlagConflictException e) {
            com.sk89q.worldguard.protection.flags.Flag<?> existing = registry.get("ptg-allow");
            Flag<?> explosion = registry.get("ptg-explosion");
            if (existing instanceof com.sk89q.worldguard.protection.flags.StateFlag)
                PTG_ALLOW = (com.sk89q.worldguard.protection.flags.StateFlag) existing;
            if(explosion instanceof StateFlag){
                EXPLOSION_FLAG=(StateFlag) explosion;
            }
        }
    }

    public boolean handleExplosion(EntityExplodeEvent e){
        if(e.isCancelled())
            return false;
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        if (e.getLocation().getWorld() == null)
            return false;
        com.sk89q.worldedit.util.Location weLoc = BukkitAdapter.adapt(e.getLocation());
        RegionManager manager = container.get(BukkitAdapter.adapt(e.getLocation().getWorld()));
        if(manager==null)
            return false;
        ApplicableRegionSet set = manager.getApplicableRegions(weLoc.toVector().toBlockPoint());
        HashMap<Integer, Boolean> priorities = new HashMap<>();
        for(ProtectedRegion region:set.getRegions()){
            StateFlag.State object = region.getFlag(EXPLOSION_FLAG);
            if(object==null)
                object= StateFlag.State.DENY;
            if(priorities.containsKey(region.getPriority()))
                continue;
            if(region.contains(weLoc.toVector().toBlockPoint())){
                priorities.put(region.getPriority(),object== StateFlag.State.DENY);
            }
        }

        int highestPriority = priorities.keySet().stream().max(Integer::compareTo).orElse(-125);
        if(highestPriority!=-125&& priorities.get(highestPriority)){
            e.blockList().clear();
            return true;
        }
        return false;
    }

    public boolean passedWorldGuardHook(Location location) {
        if (worldGuardPlugin == null) return true;

        com.sk89q.worldguard.protection.ApplicableRegionSet applicableRegionSet = null;
        if (worldGuardPlugin.getDescription().getVersion().startsWith("6")) {
            try {
                Method method = worldGuardPlugin.getClass().getMethod("getRegionManager", World.class);

                com.sk89q.worldguard.protection.managers.RegionManager regionManager =
                        (com.sk89q.worldguard.protection.managers.RegionManager) method.invoke(worldGuardPlugin, location.getWorld());
                if (regionManager == null) return true;

                Method applicableRegionsMethod = worldGuardPlugin.getClass().getMethod("getApplicableRegions", Location.class);
                applicableRegionSet = (com.sk89q.worldguard.protection.ApplicableRegionSet) applicableRegionsMethod.invoke(regionManager, location);
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            com.sk89q.worldguard.protection.regions.RegionQuery query =
                    com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
            com.sk89q.worldedit.util.Location worldEditLocation = com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(location);
            applicableRegionSet = query.getApplicableRegions(worldEditLocation);
        }

        if (applicableRegionSet == null) return true;

        Set<com.sk89q.worldguard.protection.regions.ProtectedRegion> regions = applicableRegionSet.getRegions();
        if (regions.isEmpty()) return true;

        for (com.sk89q.worldguard.protection.regions.ProtectedRegion protectedRegion : regions) {
            Object ptgFlag = protectedRegion.getFlags().getOrDefault(PTG_ALLOW, null);
            if (ptgFlag instanceof Boolean && !((Boolean) ptgFlag)) return false;
        }

        return true;
    }
}