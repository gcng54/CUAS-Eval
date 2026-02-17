package io.github.gcng54.cuaseval.terrain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;

/**
 * Downloads SRTM 1-arc-second elevation tiles (.hgt) from the public AWS
 * Terrain Tiles S3 bucket (no authentication required).
 * <p>
 * <b>Source:</b> Mapzen/AWS Terrain Tiles (Skadi dataset)<br>
 * <b>URL pattern:</b> {@code https://elevation-tiles-prod.s3.amazonaws.com/skadi/{lat}/{lat}{lon}.hgt.gz}<br>
 * <b>Resolution:</b> SRTM1 — 1 arc-second (~30 m), 3601 × 3601 posts per tile<br>
 * <b>Format:</b> GZIP-compressed SRTM HGT files<br>
 * <b>Coverage:</b> 60°S to 60°N, global
 * </p>
 * <p>
 * Downloaded tiles are stored locally for offline reuse.
 * </p>
 */
public class SrtmDownloader {

    private static final Logger log = LoggerFactory.getLogger(SrtmDownloader.class);

    /**
     * Base URL for the AWS Terrain Tiles (Skadi) bucket.
     * This is a public S3 bucket — no AWS credentials or API keys needed.
     */
    private static final String BASE_URL =
            "https://elevation-tiles-prod.s3.amazonaws.com/skadi";

    /** Expected file size for SRTM1 HGT (3601 × 3601 × 2 bytes) */
    private static final long SRTM1_SIZE = (long) DtedReader.DTED2_POSTS * DtedReader.DTED2_POSTS * 2;

    private final File outputDir;

    /**
     * Callback for download progress reporting.
     */
    public interface ProgressCallback {
        /**
         * Called for each tile progress update.
         *
         * @param tileName       e.g. "N38E027"
         * @param tileIndex      0-based index of current tile
         * @param totalTiles     total number of tiles to download
         * @param bytesReceived  bytes downloaded so far for this tile
         * @param totalBytes     total bytes for this tile (-1 if unknown)
         */
        void onProgress(String tileName, int tileIndex, int totalTiles,
                         long bytesReceived, long totalBytes);

        /** Called when all downloads are complete. */
        void onComplete(int downloaded, int skipped, int failed);
    }

    /**
     * Create a downloader that stores tiles in the given directory.
     *
     * @param outputDir directory to save .hgt files (will be created)
     */
    public SrtmDownloader(File outputDir) {
        this.outputDir = outputDir;
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
    }

    public File getOutputDir() { return outputDir; }

    /**
     * Compute which 1×1° tiles are needed to cover a circle.
     *
     * @param centreLat centre latitude
     * @param centreLon centre longitude
     * @param radiusKm  radius in kilometres
     * @return list of tile names, e.g. ["N38E027", "N38E026", …]
     */
    public List<String> computeTilesInBoundary(double centreLat, double centreLon, double radiusKm) {
        double radiusDegLat = radiusKm / 111.32;
        double radiusDegLon = radiusKm / (111.32 * Math.cos(Math.toRadians(centreLat)));

        int latMin = (int) Math.floor(centreLat - radiusDegLat);
        int latMax = (int) Math.floor(centreLat + radiusDegLat);
        int lonMin = (int) Math.floor(centreLon - radiusDegLon);
        int lonMax = (int) Math.floor(centreLon + radiusDegLon);

        List<String> tiles = new ArrayList<>();
        for (int lat = latMin; lat <= latMax; lat++) {
            for (int lon = lonMin; lon <= lonMax; lon++) {
                tiles.add(tileName(lat, lon));
            }
        }

        return tiles;
    }

