package com.fiskmods.heroes.client.sound;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.fiskmods.heroes.FiskHeroes;
import com.fiskmods.heroes.common.sound.SHSounds;

import net.minecraftforge.fml.loading.FMLPaths;

/**
 * The mod's sound repository.
 * <p>
 * The original release ships no audio files: it downloads them from the project's public sound
 * repository on first launch and caches them locally. This class does the same, using the
 * repository and version declared by {@code heropack.json} ({@code sounds.download}), and turns the
 * downloaded files into a resource pack the client can read.
 * <p>
 * Everything happens off the render thread and failures never stop the game: if the repository is
 * unreachable the mod simply plays nothing for the sounds that are not present.
 */
public final class SHSoundRepository
{
    public static final String PACK_DIR_NAME = "fiskheroes_sounds";
    private static final String DEFAULT_REPOSITORY = "FiskFille/Superheroes";
    private static final int DEFAULT_VERSION = 3;

    private static volatile Path root;
    private static volatile boolean downloading;
    private static volatile boolean ready;
    private static final Map<String, String> INDEX = new LinkedHashMap<>();

    private SHSoundRepository()
    {
    }

    public static Path root()
    {
        if (root == null)
        {
            root = FMLPaths.GAMEDIR.get().resolve(PACK_DIR_NAME);
        }

        return root;
    }

    public static boolean isReady()
    {
        return ready;
    }

    public static Map<String, String> index()
    {
        return INDEX;
    }

    /** Loads the cached index if the sounds have already been downloaded. */
    public static synchronized void loadCached()
    {
        Path indexFile = root().resolve("sounds.index");
        // Old port caches contain only filename-derived events; they need the archive's actual
        // sounds.json table and must be downloaded once more rather than marked ready.
        if (!Files.isRegularFile(root().resolve("sound-events.json")))
        {
            ready = false;
            return;
        }

        if (!Files.isRegularFile(indexFile))
        {
            ready = false;
            return;
        }

        try
        {
            INDEX.clear();

            for (String line : Files.readAllLines(indexFile, StandardCharsets.UTF_8))
            {
                int split = line.indexOf('=');

                if (split > 0)
                {
                    INDEX.put(line.substring(0, split), line.substring(split + 1));
                }
            }

            Path soundPackDir = root().resolve("pack");
            if (Files.isDirectory(soundPackDir))
            {
                // Rebuild the generated sounds.json from the index so upgrades repair older
                // cached packs without requiring another download.
                writeIndex(soundPackDir);
            }
            ready = !INDEX.isEmpty() && Files.isDirectory(soundPackDir);
            FiskHeroes.LOGGER.info("FiskHeroes sound repository: {} cached sounds", INDEX.size());
        }
        catch (IOException | RuntimeException e)
        {
            ready = false;
            FiskHeroes.LOGGER.warn("Could not read the cached sound index; it will be downloaded again", e);
        }
    }

    /** Downloads the repository in the background when it is not cached yet. */
    public static synchronized void downloadIfMissing(String repository, int version)
    {
        loadCached();
        if (ready || downloading) return;
        downloading = true;
        String repo = repository != null && !repository.isEmpty() ? repository : DEFAULT_REPOSITORY;
        int ver = version > 0 ? version : DEFAULT_VERSION;

        Thread thread = new Thread(() -> {
            try
            {
                download(repo, ver);
            }
            catch (Exception e)
            {
                FiskHeroes.LOGGER.warn("Could not download the FiskHeroes sound repository from {}: {}", repo, e.toString());
            }
            finally
            {
                downloading = false;
            }
        }, "FiskHeroes sound downloader");
        thread.setDaemon(true);
        thread.start();
    }

