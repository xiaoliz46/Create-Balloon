package com.createballoon;

import com.createballoon.block.ControlConsoleBlock;
import com.createballoon.block.ControlConsoleBlockEntity;
import com.createballoon.block.GyroscopeBlock;
import com.createballoon.block.GyroscopeBlockEntity;
import com.createballoon.block.GyroController;
import com.createballoon.block.HotAirBalloonBlock;
import com.createballoon.block.HotAirBalloonBlockEntity;
import com.createballoon.block.InflatedWoolBlock;
import com.createballoon.block.InflatedWoolBlockEntity;
import com.createballoon.cc.BalloonCCCompat;
import com.createballoon.client.ControlInputHandler;
import com.createballoon.client.MovementBlocker;
import com.createballoon.item.ColoredBalloonBlockItem;
import com.createballoon.item.ColoredInflatedWoolBlockItem;
import com.createballoon.network.ControlInputPacket;
import com.createballoon.network.ControlSyncPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Mod(CreateBalloon.ID)
public final class CreateBalloon {
    public static final String ID = "create_balloon";

    private static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, ID);
    private static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);

    public static final Supplier<HotAirBalloonBlock> HOT_AIR_BALLOON =
            BLOCKS.register("hot_air_balloon", () -> new HotAirBalloonBlock(
                    BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.WOOL)));

    public static final Supplier<BlockItem> HOT_AIR_BALLOON_ITEM =
            ITEMS.register("hot_air_balloon",
                    () -> new BlockItem(HOT_AIR_BALLOON.get(), new Item.Properties()));

    public static final Supplier<BlockEntityType<HotAirBalloonBlockEntity>> HOT_AIR_BALLOON_ENTITY =
            BLOCK_ENTITY_TYPES.register("hot_air_balloon",
                    () -> BlockEntityType.Builder.of(
                            HotAirBalloonBlockEntity::new, HOT_AIR_BALLOON.get()).build(null));

    public static final List<Supplier<BlockItem>> COLORED_BALLOON_ITEMS = new ArrayList<>();

    static {
        for (DyeColor color : DyeColor.values()) {
            String name = "hot_air_balloon_" + color.getSerializedName();
            Supplier<BlockItem> item = ITEMS.register(name,
                    () -> new ColoredBalloonBlockItem(HOT_AIR_BALLOON.get(), color,
                            new Item.Properties()));
            COLORED_BALLOON_ITEMS.add(item);
        }
    }

    public static final Supplier<ControlConsoleBlock> CONTROL_CONSOLE =
            BLOCKS.register("control_console", () -> new ControlConsoleBlock(
                    BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.METAL)));

    public static final Supplier<BlockItem> CONTROL_CONSOLE_ITEM =
            ITEMS.register("control_console",
                    () -> new BlockItem(CONTROL_CONSOLE.get(), new Item.Properties()));

    public static final Supplier<BlockEntityType<ControlConsoleBlockEntity>> CONTROL_CONSOLE_ENTITY =
            BLOCK_ENTITY_TYPES.register("control_console",
                    () -> BlockEntityType.Builder.of(
                            ControlConsoleBlockEntity::new, CONTROL_CONSOLE.get()).build(null));

    public static final Supplier<GyroscopeBlock> GYROSCOPE =
            BLOCKS.register("gyroscope", () -> new GyroscopeBlock(
                    BlockBehaviour.Properties.of().strength(2.0F).sound(SoundType.STONE)));

    // Gyroscope item removed from creative — integrated into control console.
    // Block + entity type kept for backward compatibility with existing worlds.

    public static final Supplier<BlockEntityType<GyroscopeBlockEntity>> GYROSCOPE_ENTITY =
            BLOCK_ENTITY_TYPES.register("gyroscope",
                    () -> BlockEntityType.Builder.of(
                            GyroscopeBlockEntity::new, GYROSCOPE.get()).build(null));

    public static final Supplier<InflatedWoolBlock> INFLATED_WOOL =
            BLOCKS.register("inflated_wool", () -> new InflatedWoolBlock(
                    BlockBehaviour.Properties.of().strength(0.5F).sound(SoundType.WOOL)));
    public static final Supplier<BlockItem> INFLATED_WOOL_ITEM =
            ITEMS.register("inflated_wool", () -> new BlockItem(INFLATED_WOOL.get(), new Item.Properties()));
    public static final List<Supplier<BlockItem>> COLORED_INFLATED_WOOL_ITEMS = new ArrayList<>();

    static {
        for (DyeColor color : DyeColor.values()) {
            String name = "inflated_wool_" + color.getSerializedName();
            Supplier<BlockItem> item = ITEMS.register(name,
                    () -> new ColoredInflatedWoolBlockItem(INFLATED_WOOL.get(), color));
            COLORED_INFLATED_WOOL_ITEMS.add(item);
        }
    }
    public static final Supplier<BlockEntityType<InflatedWoolBlockEntity>> INFLATED_WOOL_ENTITY =
            BLOCK_ENTITY_TYPES.register("inflated_wool",
                    () -> BlockEntityType.Builder.of(
                            InflatedWoolBlockEntity::new, INFLATED_WOOL.get()).build(null));

    public CreateBalloon(IEventBus modBus, ModContainer container) {
        DebugLog.init();
        container.registerConfig(ModConfig.Type.COMMON, ModConfigs.SPEC);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        modBus.addListener(this::registerCreativeTab);
        modBus.addListener(CreateBalloon::onRegisterPayloads);
        modBus.addListener(CreateBalloon::onClientSetup);
        modBus.addListener(com.createballoon.client.ConfigKeyHandler::onRegisterKeys);
        // GyroController cleanup on server lifecycle
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
            (net.neoforged.neoforge.event.server.ServerStoppingEvent e) -> GyroController.clearAll());
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
            (net.neoforged.neoforge.event.server.ServerStartingEvent e) -> GyroController.clearAll());
        // Read Sable's aerodynamic drag each physics step so the balloon can cancel it (game event bus).
        try {
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                    com.createballoon.block.BalloonDragCanceller::onPostPhysics);
        } catch (Throwable ignored) {}
        try {
            if (net.neoforged.fml.loading.FMLLoader.getLoadingModList().getModFileById("computercraft") != null) {
                BalloonCCCompat.register(modBus);
            }
        } catch (Throwable ignored) {}
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        ControlInputPacket.register(r);
        ControlSyncPacket.register(r);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ControlInputHandler.init();
            com.createballoon.client.ConfigKeyHandler.init();
            MovementBlocker.init();
        });
    }

    private void registerCreativeTab(RegisterEvent event) {
        if (event.getRegistryKey() == Registries.CREATIVE_MODE_TAB) {
            event.register(Registries.CREATIVE_MODE_TAB,
                    helper -> helper.register(id("tab"), CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.create_balloon"))
                            .icon(() -> new ItemStack(HOT_AIR_BALLOON_ITEM.get()))
                            .displayItems((params, output) -> {
                                for (var s : COLORED_BALLOON_ITEMS) output.accept(s.get());
                                output.accept(CONTROL_CONSOLE_ITEM.get());
                                for (var s : COLORED_INFLATED_WOOL_ITEMS) output.accept(s.get());
                            })
                            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                            .build()));
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    @EventBusSubscriber(modid = ID)
    public static final class TabHandler {
        @SubscribeEvent
        static void addToTabs(BuildCreativeModeTabContentsEvent event) {
            ResourceLocation key = event.getTabKey().location();
            if (key.getNamespace().equals("aeronautics")) {
                event.accept(HOT_AIR_BALLOON_ITEM.get());
                for (var s : COLORED_BALLOON_ITEMS) event.accept(s.get());
                event.accept(CONTROL_CONSOLE_ITEM.get());
            }
        }
    }
}
