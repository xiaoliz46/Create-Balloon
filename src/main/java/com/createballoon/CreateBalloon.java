package com.createballoon;

import com.createballoon.block.ControlConsoleBlock;
import com.createballoon.block.ControlConsoleBlockEntity;
import com.createballoon.block.HotAirBalloonBlock;
import com.createballoon.block.HotAirBalloonBlockEntity;
import com.createballoon.client.ControlInputHandler;
import com.createballoon.client.MovementBlocker;
import com.createballoon.network.ControlInputPacket;
import com.createballoon.network.ControlSyncPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
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

    public CreateBalloon(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, ModConfigs.SPEC);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        modBus.addListener(this::registerCreativeTab);
        modBus.addListener(CreateBalloon::onRegisterPayloads);
        modBus.addListener(CreateBalloon::onClientSetup);
    }

    private void registerCreativeTab(RegisterEvent event) {
        if (event.getRegistryKey() == Registries.CREATIVE_MODE_TAB) {
            event.register(Registries.CREATIVE_MODE_TAB,
                    helper -> helper.register(id("tab"), CreativeModeTab.builder()
                            .title(Component.translatable("itemGroup.create_balloon"))
                            .icon(() -> new ItemStack(HOT_AIR_BALLOON_ITEM.get()))
                            .displayItems((params, output) -> {
                                output.accept(HOT_AIR_BALLOON_ITEM.get());
                                output.accept(CONTROL_CONSOLE_ITEM.get());
                            })
                            .withTabsBefore(CreativeModeTabs.SPAWN_EGGS)
                            .build()));
        }
    }

    private static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar("1");
        ControlInputPacket.register(r);
        ControlSyncPacket.register(r);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ControlInputHandler.init();
            MovementBlocker.init();
        });
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
                event.accept(CONTROL_CONSOLE_ITEM.get());
            }
        }
    }
}