    /**
     * Download all tiles needed to cover a circular area.
     * Skips tiles that already exist locally.
     *
     * @param centreLat  centre latitude
     * @param centreLon  centre longitude
     * @param radiusKm   radius in kilometres
     * @param callback   optional progress callback (may be null)
     * @return number of tiles successfully available (existing + newly downloaded)
     */
    public int downloadForBoundary(double centreLat, double centreLon, double radiusKm,
                                    ProgressCallback callback) {
        List<String> tiles = computeTilesInBoundary(centreLat, centreLon, radiusKm);
        log.info("Need {} SRTM tiles for boundary (lat={}, lon={}, r={}km)",
                tiles.size(), centreLat, centreLon, radiusKm);

        int downloaded = 0, skipped = 0, failed = 0;

        for (int i = 0; i < tiles.size(); i++) {
            String name = tiles.get(i);
            File localFile = new File(outputDir, name + ".hgt");

            if (localFile.exists() && localFile.length() == SRTM1_SIZE) {
                log.debug("SRTM tile {} already exists, skipping", name);
                skipped++;
                if (callback != null) {
                    callback.onProgress(name, i, tiles.size(), localFile.length(), localFile.length());
                }
                continue;
            }

            boolean ok = downloadTile(name, localFile, i, tiles.size(), callback);
            if (ok) {
                downloaded++;
            } else {
                failed++;
            }
        }

        log.info("SRTM download complete: {} downloaded, {} skipped, {} failed",
                downloaded, skipped, failed);

        if (callback != null) {
            callback.onComplete(downloaded, skipped, failed);
        }

        return downloaded + skipped;
    }

    /**
     * Download a single SRTM tile.
     *
     * @param tileName  e.g. "N38E027"
     * @param localFile destination file
     * @return true if download succeeded
     */
    private boolean downloadTile(String tileName, File localFile,
                                  int index, int total, ProgressCallback callback) {
        // URL: /skadi/{latDir}{lat}/{tileName}.hgt.gz
        String latPart = tileName.substring(0, 3); // e.g. "N38"
        String url = String.format(Locale.ENGLISH, "%s/%s/%s.hgt.gz", BASE_URL, latPart, tileName);

        log.info("Downloading SRTM tile {}/{}: {} → {}", index + 1, total, url, localFile.getName());

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(60_000);
            conn.setRequestProperty("User-Agent", "CUAS-Eval/1.0");

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                log.warn("HTTP {} for tile {} (may be ocean)", responseCode, tileName);
                return false;
            }

            long contentLength = conn.getContentLengthLong();

            // Download gzipped data, decompress, and write
            File tempFile = new File(localFile.getParentFile(), localFile.getName() + ".tmp");
            try (InputStream gzIn = new GZIPInputStream(new BufferedInputStream(conn.getInputStream()));
                 OutputStream fos = new BufferedOutputStream(new FileOutputStream(tempFile))) {

                byte[] buf = new byte[8192];
                long totalRead = 0;
                int n;
                while ((n = gzIn.read(buf)) != -1) {
                    fos.write(buf, 0, n);
                    totalRead += n;
                    if (callback != null && totalRead % (64 * 1024) == 0) {
                        callback.onProgress(tileName, index, total, totalRead, SRTM1_SIZE);
                    }
                }
            }

            // Verify size and rename
            if (tempFile.length() == SRTM1_SIZE) {
                if (localFile.exists()) localFile.delete();
                tempFile.renameTo(localFile);
                log.info("SRTM tile {} downloaded OK ({} bytes)", tileName, localFile.length());
                return true;
            } else {
                log.warn("SRTM tile {} unexpected size: {} (expected {})",
                        tileName, tempFile.length(), SRTM1_SIZE);
                tempFile.delete();
                return false;
            }

        } catch (IOException e) {
            log.error("Failed to download SRTM tile {}: {}", tileName, e.getMessage());
            return false;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /**
     * Format a tile name from lat/lon, e.g. "N38E027".
     */
    private static String tileName(int lat, int lon) {
        return String.format(Locale.ENGLISH, "%s%02d%s%03d",
                lat >= 0 ? "N" : "S", Math.abs(lat),
                lon >= 0 ? "E" : "W", Math.abs(lon));
    }

    /**
     * Get info string about download source for display.
     */
    public static String getSourceInfo() {
        return "Source: AWS Terrain Tiles (Skadi)\n"
             + "URL: elevation-tiles-prod.s3.amazonaws.com/skadi/\n"
             + "Resolution: SRTM1 — 1 arc-second (~30 m)\n"
             + "Format: 3601×3601 signed 16-bit elevation posts\n"
             + "Coverage: 60°S to 60°N, global\n"
             + "License: Public domain (NASA SRTM data)";
    }
}