    private static void download(String repository, int version) throws IOException, InterruptedException
    {
        Path dir = root().resolve("pack");
        Files.createDirectories(dir);
        Path temp = Files.createTempFile(dir, "sounds", ".zip");

        String url = "https://raw.githubusercontent.com/" + repository + "/master/sounds/" + version + ".zip";
        FiskHeroes.LOGGER.info("Downloading the FiskHeroes sound repository ({})", url);

        try
        {
            HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(temp));
            if (response.statusCode() / 100 != 2) throw new IOException("HTTP " + response.statusCode());

            synchronized (SHSoundRepository.class)
            {
                // An interrupted replacement must not leave a stale cache marked ready.
                Files.deleteIfExists(root().resolve("sound-events.json"));
                extract(temp, dir);
                writeIndex(dir);
                loadCached();
            }
            FiskHeroes.LOGGER.info("FiskHeroes sound repository ready: {} sounds", INDEX.size());
            net.minecraft.client.Minecraft.getInstance().execute(SHSoundPack::refresh);
        }
        finally
        {
            Files.deleteIfExists(temp);
        }
    }

    /** Extracts the archive, keeping only the audio files and flattening the folder layout. */
    private static void extract(Path zip, Path dir) throws IOException
    {
        INDEX.clear();
        String eventManifest = null;

        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(zip)))
        {
            ZipEntry entry;

            while ((entry = in.getNextEntry()) != null)
            {
                if (entry.isDirectory())
                {
                    continue;
                }

                String name = entry.getName().replace('\\', '/');
                if (name.equals("sounds.json"))
                {
                    byte[] bytes = in.readNBytes(2_000_001);
                    if (bytes.length > 2_000_000) throw new IOException("Sound manifest is too large");
                    eventManifest = new String(bytes, StandardCharsets.UTF_8);
                    continue;
                }
                if (!name.toLowerCase(java.util.Locale.ROOT).endsWith(".ogg"))
                {
                    continue;
                }

                String path = assetPath(name);
                Path sounds = dir.resolve("assets/fiskheroes/sounds").toAbsolutePath().normalize();
                Path target = sounds.resolve(path).normalize();
                if (!target.startsWith(sounds) || !path.matches("[a-z0-9_./-]+"))
                    throw new IOException("Unsafe sound archive path: " + name);
                Files.createDirectories(target.getParent());
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                INDEX.put(eventName(path), path);
            }
        }
        if (eventManifest == null || INDEX.isEmpty()) throw new IOException("Sound archive lacks its event table or audio files");
        // Validate before marking the cache migratable on the next launch.
        com.google.gson.JsonParser.parseString(eventManifest).getAsJsonObject();
        Files.writeString(root().resolve("sound-events.json"), eventManifest, StandardCharsets.UTF_8);
    }

    /** Strips any leading folder (such as {@code sounds/} or {@code assets/}) from an archive path. */
    private static String assetPath(String name)
    {
        int index = name.indexOf("sounds/");

        if (index != -1 && index + "sounds/".length() < name.length())
        {
            name = name.substring(index + "sounds/".length());
        }

        return name;
    }

    /** {@code suit/agentliberty/blade/enable.ogg} -&gt; {@code fiskheroes:suit.agentliberty.blade.enable} */
    private static String eventName(String path)
    {
        String trimmed = path.substring(0, path.length() - ".ogg".length());
        return FiskHeroes.MODID + ":" + trimmed.replace('/', '.');
    }

    private static void writeIndex(Path dir) throws IOException
    {
        List<String> lines = new ArrayList<>();

        for (Map.Entry<String, String> e : INDEX.entrySet())
        {
            lines.add(e.getKey() + "=" + e.getValue());
        }

        Files.createDirectories(root());
        Files.write(root().resolve("sounds.index"), lines, StandardCharsets.UTF_8);

        // The original archive supplies event names, variants and pitch/stream metadata. File
        // names do not map one-to-one to event names (for example ambient.flight -> flight_loop).
        Path manifest = root().resolve("sound-events.json");
        if (!Files.isRegularFile(manifest)) throw new IOException("Sound archive lacks sounds.json");
        com.google.gson.JsonObject original;
        try (java.io.Reader reader = Files.newBufferedReader(manifest, StandardCharsets.UTF_8))
        {
            original = com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
        }
        com.google.gson.JsonObject converted = com.fiskmods.heroes.common.sound.SoundManifest.normalize(original);
        Files.writeString(dir.resolve("assets/fiskheroes/sounds.json"), converted.toString(), StandardCharsets.UTF_8);
        Files.writeString(dir.resolve("pack.mcmeta"), "{\n  \"pack\": {\n    \"pack_format\": 15,\n    \"description\": \"Fisk's Superheroes sounds\"\n  }\n}\n", StandardCharsets.UTF_8);
    }

    /** Reads a sound straight out of the downloaded pack; returns the stream or {@code null}. */
    public static InputStream open(String path) throws IOException
    {
        Path file = root().resolve("pack/assets/fiskheroes/sounds/" + path);
        return Files.isRegularFile(file) ? Files.newInputStream(file) : null;
    }

    public static boolean hasEvent(String event)
    {
        return INDEX.containsKey(event);
    }

    public static void markReady(boolean value)
    {
        ready = value;
    }
}
