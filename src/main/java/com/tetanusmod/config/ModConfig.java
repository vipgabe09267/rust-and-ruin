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
    public float mobConversionChance = 0.01F;
    public float golemConversionChance = 0.18F;
    public float animalConversionChance = 0.02F;
    public float witherConversionChance = 0.05F;
    public float salmonellaChance = 1.0F;
    public int vaccineDurationSeconds=1200;
    public float vaccineProtectionChance=0.80F;
    public float gloveProtectionChance=0.50F;
    // Natural mutations are separate from infection caused by weapons.
    public float naturalAnimalEasy = 0.005F;
    public float naturalAnimalNormal = 0.01F;
    public float naturalAnimalHard = 0.02F;
    public float naturalMobEasy = 0.005F;
    public float naturalMobNormal = 0.01F;
    public float naturalMobHard = 0.02F;
    public float spawnerEasy = 0.03F;
    public float spawnerNormal = 0.05F;
    public float spawnerHard = 0.08F;
    public int coatingWeatherTicks = 12000;
    public float naturalRabiesEasy = 0.003F;
    public float naturalRabiesNormal = 0.006F;
    public float naturalRabiesHard = 0.01F;
    public int balanceRevision = 5;

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
        if(revision<5) {
            if(config.mobConversionChance==0.05F)config.mobConversionChance=0.01F;
            if(config.animalConversionChance==0.15F)config.animalConversionChance=0.02F;
            if(config.salmonellaChance==0.10F)config.salmonellaChance=1;
            if(config.naturalAnimalEasy==0.10F)config.naturalAnimalEasy=0.005F;
            if(config.naturalAnimalNormal==0.15F)config.naturalAnimalNormal=0.01F;
            if(config.naturalAnimalHard==0.20F)config.naturalAnimalHard=0.02F;
            if(config.naturalMobEasy==0.10F)config.naturalMobEasy=0.005F;
            if(config.naturalMobNormal==0.15F)config.naturalMobNormal=0.01F;
            if(config.naturalMobHard==0.20F)config.naturalMobHard=0.02F;
            if(config.spawnerEasy==0.25F)config.spawnerEasy=0.03F;
            if(config.spawnerNormal==0.35F)config.spawnerNormal=0.05F;
            if(config.spawnerHard==0.45F)config.spawnerHard=0.08F;
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
        mobConversionChance = finite(mobConversionChance, 0.01F, 1);
        golemConversionChance = finite(golemConversionChance, 0.18F, 1);
        animalConversionChance = finite(animalConversionChance, 0.02F, 1);
        witherConversionChance = finite(witherConversionChance, 0.05F, 1);
        salmonellaChance=finite(salmonellaChance,1F,1);
        vaccineDurationSeconds=clamp(vaccineDurationSeconds,1,7200);
        vaccineProtectionChance=finite(vaccineProtectionChance,0.80F,1);
        gloveProtectionChance=finite(gloveProtectionChance,0.50F,1);
        naturalAnimalEasy=finite(naturalAnimalEasy,0.005F,1);
        naturalAnimalNormal=finite(naturalAnimalNormal,0.01F,1);
        naturalAnimalHard=finite(naturalAnimalHard,0.02F,1);
        naturalMobEasy=finite(naturalMobEasy,0.005F,1);
        naturalMobNormal=finite(naturalMobNormal,0.01F,1);
        naturalMobHard=finite(naturalMobHard,0.02F,1);
        spawnerEasy=finite(spawnerEasy,0.03F,1);
        spawnerNormal=finite(spawnerNormal,0.05F,1);
        spawnerHard=finite(spawnerHard,0.08F,1);
        coatingWeatherTicks=clamp(coatingWeatherTicks,20,72000);
        naturalRabiesEasy=finite(naturalRabiesEasy,0.003F,1);
        naturalRabiesNormal=finite(naturalRabiesNormal,0.006F,1);
        naturalRabiesHard=finite(naturalRabiesHard,0.01F,1);
        balanceRevision=5;
    }
    private static float finite(float value, float fallback, float max) { return Float.isFinite(value) ? Math.max(0,Math.min(max,value)) : fallback; }

    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
}
