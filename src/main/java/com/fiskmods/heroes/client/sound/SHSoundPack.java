package com.fiskmods.heroes.client.sound;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.fiskmods.heroes.FiskHeroes;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.IoSupplier;

/**
 * Mounts the downloaded sound repository as a resource pack.
 * <p>
 * The pack is served straight from {@code gameDir/fiskheroes_sounds} (see
 * {@link SHSoundRepository}); it is added as a required pack so the sounds are always available,
 * which is what the original mod's downloaded-audio behaviour amounts to.
 */
public final class SHSoundPack implements PackResources
{
    public static final String PACK_ID = "fiskheroes_sounds";
    private static final Set<String> NAMESPACES = Set.of(FiskHeroes.MODID);
    private static final Set<String> RESOURCE_DOMAINS = Set.of("minecraft", FiskHeroes.MODID);

    private final Path root;

    private SHSoundPack(Path root)
    {
        this.root = root;
    }

    /** Registers the pack finder. Called from the client setup (mod bus). */
    public static void register(Consumer<RepositorySource> registerSource)
    {
        registerSource.accept(SHSoundPack::findPacks);
    }

    public static void refresh()
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc != null)
        {
            mc.reloadResourcePacks();
        }
    }

    private static void findPacks(Consumer<Pack> output)
    {
        // Resource pack discovery runs before Minecraft reads sounds.json. Refresh the generated
        // index here as well as during client setup, otherwise a cached pack is first loaded with
        // its stale file and only repaired after the initial resource reload.
        SHSoundRepository.loadCached();
        Path root = SHSoundRepository.root().resolve("pack");

        if (!Files.isDirectory(root.resolve("assets/fiskheroes/sounds")))
        {
            return;
        }

        Pack pack = Pack.readMetaAndCreate(PACK_ID, Component.literal("Fisk's Superheroes Sounds"), true,
                id -> new SHSoundPack(root), PackType.CLIENT_RESOURCES, Pack.Position.TOP, PackSource.BUILT_IN);

        if (pack != null)
        {
            output.accept(pack);
        }
        else
        {
            FiskHeroes.LOGGER.warn("Could not read the sound pack metadata; suit sounds will be silent");
        }
    }

    /* --- PackResources --- */

    @Override
    public IoSupplier<InputStream> getRootResource(String... path)
    {
        return supplier(resolve(path));
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location)
    {
        if (type != PackType.CLIENT_RESOURCES || !RESOURCE_DOMAINS.contains(location.getNamespace()))
        {
            return null;
        }

        return supplier(root.resolve("assets").resolve(location.getNamespace()).resolve(location.getPath()));
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output)
    {
        if (type != PackType.CLIENT_RESOURCES || !RESOURCE_DOMAINS.contains(namespace))
        {
            return;
        }

        Path base = root.resolve("assets").resolve(namespace);

        Path directory = base.resolve(path);
        if (!Files.isDirectory(directory))
        {
            return;
        }

        try (Stream<Path> stream = Files.walk(directory))
        {
            stream.filter(Files::isRegularFile).forEach(file ->
            {
                String relative = base.relativize(file).toString().replace('\\', '/');
                output.accept(new ResourceLocation(namespace, relative), supplier(file));
            });
        }
        catch (IOException e)
        {
            FiskHeroes.LOGGER.warn("Could not list resources of the sound pack", e);
        }
    }

    @Override
    public Set<String> getNamespaces(PackType type)
    {
        return type == PackType.CLIENT_RESOURCES ? NAMESPACES : Set.of();
    }

    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws IOException
    {
        Path meta = root.resolve("pack.mcmeta");

        if (!Files.isRegularFile(meta))
        {
            return null;
        }

        try (InputStream in = Files.newInputStream(meta))
        {
            JsonObject json = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();

            String name = serializer.getMetadataSectionName();
            if (!json.has(name) || !json.get(name).isJsonObject()) return null;
            return serializer.fromJson(json.getAsJsonObject(name));
        }
    }

    @Override
    public String packId()
    {
        return PACK_ID;
    }

    @Override
    public void close()
    {
    }

    /* --- Helpers --- */

    private Path resolve(String... path)
    {
        Path result = root;

        for (String part : path)
        {
            result = result.resolve(part);
        }

        return result;
    }

    private static IoSupplier<InputStream> supplier(Path path)
    {
        if (!Files.isRegularFile(path))
        {
            return null;
        }

        return () -> Files.newInputStream(path);
    }
}
