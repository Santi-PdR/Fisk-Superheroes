package com.fiskmods.heroes.pack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.data.DataRegistry;
import com.fiskmods.heroes.common.data.DataType;
import com.fiskmods.heroes.common.hero.Hero;
import com.fiskmods.heroes.common.hero.HeroIteration;
import com.fiskmods.heroes.common.hero.power.Modifier;
import com.fiskmods.heroes.common.hero.power.ModifierEntry;
import com.fiskmods.heroes.common.hero.power.ModifierRegistry;
import com.fiskmods.heroes.common.hero.power.Power;
import com.fiskmods.heroes.common.hero.power.PowerProperty;
import com.fiskmods.heroes.pack.js.JSHero;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

/**
 * Loads hero packs.
 * <p>
 * A pack is a directory (or a zip) with a {@code heropack.json} manifest, {@code data/heroes/*.js}
 * hero scripts, {@code data/powers/*.json} powers and {@code data/models/*.json} model overrides.
 * The mod's own pack ships inside the mod jar; additional packs are read from
 * {@code config/fiskheroes/heropacks}.
 */
public class HeroPackEngine
{
    public static final HeroPackEngine INSTANCE = new HeroPackEngine();

    private final List<PackSource> loadedPacks = new ArrayList<>();
    private boolean loaded;

    private HeroPackEngine()
    {
    }

    public void setup()
    {
        load();
    }

    /** Loads every pack. Can be called again to apply pack changes without restarting. */
    public void load()
    {
        Hero.REGISTRY.clear();
        Power.REGISTRY.clear();
        com.fiskmods.heroes.common.data.var.Vars.ensureRegistered();

        loadedPacks.clear();
        loadBuiltIn();
        loadExternal();
        Hero.REGISTRY.sort();
        loaded = true;

        FiskHeroes.LOGGER.info("Loaded {} hero pack(s): {} heroes, {} powers", loadedPacks.size(), Hero.REGISTRY.size(), Power.REGISTRY.size());
    }

    public boolean isLoaded()
    {
        return loaded;
    }

    /* ------------------------------------------------------------------ */

    private void loadBuiltIn()
    {
        Path root = modRoot();

        if (root == null)
        {
            FiskHeroes.LOGGER.error("Could not locate the FiskHeroes mod file; no built-in heroes will be available");
            return;
        }

        try
        {
            if (Files.isDirectory(root))
            {
                LoadedPack pack = loadDirectory(root, "fiskheroes");
                loadedPacks.add(pack.source);
            }
            else
            {
                LoadedPack pack = loadZip(root, "fiskheroes");
                loadedPacks.add(pack.source);
            }
        }
        catch (Exception e)
        {
            FiskHeroes.LOGGER.error("Failed to load the built-in hero pack", e);
        }
    }

    private void loadExternal()
    {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("fiskheroes/heropacks");

        if (!Files.isDirectory(dir))
        {
            return;
        }

        try (var stream = Files.list(dir))
        {
            for (Path path : stream.sorted().toList())
            {
                try
                {
                    if (Files.isDirectory(path))
                    {
                        loadedPacks.add(loadDirectory(path, path.getFileName().toString()).source);
                    }
                    else if (path.getFileName().toString().endsWith(".zip"))
                    {
                        loadedPacks.add(loadZip(path, path.getFileName().toString()).source);
                    }
                }
                catch (Exception e)
                {
                    FiskHeroes.LOGGER.error("Failed to load hero pack {}", path, e);
                }
            }
        }
        catch (IOException e)
        {
            FiskHeroes.LOGGER.error("Could not read the hero pack directory", e);
        }
    }

