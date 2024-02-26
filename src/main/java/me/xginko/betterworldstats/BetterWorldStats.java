package me.xginko.betterworldstats;

import com.tcoded.folialib.FoliaLib;
import me.xginko.betterworldstats.commands.BetterWorldStatsCommand;
import me.xginko.betterworldstats.config.Config;
import me.xginko.betterworldstats.config.LanguageCache;
import me.xginko.betterworldstats.utils.KyoriUtil;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;
import org.bstats.bukkit.Metrics;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public final class BetterWorldStats extends JavaPlugin {
    public static final TextColor COLOR = TextColor.color(0,204,204);
    public static final Style STYLE = Style.style(COLOR, TextDecoration.BOLD);

    private static BetterWorldStats instance;
    private static FoliaLib foliaLib;
    private static Map<String, LanguageCache> languageCacheMap;
    private static Config config;
    private static Statistics statistics;
    private static PAPIExpansion papiExpansion;
    private static BukkitAudiences audiences;
    private static ComponentLogger logger;
    private static Metrics metrics;

    @Override
    public void onEnable() {
        instance = this;
        foliaLib = new FoliaLib(this);
        audiences = BukkitAudiences.create(this);
        logger = ComponentLogger.logger(this.getName());
        metrics = new Metrics(this, 17204);
        logger.info(Component.text("                                                                                ").style(STYLE));
        logger.info(Component.text("  ___      _   _         __      __       _    _ ___ _        _                 ").style(STYLE));
        logger.info(Component.text(" | _ ) ___| |_| |_ ___ _ \\ \\    / /__ _ _| |__| / __| |_ __ _| |_ ___         ").style(STYLE));
        logger.info(Component.text(" | _ \\/ -_)  _|  _/ -_) '_\\ \\/\\/ / _ \\ '_| / _` \\__ \\  _/ _` |  _(_-<    ").style(STYLE));
        logger.info(Component.text(" |___/\\___|\\__|\\__\\___|_|  \\_/\\_/\\___/_| |_\\__,_|___/\\__\\__,_|\\__/__/").style(STYLE));
        logger.info(Component.text("                                                                                ").style(STYLE));
        logger.info("Loading languages");
        reloadLang();
        logger.info("Loading config");
        reloadConfiguration();
        logger.info("Registering commands");
        BetterWorldStatsCommand.reloadCommands();
        logger.info("Done.");
    }

    @Override
    public void onDisable() {
        HandlerList.unregisterAll(this);
        if (foliaLib != null) {
            foliaLib.getImpl().cancelAllTasks();
            foliaLib = null;
        }
        if (audiences != null) {
            audiences.close();
            audiences = null;
        }
        if (papiExpansion != null) {
            papiExpansion.unregister();
            papiExpansion = null;
        }
        if (metrics != null) {
            metrics.shutdown();
            metrics = null;
        }
        statistics = null;
        config = null;
        languageCacheMap = null;
        logger = null;
    }

    public static BetterWorldStats getInstance() {
        return instance;
    }

    public static Statistics getStatistics() {
        return statistics;
    }

    public static FoliaLib getFoliaLib() {
        return foliaLib;
    }

    public static BukkitAudiences getAudiences() {
        return audiences;
    }

    public static Config getConfiguration() {
        return config;
    }

    public static ComponentLogger getLog() {
        return logger;
    }

    public static LanguageCache getLang(Locale locale) {
        return getLang(locale.toString().toLowerCase());
    }

    public static LanguageCache getLang(CommandSender commandSender) {
        return commandSender instanceof Player ? getLang(KyoriUtil.getLocale((Player) commandSender)) : getLang(config.default_lang);
    }

    public static LanguageCache getLang(String lang) {
        if (!config.auto_lang) return languageCacheMap.get(config.default_lang.toString().toLowerCase());
        return languageCacheMap.getOrDefault(lang.replace("-", "_"), languageCacheMap.get(config.default_lang.toString().toLowerCase()));
    }

    public void reloadPlugin() {
        reloadLang();
        reloadConfiguration();
        BetterWorldStatsCommand.reloadCommands();
    }

    private void reloadConfiguration() {
        try {
            HandlerList.unregisterAll(this);
            config = new Config();
            statistics = new Statistics();
            if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                if (papiExpansion != null) papiExpansion.unregister();
                papiExpansion = new PAPIExpansion();
            }
            config.saveConfig();
        } catch (Exception e) {
            logger.error("Failed loading config!", e);
        }
    }

    public void reloadLang() {
        languageCacheMap = new HashMap<>();
        try {
            File langDirectory = new File(getDataFolder()  + File.separator + "lang");
            Files.createDirectories(langDirectory.toPath());
            for (String fileName : getDefaultLanguageFiles()) {
                final String localeString = fileName.substring(fileName.lastIndexOf(File.separator) + 1, fileName.lastIndexOf('.'));
                logger.info("Found language file for " + localeString);
                languageCacheMap.put(localeString, new LanguageCache(localeString));
            }
            final Pattern langPattern = Pattern.compile("([a-z]{1,3}_[a-z]{1,3})(\\.yml)", Pattern.CASE_INSENSITIVE);
            for (File langFile : langDirectory.listFiles()) {
                final Matcher langMatcher = langPattern.matcher(langFile.getName());
                if (langMatcher.find()) {
                    final String localeString = langMatcher.group(1).toLowerCase();
                    if (!languageCacheMap.containsKey(localeString)) { // make sure it wasn't a default file that we already loaded
                        logger.info("Found language file for " + localeString);
                        languageCacheMap.put(localeString, new LanguageCache(localeString));
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error loading language files! Language files will not reload to avoid errors, make sure to correct this before restarting the server!", e);
        }
    }

    private Set<String> getDefaultLanguageFiles() {
        try (final JarFile pluginJarFile = new JarFile(this.getFile())) {
            return pluginJarFile.stream()
                    .map(ZipEntry::getName)
                    .filter(name -> name.startsWith("lang" + File.separator) && name.endsWith(".yml"))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            logger.error("Failed getting default lang files!", e);
            return Collections.emptySet();
        }
    }
}