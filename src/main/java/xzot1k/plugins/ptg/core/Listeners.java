/*
 * Copyright (c) XZot1K $year. All rights reserved.
 */

package xzot1k.plugins.ptg.core;

import me.devtec.shared.Ref;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import xzot1k.plugins.ptg.PhysicsToGo;
import xzot1k.plugins.ptg.core.enums.ActionType;
import xzot1k.plugins.ptg.core.objects.LocationClone;
import xzot1k.plugins.ptg.core.objects.Pair;
import xzot1k.plugins.ptg.core.tasks.BlockRegenerationTask;
import xzot1k.plugins.ptg.core.tasks.TreePhysicsTask;
import xzot1k.plugins.ptg.events.PhysicsActionEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class Listeners implements Listener {

    private PhysicsToGo pluginInstance;

    public Listeners(PhysicsToGo pluginInstance) {
        setPluginInstance(pluginInstance);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDecay(LeavesDecayEvent e) {
        if (getPluginInstance().getAdvancedConfig().getBoolean("cancel-decay-radius")) {
            if (getPluginInstance().doesNotPassHooksCheck(e.getBlock().getLocation())) return;

            final boolean invertedBlockedWorlds = getPluginInstance().getConfig().getBoolean("invert-wb");
            if ((!invertedBlockedWorlds && getPluginInstance().getManager().isBlockedWorld(e.getBlock().getWorld()))
                    || (invertedBlockedWorlds && !getPluginInstance().getManager().isBlockedWorld(e.getBlock().getWorld())))
                return;

            for (BlockState blockState : getPluginInstance().getManager().getSavedBlockStates())
                if (blockState.getWorld().getName().equals(e.getBlock().getWorld().getName()) && blockState.getLocation().distance(e.getBlock().getLocation()) < 5) {
                    e.setCancelled(true);
                    break;
                }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFall(BlockPhysicsEvent e) {
        if (!getPluginInstance().getAdvancedConfig().getBoolean("cancel-fall-radius", true))
            return;

        if (getPluginInstance().doesNotPassHooksCheck(e.getBlock().getLocation()))
            return;

        World world = e.getBlock().getWorld();
        Location location = e.getSourceBlock().getLocation();

        boolean isWorldBlocked = getPluginInstance().getManager().isBlockedWorld(world);
        boolean invert = getPluginInstance().getConfig().getBoolean("invert-wb", false);
        if ((!invert && isWorldBlocked) || (invert && !isWorldBlocked))
            return;

        for (BlockState state : getPluginInstance().getManager().getSavedBlockStates()) {
            if (state.getWorld() != world)
                continue;

            if (state.getLocation().distanceSquared(location) < 25) {
                e.setCancelled(true);
                return;
            }
        }

        /*
        if (getPluginInstance().getAdvancedConfig().getBoolean("cancel-fall-radius")) {
            if (getPluginInstance().doesNotPassHooksCheck(e.getBlock().getLocation())) return;

            final boolean invertedBlockedWorlds = getPluginInstance().getConfig().getBoolean("invert-wb");
            if ((!invertedBlockedWorlds && getPluginInstance().getManager().isBlockedWorld(e.getBlock().getWorld()))
                    || (invertedBlockedWorlds && !getPluginInstance().getManager().isBlockedWorld(e.getBlock().getWorld())))
                return;

            try {
                for (BlockState blockState : getPluginInstance().getManager().getSavedBlockStates())
                    if (blockState.getWorld().getName().equals(e.getSourceBlock().getWorld().getName())
                            && blockState.getLocation().distance(e.getSourceBlock().getLocation()) < 5) {
                        e.setCancelled(true);
                        break;
                    }
            } catch (Exception ignored) {}
        }*/
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFlow(BlockFromToEvent e) {
        if ((e.getBlock().getType().name().contains("WATER") || e.getBlock().getType().name().contains("LAVA"))
                && getPluginInstance().getAdvancedConfig().getBoolean("cancel-flow-radius")) {
            if (getPluginInstance().doesNotPassHooksCheck(e.getBlock().getLocation())) return;

            final boolean invertedBlockedWorlds = getPluginInstance().getConfig().getBoolean("invert-wb");
            if ((!invertedBlockedWorlds && getPluginInstance().getManager().isBlockedWorld(e.getBlock().getWorld()))
                    || (invertedBlockedWorlds && !getPluginInstance().getManager().isBlockedWorld(e.getBlock().getWorld())))
                return;

            for (BlockState blockState : getPluginInstance().getManager().getSavedBlockStates())
                if (blockState.getWorld().getName().equals(e.getToBlock().getWorld().getName())
                        && blockState.getLocation().distance(e.getToBlock().getLocation()) < 5) {
                    e.setCancelled(true);
                    break;
                }
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onForm(EntityChangeBlockEvent e) {
        if (getPluginInstance().doesNotPassHooksCheck(e.getBlock().getLocation())) return;

        final boolean invertedBlockedWorlds = getPluginInstance().getConfig().getBoolean("invert-wb");
        if ((!invertedBlockedWorlds && getPluginInstance().getManager().isBlockedWorld(e.getBlock().getWorld()))
                || (invertedBlockedWorlds && !getPluginInstance().getManager().isBlockedWorld(e.getBlock().getWorld())))
            return;

        if (e.getEntity().getType() == EntityType.FALLING_BLOCK && (e.getEntity().getCustomName() != null
                && e.getEntity().getCustomName().toUpperCase().contains("PTG_FALLING_BLOCK"))
                && !getPluginInstance().getConfig().getBoolean("block-forming")) {
            e.setCancelled(true);

            if (getPluginInstance().getManager().isBlockDataVersion())
                e.getEntity().getWorld().spawnParticle(Ref.isNewerThan(20)?((Particle)Ref.getStatic(Particle.class,"BLOCK")):Particle.BLOCK_CRACK, e.getEntity().getLocation(), 10, e.getBlock().getBlockData());
            else
                e.getEntity().getWorld().playEffect(e.getEntity().getLocation(), Effect.STEP_SOUND, e.getBlock().getType().getId());
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent e) {
        if (e.isCancelled() || getPluginInstance().doesNotPassHooksCheck(e.getBlock().getLocation())) return;

        final boolean invertedBlockedWorlds = getPluginInstance().getConfig().getBoolean("invert-wb");
        if ((!invertedBlockedWorlds && getPluginInstance().getManager().isBlockedWorld(e.getBlock().getWorld()))
                || (invertedBlockedWorlds && !getPluginInstance().getManager().isBlockedWorld(e.getBlock().getWorld())))
            return;

        if (getPluginInstance().getConfig().getBoolean("place-removal")) {
            final BlockState blockState = e.getBlockReplacedState();
            if (checkState(blockState, ActionType.PLACE) || isSaved(blockState)) return;

            if (!getPluginInstance().getManager().isWhitelistedPlaceMaterial(e.getBlock().getType())
                    || getPluginInstance().getManager().isAvoidedMaterial(e.getBlock().getType(), e.getBlock().getData()))
                return;

            if (getPluginInstance().getCoreProtectHook() != null && getPluginInstance().getConfig().getBoolean("core-protect"))
                getPluginInstance().getCoreProtectHook().logLocation(e.getBlockReplacedState().getLocation()); // log to core protect.

            getPluginInstance().getManager().getSavedBlockStates().add(blockState);
            getPluginInstance().getServer().getScheduler().runTaskLater(getPluginInstance(),
                    new BlockRegenerationTask(getPluginInstance(), e.getBlockReplacedState().getBlock(), blockState, true),
                    getPluginInstance().getConfig().getInt("place-removal-delay"));
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBreak(BlockBreakEvent e) {
        if (e.isCancelled() || getPluginInstance().doesNotPassHooksCheck(e.getBlock().getLocation())) return;

        final boolean invertedBlockedWorlds = getPluginInstance().getConfig().getBoolean("invert-wb");
        if ((!invertedBlockedWorlds && getPluginInstance().getManager().isBlockedWorld(e.getBlock().getWorld()))
                || (invertedBlockedWorlds && !getPluginInstance().getManager().isBlockedWorld(e.getBlock().getWorld())))
            return;

        boolean skipToBreakAction = false;
        if (e.getBlock().getType().name().contains("LOG") && getPluginInstance().getConfig().getBoolean("tree-physics")) {
            if (getPluginInstance().getManager().isNotTreeBlock(e.getBlock().getRelative(BlockFace.UP)))
                skipToBreakAction = true;

            if (!skipToBreakAction) {
                for (BlockFace blockFace : new BlockFace[]{BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST}) {
                    Block relativeBlock = e.getBlock().getRelative(blockFace);
                    if (!relativeBlock.getType().name().contains("AIR") && getPluginInstance().getManager().isNotTreeBlock(relativeBlock))
                        return;
                }

                final BlockState blockState = e.getBlock().getState();
                final boolean drops = getPluginInstance().getConfig().getBoolean("tree-drops"),
                        treeRegeneration = getPluginInstance().getConfig().getBoolean("tree-regeneration");
                if (checkState(blockState, ActionType.TREE_BREAK)) return;

                e.setCancelled(true);
                TreePhysicsTask treePhysicsTask = new TreePhysicsTask(getPluginInstance(), e.getPlayer(), blockState, drops, treeRegeneration);
                treePhysicsTask.setTaskId(getPluginInstance().getServer().getScheduler().runTaskTimer(getPluginInstance(), treePhysicsTask, 0,
                        getPluginInstance().getConfig().getInt("tree-regeneration" +
                                "-speed")).getTaskId());
                return;
            }
        }

        if (getPluginInstance().getConfig().getBoolean("break-regeneration")) {
            final BlockState blockState = e.getBlock().getState();
            if (checkState(blockState, ActionType.BREAK) || isSaved(blockState)) return;

            final Pair<Boolean, Integer> wbmPair = getPluginInstance().getManager().isWhitelistedBreakMaterial(blockState.getType());
            if (wbmPair != null && (!wbmPair.getKey() || getPluginInstance().getManager().isAvoidedMaterial(blockState.getType(), blockState.getRawData())))
                return;

            if (getPluginInstance().getCoreProtectHook() != null && getPluginInstance().getConfig().getBoolean("core-protect"))
                getPluginInstance().getCoreProtectHook().logLocation(blockState.getLocation());


            getPluginInstance().getManager().getSavedBlockStates().add(blockState);

            if (!getPluginInstance().getConfig().getBoolean("break-drops")) {
                e.setCancelled(true);
                getPluginInstance().getManager().playNaturalBlockBreakEffect(e.getBlock());
                e.getBlock().setType(Material.AIR);
            }

            final boolean inverted = getPluginInstance().getConfig().getBoolean("invert-bmr");
            if ((!inverted && getPluginInstance().getManager().isBlockedRegenMaterial(blockState.getType()))
                    || (inverted && !getPluginInstance().getManager().isBlockedRegenMaterial(blockState.getType())))
                return;

            final boolean containerRestore = getPluginInstance().getConfig().getBoolean("container-restoration"),
                    signRestore = getPluginInstance().getConfig().getBoolean("sign-restoration");
            handleSpecialStateRestore(e.getBlock(), blockState, containerRestore, signRestore);

            if (wbmPair != null) getPluginInstance().getServer().getScheduler().runTaskLater(getPluginInstance(),
                    new BlockRegenerationTask(getPluginInstance(), e.getBlock(), blockState, false), wbmPair.getValue());
            else getPluginInstance().getServer().getScheduler().runTaskLater(getPluginInstance(),
                    new BlockRegenerationTask(getPluginInstance(), e.getBlock(), blockState, false),
                    getPluginInstance().getConfig().getInt("break-regeneration-delay"));
        }
    }

    @EventHandler
    public void onFireStart(BlockIgniteEvent e){
        for(BlockState state:new HashSet<>(getPluginInstance().getManager().getSavedBlockStates())){
            if(state.getLocation().equals(e.getBlock().getLocation())){
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockExplode(BlockExplodeEvent e) {
        if (e.isCancelled()) return;

        final boolean invertedBlockedWorlds = getPluginInstance().getConfig().getBoolean("invert-wb");
        if ((!invertedBlockedWorlds && getPluginInstance().getManager().isBlockedWorld(e.getBlock().getWorld()))
                || (invertedBlockedWorlds && !getPluginInstance().getManager().isBlockedWorld(e.getBlock().getWorld())))
            return;

        if (getPluginInstance().getConfig().getBoolean("no-block-explosions")) {
            e.blockList().clear();
            return;
        }
        Block block;
        Material type;
        if(Ref.isNewerThan(20)){
            Object blockState = Ref.invoke(e, "getExplodedBlockState");
            block=(Block) Ref.invoke(blockState,"getBlock");
            type=(Material) Ref.invoke(blockState,"getType");
        }else {
            block=e.getBlock();
            type=block.getType();
        }

        e.setYield(handleExplosives(e.blockList(), e.getYield(), null, block,type));
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onExplode(EntityExplodeEvent e) {
        if (e.isCancelled()) return;

        final boolean invertedBlockedWorlds = getPluginInstance().getConfig().getBoolean("invert-wb");
        if ((!invertedBlockedWorlds && getPluginInstance().getManager().isBlockedWorld(e.getLocation().getWorld()))
                || (invertedBlockedWorlds && !getPluginInstance().getManager().isBlockedWorld(e.getLocation().getWorld())))
            return;

        if (getPluginInstance().getConfig().getBoolean("no-entity-explosions")) {
            e.blockList().clear();
            return;
        }

        if(getPluginInstance().getWorldGuardHook().handleExplosion(e)){
            return;
        }

        e.setYield(handleExplosives(e.blockList(), e.getYield(), e.getEntity(), null,null));
    }

    @SuppressWarnings("deprecation")
    private float handleExplosives(List<Block> blocklist, float ogYield, Entity entity, Block causeBlock,Material causeBlockType) {
        boolean yield = false,
                isBlockedEntity = (causeBlock == null && entity != null && getPluginInstance().getManager().isBlockedExplosiveRegenEntity(entity.getType())),
                isBlockedBlock = (entity == null && causeBlock != null && getPluginInstance().getManager().isBlockedExplosiveRegenBlock(causeBlockType));
        blocklist.removeIf(block -> getPluginInstance().getManager().isAvoidedMaterial(block.getType(), block.getData()));

        final List<Block> blockList = new ArrayList<>(blocklist);
        int delay = getPluginInstance().getConfig().getInt("explosive-regeneration-delay"),
                speed = getPluginInstance().getConfig().getInt("explosive-regeneration-speed");
        final boolean explosiveDrops = getPluginInstance().getConfig().getBoolean("explosive-drops"),
                physics = getPluginInstance().getConfig().getBoolean("explosive-physics"),
                regen = getPluginInstance().getConfig().getBoolean("explosive-regeneration"),
                containerRestore = getPluginInstance().getConfig().getBoolean("container-restoration"),
                signRestore = getPluginInstance().getConfig().getBoolean("sign-restoration"),
                ignite = getPluginInstance().getConfig().getBoolean("explosive-tnt-ignite"),
                restoreContainerEmptyAndDropItems = getPluginInstance().getConfig().getBoolean("restore-container-empty-but-drop-items");
        getPluginInstance().getManager().sortFromLowestToHighest(blockList);

        int fallingBlockCount = 0;
        for (int i = -1; ++i < blockList.size(); ) {
            final Block block = blockList.get(i);
            if (block == null || getPluginInstance().doesNotPassHooksCheck(block.getLocation())) {
                continue;
            }

            BlockState blockState = block.getState();
            boolean isInventoryHolder = block.getState() instanceof InventoryHolder;

            if (checkState(isInventoryHolder ? block.getState() : blockState, ActionType.EXPLOSIVE)
                    || getPluginInstance().getManager().isAvoidedMaterial(block.getType(), block.getData())) {
                continue;
            }
            getPluginInstance().getManager().playNaturalBlockBreakEffect(block); // play special effect

            if (ignite && block.getType().name().contains("TNT")) {
                blocklist.remove(i);
                block.setType(Material.AIR);

                TNTPrimed primed = block.getWorld().spawn(block.getLocation().add(0.0D, 1.0D, 0.0D), TNTPrimed.class);
                primed.setFuseTicks(getPluginInstance().getConfig().getInt("explosive-tnt-fuse"));
                continue;
            }


            if (physics && ((Math.random() * 100) < 15) && fallingBlockCount < (blockList.size() * 0.25)) {
                getPluginInstance().getManager().createFallingBlock(block, isInventoryHolder ? block.getState() : blockState, true, false);
                fallingBlockCount++;
            }
            BlockState beforeClear = null;
            if (!explosiveDrops) {
                if (isInventoryHolder) {
                    beforeClear = block.getState().copy();
                    ((InventoryHolder) block.getState()).getInventory().clear();
                    if (restoreContainerEmptyAndDropItems && !containerRestore) {
                        Inventory holder = ((InventoryHolder) beforeClear).getInventory();
                        for (ItemStack item : holder.getContents()) {
                            if (item != null && item.getType() != Material.AIR) {
                                block.getLocation().getWorld().dropItem(block.getLocation(), item);
                            }
                        }
                    }
                }
                yield = true;
            }


            if (!regen || isBlockedEntity || isBlockedBlock) {
                continue;
            }

            final boolean inverted = getPluginInstance().getConfig().getBoolean("invert-bmr");
            if ((!inverted && getPluginInstance().getManager().isBlockedRegenMaterial(block.getType()))
                    || (inverted && !getPluginInstance().getManager().isBlockedRegenMaterial(block.getType()))) {
                continue;
            }


            handleSpecialStateRestore(block, isInventoryHolder ? beforeClear : blockState, containerRestore, signRestore);
            getPluginInstance().getManager().getSavedBlockStates().add(isInventoryHolder ? block.getState() : blockState);
            getPluginInstance().getServer().getScheduler().runTaskLater(getPluginInstance(),
                    new BlockRegenerationTask(getPluginInstance(), block, isInventoryHolder ? block.getState() : blockState, false), delay);
            delay += speed;
        }

        return (yield ? 0 : ogYield);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeath(EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION && getPluginInstance().getManager().isBlockedExplosiveEntity(e.getEntity().getType())) {

            final boolean invertedBlockedWorlds = getPluginInstance().getConfig().getBoolean("invert-wb");
            if ((!invertedBlockedWorlds && getPluginInstance().getManager().isBlockedWorld(e.getEntity().getWorld()))
                    || (invertedBlockedWorlds && !getPluginInstance().getManager().isBlockedWorld(e.getEntity().getWorld())))
                return;

            if (getPluginInstance().doesNotPassHooksCheck(e.getEntity().getLocation())) return;
            e.setCancelled(true);
        }
    }

    // helper methods
    private void handleSpecialStateRestore(Block block, BlockState blockState, boolean containerRestore, boolean signRestore) {
        if (blockState instanceof InventoryHolder && containerRestore) {
            if (!getPluginInstance().getManager().isDoubleChestPartnerStored(block)) {
                InventoryHolder ih = (InventoryHolder) blockState;
                ItemStack[] clonedContents = new ItemStack[ih.getInventory().getSize()];
                for (int j = -1; ++j < clonedContents.length; ) {
                    ItemStack item = ih.getInventory().getItem(j);
                    if (item == null || item.getType() == Material.AIR) continue;
                    clonedContents[j] = item.clone();
                }

                getPluginInstance().getManager().getSavedContainerContents().put(new LocationClone(getPluginInstance(), block.getLocation()), clonedContents);
            }

        } else if (blockState instanceof Sign && signRestore) {
            Sign sign = (Sign) blockState;

            if (Ref.isNewerThan(19)) {
                List<Pair<org.bukkit.block.sign.Side, org.bukkit.block.sign.SignSide>> signSides = new ArrayList<Pair<org.bukkit.block.sign.Side,
                        org.bukkit.block.sign.SignSide>>() {{
                    for (int j = -1; ++j < org.bukkit.block.sign.Side.values().length; ) {
                        org.bukkit.block.sign.Side side = org.bukkit.block.sign.Side.values()[j];
                        add(new Pair<>(side, sign.getSide(side)));
                    }
                }};

                getPluginInstance().getManager().getSavedSignData().put(new LocationClone(getPluginInstance(), block.getLocation()), signSides);
            } else
                getPluginInstance().getManager().getSavedSignData().put(new LocationClone(getPluginInstance(), block.getLocation()), sign.getLines());
        }
    }

    private boolean isSame(BlockState blockStateOne, BlockState blockStateTwo) {
        return (blockStateOne.getWorld().getName().equals(blockStateTwo.getWorld().getName())
                && blockStateOne.getX() == blockStateTwo.getX() && blockStateOne.getY() == blockStateTwo.getY()
                && blockStateOne.getZ() == blockStateTwo.getZ());
    }

    private boolean isSaved(BlockState blockState) {
        return getPluginInstance().getManager().getSavedBlockStates().parallelStream().anyMatch(state -> isSame(blockState, state));
    }

    private boolean checkState(BlockState blockState, ActionType actionType) {
        final boolean stateOverride = getPluginInstance().getConfig().getBoolean("state-override");

        List<BlockState> states = getPluginInstance().getManager().getSavedBlockStates().parallelStream()
                .filter(state -> isSame(blockState, state)).collect(Collectors.toList());
        if (!states.isEmpty() && stateOverride) return false;

        PhysicsActionEvent actionEvent = new PhysicsActionEvent(getPluginInstance(), blockState, actionType);
        getPluginInstance().getServer().getPluginManager().callEvent(actionEvent);
        return actionEvent.isCancelled();
    }

    // getters & setters
    private PhysicsToGo getPluginInstance() {
        return pluginInstance;
    }

    private void setPluginInstance(PhysicsToGo pluginInstance) {
        this.pluginInstance = pluginInstance;
    }
}