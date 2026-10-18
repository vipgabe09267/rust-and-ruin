package com.tetanusmod.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModConfig {
    public int minDurationSeconds = 300;
    public int maxDurationSeconds = 600;
    public int waterRustTicks = 100;
    public int rainRustTicks = 300;
    public float nailDamage = 2.0F;
    public float rebarDamage = 1.0F;
    public double spasmFrequency = 1.0;
    public float periodicDamage = 1.0F;
    public boolean cameraShaking = true;
    public boolean rainRusting = true;
    public boolean tetanusCanKill = false;
    public float infectionSoundVolume = 2.5F;
    public float mobConversionChance = 0.05F;
    public float animalConversionChance = 0.15F;
    public float witherConversionChance = 0.05F;
    public float salmonellaChance = 0.10F;
    public int balanceRevision = 4;

    public static ModConfig load() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("tetanusmod.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ModConfig config = new ModConfig();
        try {
            if (Files.exists(path)) {
                config=fromJson(Files.readString(path));
            }
            config.validate();
            Files.createDirectories(path.getParent());
            Files.writeString(path, gson.toJson(config) + System.lineSeparator());
        } catch (Exception e) {
            LoggerFactory.getLogger("tetanusmod").error("Could not read/write tetanusmod.json", e);
            config.validate();
        }
        return config;
    }

    public static ModConfig fromJson(String json) {
        var object=com.google.gson.JsonParser.parseString(json).getAsJsonObject();
        ModConfig config=new Gson().fromJson(object,ModConfig.class);
        int revision=object.has("balanceRevision")?object.get("balanceRevision").getAsInt():0;
        if(revision==0) {
            if(config.mobConversionChance==0.25F)config.mobConversionChance=0.05F;
            if(config.witherConversionChance==0.02F)config.witherConversionChance=0.05F;
        }
        if(revision<3&&config.animalConversionChance==0.02F)config.animalConversionChance=0.15F;
        if(revision<4&&config.minDurationSeconds==120&&config.maxDurationSeconds==300) {
            config.minDurationSeconds=300;config.maxDurationSeconds=600;
        }
        config.validate();return config;
    }
    private void validate() {
        minDurationSeconds = clamp(minDurationSeconds, 1, 3600);
        maxDurationSeconds = clamp(maxDurationSeconds, minDurationSeconds, 3600);
        waterRustTicks = clamp(waterRustTicks, 1, 72000);
        rainRustTicks = clamp(rainRustTicks, 1, 72000);
        nailDamage = Float.isFinite(nailDamage) ? Math.max(0, Math.min(100, nailDamage)) : 2;
        rebarDamage = Float.isFinite(rebarDamage) ? Math.max(0, Math.min(100, rebarDamage)) : 1;
        periodicDamage = Float.isFinite(periodicDamage) ? Math.max(0, Math.min(20, periodicDamage)) : 1;
        spasmFrequency = Double.isFinite(spasmFrequency) ? Math.max(0, Math.min(5, spasmFrequency)) : 1;
        infectionSoundVolume = finite(infectionSoundVolume, 2.5F, 4);
        mobConversionChance = finite(mobConversionChance, 0.05F, 1);
        animalConversionChance = finite(animalConversionChance, 0.15F, 1);
        witherConversionChance = finite(witherConversionChance, 0.05F, 1);
        salmonellaChance=finite(salmonellaChance,0.10F,1);
        balanceRevision=4;
    }
    private static float finite(float value, float fallback, float max) { return Float.isFinite(value) ? Math.max(0,Math.min(max,value)) : fallback; }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
