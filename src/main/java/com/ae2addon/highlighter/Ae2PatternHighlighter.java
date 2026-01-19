package com.ae2addon.highlighter;

import com.ae2addon.highlighter.client.ClientSetup;
import com.ae2addon.highlighter.network.ModNetworkHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AE2 Pattern Highlighter - 在世界中高亮显示正在处理配方的样板接口
 */
@Mod(Ae2PatternHighlighter.MOD_ID)
public class Ae2PatternHighlighter {
    public static final String MOD_ID = "ae2patternhighlighter";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public Ae2PatternHighlighter() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onClientSetup);
        
        MinecraftForge.EVENT_BUS.register(this);
        
        LOGGER.info("AE2 Pattern Highlighter initialized!");
    }
    
    private void onCommonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetworkHandler.register();
            LOGGER.info("Network handler registered");
        });
    }
    
    private void onClientSetup(final FMLClientSetupEvent event) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientSetup::init);
        LOGGER.info("Client setup complete");
    }
}