    private static Path modRoot()
    {
        try
        {
            return net.minecraftforge.fml.ModList.get().getModFileById(FiskHeroes.MODID).getFile().getFilePath();
        }
        catch (Exception e)
        {
            try
            {
                return Path.of(FiskHeroes.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            }
            catch (Exception ignored)
            {
                return null;
            }
        }
    }

    /* ------------------------------------------------------------------ */

    private record PackSource(String name, int heroes, int powers)
    {
    }

    private record LoadedPack(PackSource source)
    {
    }

    private LoadedPack loadDirectory(Path root, String name)
    {
        Map<String, String> files = new LinkedHashMap<>();

        try (var stream = Files.walk(root))
        {
            for (Path path : stream.filter(Files::isRegularFile).toList())
            {
                String relative = root.relativize(path).toString().replace('\\', '/');

                if (relative.endsWith(".json") || relative.endsWith(".js"))
                {
                    files.put(relative, Files.readString(path, StandardCharsets.UTF_8));
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not read hero pack " + root, e);
        }

        return process(name, files);
    }

    private LoadedPack loadZip(Path zip, String name)
    {
        Map<String, String> files = new LinkedHashMap<>();

        try (ZipFile file = new ZipFile(zip.toFile()))
        {
            var entries = file.entries();

            while (entries.hasMoreElements())
            {
                ZipEntry entry = entries.nextElement();
                String entryName = entry.getName();

                if (!entry.isDirectory() && (entryName.endsWith(".json") || entryName.endsWith(".js")))
                {
                    try (InputStream in = file.getInputStream(entry))
                    {
                        files.put(entryName, new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    }
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException("Could not read hero pack " + zip, e);
        }

        return process(name, files);
    }

    /** Processes the manifest, powers and hero scripts of one pack. */
    private LoadedPack process(String name, Map<String, String> files)
    {
        String manifest = files.get("heropack.json");

        if (manifest == null)
        {
            throw new IllegalStateException("Hero pack " + name + " has no heropack.json");
        }

        JsonObject json = JsonParser.parseString(manifest).getAsJsonObject();
        String domain = json.has("domain") ? json.get("domain").getAsString() : name;

        // 1. data variables
        if (json.has("dataVars"))
        {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("dataVars").entrySet())
            {
                boolean reset = false;
                String typeName;

                if (e.getValue().isJsonObject())
                {
                    JsonObject object = e.getValue().getAsJsonObject();
                    typeName = object.get("type").getAsString();
                    reset = object.has("resetWithoutSuit") && object.get("resetWithoutSuit").getAsBoolean();
                }
                else
                {
                    typeName = e.getValue().getAsString();
                }

                DataType<?> type = DataType.byName(typeName);

                if (type != null)
                {
                    DataRegistry.INSTANCE.register(new ResourceLocation(domain, e.getKey()), type, reset);
                }
            }
        }

        // 2. iterations ("alts")
        Map<String, Map<String, HeroIteration.Candidate>> alts = new LinkedHashMap<>();

        if (json.has("alts"))
        {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("alts").entrySet())
            {
                Map<String, HeroIteration.Candidate> map = new LinkedHashMap<>();

                for (Map.Entry<String, JsonElement> alt : e.getValue().getAsJsonObject().entrySet())
                {
                    HeroIteration.Candidate candidate = new HeroIteration.Candidate();

                    if (alt.getValue().isJsonPrimitive())
                    {
                        candidate.name = alt.getValue().getAsString();
                    }
                    else
                    {
                        JsonObject object = alt.getValue().getAsJsonObject();
                        candidate.name = object.has("name") ? object.get("name").getAsString() : null;
                        candidate.soundProfile = object.has("soundProfile") ? object.get("soundProfile").getAsString() : null;
                        candidate.vanity = object.has("vanity") ? object.get("vanity").getAsString() : null;
                        candidate.domain = object.has("domain") ? object.get("domain").getAsString() : null;
                        candidate.maskToggleTicks = object.has("maskToggleTicks") ? object.get("maskToggleTicks").getAsInt() : 0;
                        candidate.disableMask = object.has("disableMask") && object.get("disableMask").getAsBoolean();

                        if (object.has("armor"))
                        {
                            Map<String, String> armor = new LinkedHashMap<>();

                            for (Map.Entry<String, JsonElement> piece : object.getAsJsonObject("armor").entrySet())
                            {
                                armor.put(piece.getKey(), piece.getValue().getAsString());
                            }

                            candidate.armor = armor;
                        }
                    }

                    map.put(alt.getKey(), candidate);
                }

                alts.put(e.getKey(), map);
            }
        }

        // 3. powers
        int powerCount = 0;

        for (Map.Entry<String, String> e : files.entrySet())
        {
            if (!e.getKey().startsWith("data/powers/") || !e.getKey().endsWith(".json"))
            {
                continue;
            }

            String id = e.getKey().substring("data/powers/".length(), e.getKey().length() - ".json".length());
            loadPower(new ResourceLocation(domain, id), JsonParser.parseString(e.getValue()).getAsJsonObject());
            powerCount++;
        }

        // 4. heroes (scripts)
        int heroCount = 0;
        List<String> scripts = new ArrayList<>();
        List<String> helperScripts = new ArrayList<>();

        for (Map.Entry<String, String> e : files.entrySet())
        {
            if (!e.getKey().endsWith(".js"))
            {
                continue;
            }

            if (e.getKey().startsWith("data/heroes/external/"))
            {
                helperScripts.add(e.getValue());
            }
            else if (e.getKey().startsWith("data/heroes/"))
            {
                scripts.add(e.getKey() + "\u0000" + e.getValue());
            }
        }

        for (String entry : scripts)
        {
            int index = entry.indexOf('\u0000');
            String path = entry.substring(0, index);
            String source = entry.substring(index + 1);
            String id = path.substring("data/heroes/".length(), path.length() - ".js".length());

            try
            {
                loadHero(new ResourceLocation(domain, id), source, helperScripts, alts.getOrDefault(domain + ":" + id, alts.get(id)));
                heroCount++;
            }
            catch (Exception ex)
            {
                FiskHeroes.LOGGER.error("Failed to load hero {}", id, ex);
            }
        }

        return new LoadedPack(new PackSource(name, heroCount, powerCount));
    }

    private void loadPower(ResourceLocation id, JsonObject json)
    {
        String nameKey = json.has("name") ? json.get("name").getAsString() : null;
        Power power = new Power(id, nameKey, json.get("hud"));

        if (json.has("modifiers"))
        {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("modifiers").entrySet())
            {
                ResourceLocation modifierId = ResourceLocation.tryParse(e.getKey());

                if (modifierId == null)
                {
                    continue;
                }

                Modifier modifier = ModifierRegistry.INSTANCE.get(modifierId);

                if (modifier == null)
                {
                    FiskHeroes.LOGGER.warn("Power {} references unknown modifier {}", id, modifierId);
                    continue;
                }

                ModifierEntry entry = new ModifierEntry(modifier, e.getKey());

                if (e.getValue().isJsonObject())
                {
                    for (Map.Entry<String, JsonElement> property : e.getValue().getAsJsonObject().entrySet())
                    {
                        PowerProperty<?> key = PowerProperty.byName(property.getKey());

                        if (key != null)
                        {
                            entry.setProperty(key, key.parse(property.getValue()));
                        }
                    }
                }

                power.addEntry(e.getKey(), entry);
            }
        }

        Power.REGISTRY.register(power);
    }

    private void loadHero(ResourceLocation id, String source, List<String> helperScripts, Map<String, HeroIteration.Candidate> candidates)
    {
        Hero hero = new Hero(id);
        Context cx = com.fiskmods.heroes.pack.js.JSContext.enter();

        try
        {
            Scriptable scope = com.fiskmods.heroes.pack.js.JSContext.createScope(cx);

            // Helpers shared by several heroes (speedster_base, firestorm_base, ...)
            for (String helper : helperScripts)
            {
                cx.evaluateString(scope, helper, id + ":helpers", 1, null);
            }

            cx.evaluateString(scope, source, id.toString(), 1, null);

            Object init = scope.get("init", scope);

            if (init instanceof Function function)
            {
                function.call(cx, scope, scope, new Object[] { new JSHero(hero, scope) });
            }

            Hero.REGISTRY.register(id, hero, candidates);
        }
        finally
        {
            Context.exit();
        }
    }
}
