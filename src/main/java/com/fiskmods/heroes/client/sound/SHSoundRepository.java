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

    private static Path root;
    private static boolean downloading;
    private static boolean ready;
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
    public static void loadCached()
    {
        Path indexFile = root().resolve("sounds.index");

        if (!Files.isRegularFile(indexFile))
        {
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

            ready = !INDEX.isEmpty();
            FiskHeroes.LOGGER.info("FiskHeroes sound repository: {} cached sounds", INDEX.size());
        }
        catch (IOException e)
        {
            FiskHeroes.LOGGER.warn("Could not read the cached sound index", e);
        }
    }

    /** Downloads the repository in the background when it is not cached yet. */
    public static void downloadIfMissing(String repository, int version)
    {
        if (ready || downloading || Files.isRegularFile(root().resolve("sounds.index")))
        {
            loadCached();
            return;
        }

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

        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        HttpResponse<Path> response = client.send(request, HttpResponse.BodyHandlers.ofFile(temp));

        if (response.statusCode() / 100 != 2)
        {
            Files.deleteIfExists(temp);
            throw new IOException("HTTP " + response.statusCode());
        }

        extract(temp, dir);
        Files.deleteIfExists(temp);
        writeIndex(dir);
        loadCached();
        SHSoundPack.refresh();
        FiskHeroes.LOGGER.info("FiskHeroes sound repository ready: {} sounds", INDEX.size());
    }

    /** Extracts the archive, keeping only the audio files and flattening the folder layout. */
    private static void extract(Path zip, Path dir) throws IOException
    {
        INDEX.clear();

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

                if (!name.toLowerCase(java.util.Locale.ROOT).endsWith(".ogg"))
                {
                    continue;
                }

                String path = assetPath(name);
                Path target = dir.resolve("assets/fiskheroes/sounds/" + path);
                Files.createDirectories(target.getParent());
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                INDEX.put(eventName(path), path);
            }
        }
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

        // sounds.json for the pack we serve to the client
        StringBuilder json = new StringBuilder("{\n");
        boolean first = true;

        for (Map.Entry<String, String> e : INDEX.entrySet())
        {
            String event = e.getKey().substring(e.getKey().indexOf(':') + 1);

            if (!first)
            {
                json.append(",\n");
            }

            first = false;
            json.append("  \"").append(event).append("\": { \"sounds\": [ { \"name\": \"").append(e.getValue()).append("\", \"stream\": true } ] }");
        }

        json.append("\n}\n");
        Files.write(dir.resolve("assets/fiskheroes/sounds.json"), json.toString().getBytes(StandardCharsets.UTF_8));
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

    static String describe(int count)
    {
        return count + " sounds";
    }
}
