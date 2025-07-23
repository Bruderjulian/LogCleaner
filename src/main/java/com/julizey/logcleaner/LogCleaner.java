package com.julizey.logcleaner;

import java.io.File;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.plugin.java.JavaPlugin;

public class LogCleaner extends JavaPlugin {

  private String mode;
  private int days;
  private int keepLogs;
  private boolean deleteCrashReports;
  private long cleanInterval;

  private static final List<String> modes = List.of("days", "amount", "both");

  @Override
  public void onEnable() {
    saveDefaultConfig();
    mode = getConfig().getString("mode", "days");
    days = getConfig().getInt("days", 3);
    keepLogs = getConfig().getInt("keep-logs", 3);
    deleteCrashReports = getConfig().getBoolean("delete-crash-reports", true);
    cleanInterval = getConfig().getLong("clean-interval", 1728000L);

    if (!modes.contains(mode)) {
      getLogger().log(Level.WARNING, "Invalid Mode: " + mode);
      return;
    }

    getLogger().log(Level.INFO, "LogCleaner enabled! Mode: " + mode);
    cleanLogs();
    if (cleanInterval <= 0) return;
    getServer()
      .getScheduler()
      .runTaskTimer(this, this::cleanLogs, 0L, cleanInterval);
  }

  private void cleanLogs() {
    cleanDirectory(new File(getServer().getWorldContainer(), "logs"));

    if (deleteCrashReports) {
      cleanDirectory(
        new File(getServer().getWorldContainer(), "crash-reports")
      );
    }
  }

  private void cleanDirectory(File directory) {
    if (!directory.exists()) {
      getLogger()
        .log(Level.WARNING, "Directory does not exist: " + directory.getName());
      return;
    }

    File[] files = directory.listFiles((dir, name) ->
      name.endsWith(".log.gz") ||
      name.equals("latest.log") ||
      name.endsWith(".txt")
    );
    if (files == null || files.length == 0) {
      getLogger()
        .log(
          Level.WARNING,
          "No files found in directory: " + directory.getName()
        );
      return;
    }

    Arrays.sort(files, Comparator.comparingLong(File::lastModified).reversed());

    if ("days".equalsIgnoreCase(mode)) {
      cleanFilesByDays(files);
    } else if ("amount".equalsIgnoreCase(mode)) {
      cleanFilesByAmount(files);
    } else if ("both".equalsIgnoreCase(mode)) {
      cleanFiles(files);
    }
  }

  private void cleanFilesByDays(File[] files) {
    Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
    for (File file : files) {
      if (file.getName().equals("latest.log")) {
        continue;
      }

      if (file.lastModified() < cutoff.toEpochMilli()) {
        if (file.delete()) {
          getLogger()
            .log(
              Level.INFO,
              "Deleted file (older than " + days + " days): " + file.getName()
            );
        } else {
          getLogger()
            .log(Level.WARNING, "Failed to delete file: " + file.getName());
        }
      }
    }
  }

  private void cleanFilesByAmount(File[] files) {
    int filesKept = 0;
    for (File file : files) {
      if (file.getName().equals("latest.log")) {
        continue;
      }

      filesKept++;
      if (filesKept < keepLogs) continue;
      if (file.delete()) {
        getLogger()
          .log(
            Level.INFO,
            "Deleted file (exceeded " + keepLogs + " files): " + file.getName()
          );
      } else {
        getLogger()
          .log(Level.WARNING, "Failed to delete file: " + file.getName());
      }
    }
  }

  private void cleanFiles(File[] files) {
    int filesKept = 0;
    Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
    for (File file : files) {
      if (file.getName().equals("latest.log")) {
        continue;
      }

      filesKept++;
      if (filesKept < keepLogs) {
        if (file.delete()) {
          getLogger()
            .log(
              Level.INFO,
              "Deleted file (exceeded " +
              keepLogs +
              " files): " +
              file.getName()
            );
        } else {
          getLogger()
            .log(Level.WARNING, "Failed to delete file: " + file.getName());
        }
      }

      if (file.lastModified() < cutoff.toEpochMilli()) {
        if (file.delete()) {
          getLogger()
            .log(
              Level.INFO,
              "Deleted file (older than " + days + " days): " + file.getName()
            );
        } else {
          getLogger()
            .log(Level.WARNING, "Failed to delete file: " + file.getName());
        }
      }
    }
  }

  @Override
  public void onDisable() {
    getLogger().log(Level.INFO, "LogCleaner has been disabled!");
  }
}
