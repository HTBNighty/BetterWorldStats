package me.xginko.betterworldstats.stats;

import me.xginko.betterworldstats.BetterWorldStats;
import me.xginko.betterworldstats.config.Config;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class FileStats {

    private final @NotNull Config config;
    private final @NotNull AtomicReference<ScanResult> scan_result;
    private final @NotNull AtomicBoolean scan_lock;

    public FileStats() {
        this.config = BetterWorldStats.getConfiguration();
        this.scan_result = new AtomicReference<>();
        this.scan_lock = new AtomicBoolean();
        this.refresh();
    }

    private void refresh() {
        if (System.currentTimeMillis() > scan_result.get().expiration_time_millis && scan_lock.getAndSet(true)) {
            CompletableFuture.supplyAsync(() -> new ScanResult(config.paths_to_scan, config.filesize_update_period_millis))
                    .thenAccept(scan_result::set)
                    .thenRun(() -> scan_lock.set(false));
        }
    }

    public String getSize() {
        refresh();
        return config.filesize_format.format(scan_result.get().size_in_gb);
    }

    public String getSpoofedSize() {
        refresh();
        return config.filesize_format.format(scan_result.get().size_in_gb + config.additional_spoof_filesize);
    }

    public String getFolderCount() {
        refresh();
        return Integer.toString(scan_result.get().folder_count);
    }

    public String getFileCount() {
        refresh();
        return Integer.toString(scan_result.get().file_count);
    }

    public String getChunkFileCount() {
        refresh();
        return Integer.toString(scan_result.get().region_file_count);
    }

    private static class ScanResult {

        public final long expiration_time_millis;
        public final double size_in_gb;
        public int file_count, region_file_count, folder_count;

        protected ScanResult(@NotNull Iterable<String> paths_to_scan, long cooldow_millis) {
            this.file_count = this.region_file_count = this.folder_count = 0;
            long byteSize = 0L;
            for (String path : paths_to_scan)
                byteSize += this.getByteSize(new File(path));
            this.size_in_gb = byteSize / 1048576.0D / 1000.0D;
            this.expiration_time_millis = System.currentTimeMillis() + cooldow_millis;
        }

        private long getByteSize(File file) {
            long bytes = 0L;

            if (file.isDirectory()) {
                this.folder_count++;
                try {
                    File[] subFiles = file.listFiles();
                    assert subFiles != null;
                    for (File subFile : subFiles) {
                        bytes += this.getByteSize(subFile);
                    }
                } catch (Throwable t) {
                    BetterWorldStats.getLog().warn("Unable to stat directory '"+file.getPath()+"'.", t);
                }
            }

            else if (file.isFile()) {
                this.file_count++;
                // Check if it is a region file
                if (file.getName().endsWith(".mca")) {
                    final File parent = file.getParentFile();
                    if (parent.isDirectory() && parent.getName().toLowerCase().contains("region")) {
                        this.region_file_count++;
                    }
                }
                bytes += file.length();
            }

            return bytes;
        }
    }
}