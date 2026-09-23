package com.example.sableboats;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SableBoats.MODID)
public class SableBoats {
    public static final String MODID = "sableboats";
    public static final Logger LOGGER = LoggerFactory.getLogger("SableBoats");

    public SableBoats(IEventBus modEventBus) {
        LOGGER.info("Sable Boats initialized with Sable physics integration.");
    }
}
