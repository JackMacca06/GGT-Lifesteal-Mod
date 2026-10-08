package io.github.jackmacca06.ggtlifesteal;

import com.mojang.serialization.Codec;
import java.util.*;
import net.fabricmc.fabric.api.attachment.v1.*;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import io.github.jackmacca06.ggtlifesteal.mixin.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VaultBlock;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultState;
import net.minecraft.world.level.levelgen.Heightmap;

public final class FuryVaultRules {
    private static int visualTick;
    private static final AttachmentType<Optional<FuryVaultState>> ACTIVE = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(Ggtlifesteal.MOD_ID, "fury_vault"),
            b -> b.initializer(Optional::empty).persistent(FuryVaultState.CODEC.optionalFieldOf("event").codec()));
    private static final AttachmentType<Boolean> BLOCKED = AttachmentRegistry.create(
            Identifier.fromNamespaceAndPath(Ggtlifesteal.MOD_ID, "fury_vault_blocked"),
            b -> b.initializer(() -> false).persistent(Codec.BOOL));
    private FuryVaultRules() {}
    private static AttachmentTarget world(MinecraftServer server) { return (AttachmentTarget) server.overworld(); }
    private static void set(MinecraftServer server, FuryVaultState state) { world(server).setAttached(ACTIVE, Optional.ofNullable(state)); }
    public static void initialize() {
        PlayerBlockBreakEvents.BEFORE.register((level, player, pos, state, entity) -> !(state.getBlock() instanceof FuryVaultBlock));
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!(level.getBlockState(hit.getBlockPos()).getBlock() instanceof FuryVaultBlock)) return InteractionResult.PASS;
            if (player instanceof ServerPlayer serverPlayer && !player.isSpectator()) claim(serverPlayer, hit.getBlockPos());
            return InteractionResult.SUCCESS;
        });
    }
    public static boolean start(MinecraftServer server, boolean manual) {
        if (world(server).getAttachedOrCreate(ACTIVE).isPresent()) return false;
        if (!manual && world(server).getAttachedOrCreate(BLOCKED)) return false;
        FuryEntry entry = FuryRules.entries(server).stream().filter(e -> e.owner().isEmpty()).findFirst().orElse(null);
        if (entry == null) return false;
        ServerLevel level = server.overworld();
        BlockPos pos = findSurface(level);
        if (pos == null) { blocked(server); return false; }
        boolean forced = level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, true);
        if (!level.setBlock(pos, Ggtlifesteal.FURY_VAULT.defaultBlockState()
                .setValue(VaultBlock.OMINOUS, true).setValue(VaultBlock.STATE, VaultState.INACTIVE), Block.UPDATE_ALL)) {
            if (forced) level.setChunkForced(pos.getX() >> 4, pos.getZ() >> 4, false);
            blocked(server); return false;
        }
        Display.TextDisplay label = label(level, pos);
        set(server, new FuryVaultState(entry.id(), pos, 12000, label.getUUID().toString(), forced));
        world(server).setAttached(BLOCKED, false);
        server.getPlayerList().broadcastSystemMessage(Component.translatable("message.ggtlifesteal.fury.spawn"), false);
        return true;
    }
    private static void blocked(MinecraftServer server) {
        world(server).setAttached(BLOCKED, true);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                player.sendSystemMessage(Component.translatable("message.ggtlifesteal.fury.no_site"));
        }
        org.slf4j.LoggerFactory.getLogger("ggtlifesteal").warn("Fury vault queued: no safe spawn site. Clear space, then run /ggtls start_fury_event.");
    }
    private static BlockPos findSurface(ServerLevel level) {
        BlockPos spawn = level.getRespawnData().pos();
        int minX = ((spawn.getX() >> 4) - 1) * 16, minZ = ((spawn.getZ() >> 4) - 1) * 16;
        List<BlockPos> water = new ArrayList<>();
        List<BlockPos> land = new ArrayList<>();
        boolean sawDrySurface = false;
        for (int x = minX; x < minX + 48; x++) for (int z = minZ; z < minZ + 48; z++) {
            // WORLD_SURFACE is the cached top-down scan including player-built structures.
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            var floor = level.getBlockState(pos.below());
            boolean wet = floor.getFluidState().is(FluidTags.WATER);
            if (!wet && !floor.isAir()) sawDrySurface = true;
            if (y >= level.getMaxY() - 2 || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
            if (wet) water.add(pos);
            else if (floor.getFluidState().isEmpty() && floor.isFaceSturdy(level, pos.below(), Direction.UP)
                    && !floor.is(Blocks.MAGMA_BLOCK) && !floor.is(Blocks.CAMPFIRE) && !floor.is(Blocks.SOUL_CAMPFIRE)) land.add(pos);
        }
        return (land.isEmpty() && !sawDrySurface ? water : land).stream()
                .min(Comparator.comparingDouble(p -> p.distSqr(spawn))).orElse(null);
    }
    private static Display.TextDisplay label(ServerLevel level, BlockPos pos) {
        var display = new Display.TextDisplay(EntityTypes.TEXT_DISPLAY, level);
        display.setPos(pos.getX() + 0.5, pos.getY() + 1.75, pos.getZ() + 0.5);
        display.addTag("ggtls_fury_timer");
        ((FuryTextAccessor) display).ggtls$flags((byte) (Display.TextDisplay.FLAG_SEE_THROUGH | Display.TextDisplay.FLAG_SHADOW));
        ((FuryTextAccessor) display).ggtls$width(500);
        ((FuryDisplayAccessor) display).ggtls$billboard(Display.BillboardConstraints.CENTER);
        ((FuryDisplayAccessor) display).ggtls$range(4.0F);
        level.addFreshEntity(display);
        return display;
    }
    public static void tick(MinecraftServer server) {
        if (server.getPlayerList().getPlayers().isEmpty()) return;
        FuryVaultState event = world(server).getAttachedOrCreate(ACTIVE).orElse(null);
        if (event == null) { start(server, false); return; }
        ServerLevel level = server.overworld();
        level.getChunkAt(event.pos());
        if (!(level.getBlockState(event.pos()).getBlock() instanceof FuryVaultBlock)) {
            cleanup(server, event); set(server, null); blocked(server); return;
        }
        event = event.tick(); set(server, event);
        if (++visualTick % 20 != 0) return;
        var entity = level.getEntity(UUID.fromString(event.label()));
        if (!(entity instanceof Display.TextDisplay)) {
            // The event chunk is forced; remove any recovered labels before replacing a missing one.
            for (var old : level.getEntitiesOfClass(Display.TextDisplay.class,
                    new net.minecraft.world.phys.AABB(event.pos()).inflate(3), e -> e.entityTags().contains("ggtls_fury_timer"))) old.discard();
            entity = label(level, event.pos());
            event = new FuryVaultState(event.id(), event.pos(), event.remaining(), entity.getUUID().toString(), event.forcedByMod());
            set(server, event);
        }
        int green = (int) Math.ceil(24.0 * event.remaining() / 12000);
        var text = Component.literal(event.remaining() == 0 ? "UNLOCKED | Right-click to claim "
                : LocatorRules.time((event.remaining() + 19) / 20) + " ")
                .withStyle(ChatFormatting.WHITE)
                .append(Component.literal("|".repeat(green)).withStyle(ChatFormatting.GREEN))
                .append(Component.literal("|".repeat(24 - green)).withStyle(ChatFormatting.RED));
        ((FuryTextAccessor) entity).ggtls$text(text);
        boolean nearby = level.players().stream().anyMatch(p -> !p.isSpectator() && p.position().distanceToSqr(
                net.minecraft.world.phys.Vec3.atCenterOf(eventPosition(server))) <= 16);
        var state = level.getBlockState(event.pos());
        level.setBlock(event.pos(), state.setValue(VaultBlock.STATE, nearby ? VaultState.ACTIVE : VaultState.INACTIVE), Block.UPDATE_ALL);
        if (level.getBlockEntity(event.pos()) instanceof VaultBlockEntity vault) {
            FuryEntry preview = new FuryEntry(event.id(), "", 0);
            vault.getSharedData().setDisplayItem(FuryRules.stack(preview));
            vault.setChanged(); level.sendBlockUpdated(event.pos(), state, level.getBlockState(event.pos()), Block.UPDATE_ALL);
        }
    }
    private static BlockPos eventPosition(MinecraftServer server) { return world(server).getAttachedOrCreate(ACTIVE).orElseThrow().pos(); }
    private static void claim(ServerPlayer player, BlockPos pos) {
        MinecraftServer server = player.level().getServer();
        FuryVaultState event = world(server).getAttachedOrCreate(ACTIVE).orElse(null);
        if (event == null || player.level() != server.overworld() || !event.pos().equals(pos) || !player.isAlive()) return;
        if (event.remaining() > 0) { FuryRules.message(player, "fury.vault_locked"); return; }
        boolean alreadyOwns = FuryRules.owned(player) != null;
        if (!alreadyOwns && FuryRules.freeSlot(player) < 0) { FuryRules.message(player, "fury.make_room"); return; }
        var entries = new ArrayList<>(FuryRules.entries(server));
        FuryEntry entry = entries.stream().filter(e -> e.id().equals(event.id()) && e.owner().isEmpty()).findFirst().orElse(null);
        if (entry == null) return;
        entries.remove(entry);
        if (!alreadyOwns) entries.add(entry.withOwner(player.getUUID().toString()));
        FuryRules.save(server, entries);
        if (alreadyOwns) FuryRules.dropMaterials(player);
        else player.getInventory().setItem(FuryRules.freeSlot(player), FuryRules.stack(entry.withOwner(player.getUUID().toString())));
        cleanup(server, event);
        levelRemove(server.overworld(), pos);
        set(server, null);
        start(server, false);
    }
    private static void levelRemove(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() instanceof FuryVaultBlock) level.removeBlock(pos, false);
    }
    private static void cleanup(MinecraftServer server, FuryVaultState event) {
        ServerLevel level = server.overworld();
        var entity = level.getEntity(UUID.fromString(event.label()));
        if (entity != null) entity.discard();
        if (event.forcedByMod()) level.setChunkForced(event.pos().getX() >> 4, event.pos().getZ() >> 4, false);
    }
}
