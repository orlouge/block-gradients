package io.github.orlouge.blockgradients;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
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
            File f = new File(ExampleExpectPlatform.getConfigDirectory() + "/" + BLACKLIST_FNAME);
            if (f.isFile() && f.canRead()) {
                patterns = Files.readAllLines(f.toPath(), Charset.defaultCharset());
            } else {
                Files.write(f.toPath(), patterns);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String pattern : patterns) {
            blacklistRegex.append("|").append(("^\\Q" + pattern + "\\E$").replace("*", "\\E.*\\Q"));
        }
        blacklistRegex = new StringBuilder(blacklistRegex.substring(1));
        BLACKLIST_PATTERN = Pattern.compile(blacklistRegex.toString());
    }
}
