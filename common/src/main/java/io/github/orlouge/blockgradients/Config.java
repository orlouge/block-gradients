package io.github.orlouge.blockgradients;

import io.github.orlouge.blockgradients.platform.Services;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

public class Config {
    private static final String BLACKLIST_FNAME = "blockgradients-blacklist.txt";
    public static Pattern BLACKLIST_PATTERN;

    public static void loadConfig() {
        List<String> patterns = List.of(
                "minecraft:grass_block",
                "minecraft:grindstone",
                "minecraft:big_dripleaf",
                "minecraft:*_carpet",
                "mcw*:*",
                "everycomp:*"
        );
        StringBuilder blacklistRegex = new StringBuilder();

        try {
            Path path = Services.PLATFORM.getConfigDir().resolve(BLACKLIST_FNAME);
            if (Files.isRegularFile(path) && Files.isReadable(path)) {
                patterns = Files.readAllLines(path, Charset.defaultCharset());
            } else {
                Files.createDirectories(path.getParent());
                Files.write(path, patterns);
            }
        } catch (IOException e) {
            Constants.LOGGER.error("Failed to load block gradients blacklist", e);
        }

        for (String pattern : patterns) {
            blacklistRegex.append("|").append(("^\\Q" + pattern + "\\E$").replace("*", "\\E.*\\Q"));
        }
        BLACKLIST_PATTERN = Pattern.compile(blacklistRegex.substring(1));
    }
}
