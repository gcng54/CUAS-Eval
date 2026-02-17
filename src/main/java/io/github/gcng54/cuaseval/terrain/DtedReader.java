package io.github.gcng54.cuaseval.terrain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.*;
import java.util.Locale;

/**
 * Reads DTED elevation data (.dt0 / .dt1 / .dt2) and SRTM HGT files,
 * and manages a binary cache for fast random-access elevation queries.
 * <p>
 * Supported formats:
 * <ul>
 *   <li>DTED Level 0 (.dt0) — 121 × 121 posts (~30 arc-sec, ~900 m)</li>
 *   <li>DTED Level 1 (.dt1) — 1201 × 1201 posts (~3 arc-sec, ~90 m)</li>
 *   <li>DTED Level 2 (.dt2) — 3601 × 3601 posts (~1 arc-sec, ~30 m)</li>
 *   <li>SRTM HGT (.hgt)   — 3601 × 3601 (SRTM1) or 1201 × 1201 (SRTM3)</li>
 * </ul>
 * File naming: {@code eXXX/nYY.dtN} or {@code N38E027.hgt}.
 * </p>
 * <p>
 * The cache format ({@code .dtcache}) stores all tiles in a flat binary file.
 * Cache version 2 stores the tile resolution (posts per side) in the header.
 * </p>
 */
public class DtedReader {

    private static final Logger log = LoggerFactory.getLogger(DtedReader.class);

    /** Grid dimensions per DTED level */
    public static final int DTED0_POSTS = 121;
    public static final int DTED1_POSTS = 1201;
    public static final int DTED2_POSTS = 3601;

    /** Elevation value representing "no data" (ocean or void) */
    public static final short NO_DATA = -32767;

    /** DTED header sizes (UHL 80 + DSI 648 + ACC 2700) */
    private static final int DTED_HEADER_SIZE = 3428;

    // ── Cache format constants ──────────────────────────────────────────
    private static final byte[] MAGIC_V1 = "DTCACHE1".getBytes();
    private static final byte[] MAGIC_V2 = "DTCACHE2".getBytes();

    // ── In-memory cache ─────────────────────────────────────────────────
    private int minLon, minLat, maxLon, maxLat;
    private int gridCols, gridRows;
    private int tilePosts;           // posts per side for cached tiles
    private short[][][] elevations;  // [col][row][posts*posts]
    private boolean loaded;

    // ── SRTM HGT directory for high-res queries ────────────────────────
    private File srtmDir;

    // ── SRTM directory for high-res on-demand queries ─────────────────

    /**
     * Set the directory containing SRTM HGT files for high-resolution queries.
     * Files are expected as {@code NxxEyyy.hgt} in flat layout.
     */
    public void setSrtmDir(File dir) {
        this.srtmDir = dir;
        log.info("SRTM directory set: {}", dir);
    }

    public File getSrtmDir() { return srtmDir; }

    // ── Read DTED files (any level) ─────────────────────────────────────

    /**
     * Detect the DTED level from file extension and size, then read.
     *
     * @param file any .dt0 / .dt1 / .dt2 file
     * @return elevation array [lonCol][latRow], or null on error
     */
    public short[][] readDted(File file) {
        int posts = detectDtedPosts(file);
        if (posts <= 0) return null;
        return readDtedFile(file, posts);
    }

    /**
     * Read a single .dt0 file and return its 121×121 elevation grid.
     */
    public short[][] readDt0(File file) {
        return readDtedFile(file, DTED0_POSTS);
    }

    /**
     * Read a single .dt1 file and return its 1201×1201 elevation grid.
     */
    public short[][] readDt1(File file) {
        return readDtedFile(file, DTED1_POSTS);
    }

    /**
     * Read a single .dt2 file and return its 3601×3601 elevation grid.
     */
    public short[][] readDt2(File file) {
        return readDtedFile(file, DTED2_POSTS);
    }

    /**
     * Read a DTED file with the given number of posts per side.
     */
    private short[][] readDtedFile(File file, int posts) {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(DTED_HEADER_SIZE);

            short[][] grid = new short[posts][posts];

            for (int col = 0; col < posts; col++) {
                // Record header: sentinel(1) + block_count(3) + lon_count(2) + lat_count(2) = 8 bytes
                raf.skipBytes(8);

                byte[] data = new byte[posts * 2];
                raf.readFully(data);

                ByteBuffer buf = ByteBuffer.wrap(data);
                buf.order(ByteOrder.BIG_ENDIAN);
                for (int row = 0; row < posts; row++) {
                    short raw = buf.getShort();
                    // DTED elevations use sign-magnitude (bit 15 = sign)
                    if ((raw & 0x8000) != 0) {
                        grid[col][row] = (short) -(raw & 0x7FFF);
                    } else {
                        grid[col][row] = raw;
                    }
                }

                // Checksum: 4 bytes
                raf.skipBytes(4);
            }

            return grid;

        } catch (IOException e) {
            log.warn("Failed to read DTED file {}: {}", file.getName(), e.getMessage());
            return null;
        }
    }

    // ── Read SRTM HGT files ────────────────────────────────────────────

    /**
     * Read a single SRTM HGT file. Auto-detects SRTM1 (3601) vs SRTM3 (1201)
     * from file size.
     * <p>
     * HGT format: raw 16-bit signed big-endian integers, row-major from
     * the NW corner. No header.
     * </p>
     *
     * @param file .hgt file
     * @return elevation [lonCol][latRow] with row 0 = south (DTED convention),
     *         or null on error
     */
    public short[][] readHgt(File file) {
        long fileSize = file.length();
        int posts;
        if (fileSize == (long) DTED2_POSTS * DTED2_POSTS * 2) {
            posts = DTED2_POSTS; // SRTM1 — 1 arc-second
        } else if (fileSize == (long) DTED1_POSTS * DTED1_POSTS * 2) {
            posts = DTED1_POSTS; // SRTM3 — 3 arc-second
        } else {
            log.warn("Unknown HGT file size {} for {}", fileSize, file.getName());
            return null;
        }

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {

            // HGT is row-major from NW corner. We need [col][row] with row 0 = south.
            short[][] grid = new short[posts][posts];
            for (int hgtRow = 0; hgtRow < posts; hgtRow++) {
                int latRow = posts - 1 - hgtRow; // flip: HGT row 0 = north
                for (int col = 0; col < posts; col++) {
                    short raw = dis.readShort();
                    grid[col][latRow] = (raw == -32768) ? NO_DATA : raw;
                }
            }

            return grid;

        } catch (IOException e) {
            log.warn("Failed to read HGT file {}: {}", file.getName(), e.getMessage());
            return null;
        }
    }

    /**
     * Get high-resolution elevation from SRTM HGT files (on-demand, not cached).
     * Falls back to cached data if HGT file is not available.
     */
    public double getHighResElevation(double lat, double lon) {
        if (srtmDir != null) {
            int tileLat = (int) Math.floor(lat);
            int tileLon = (int) Math.floor(lon);
            String name = String.format(Locale.ENGLISH, "%s%02d%s%03d.hgt",
                    tileLat >= 0 ? "N" : "S", Math.abs(tileLat),
                    tileLon >= 0 ? "E" : "W", Math.abs(tileLon));
            File hgtFile = new File(srtmDir, name);
            if (hgtFile.exists()) {
                short[][] grid = readHgt(hgtFile);
                if (grid != null) {
                    int posts = grid.length;
                    double fracLon = lon - tileLon;
                    double fracLat = lat - tileLat;
                    double gx = fracLon * (posts - 1);
                    double gy = fracLat * (posts - 1);
                    int x0 = (int) Math.floor(gx);
                    int y0 = (int) Math.floor(gy);
                    int x1 = Math.min(x0 + 1, posts - 1);
                    int y1 = Math.min(y0 + 1, posts - 1);
                    double fx = gx - x0;
                    double fy = gy - y0;

                    short e00 = grid[x0][y0], e10 = grid[x1][y0];
                    short e01 = grid[x0][y1], e11 = grid[x1][y1];

                    if (e00 == NO_DATA || e10 == NO_DATA || e01 == NO_DATA || e11 == NO_DATA) {
                        if (e00 != NO_DATA) return e00;
                        if (e10 != NO_DATA) return e10;
                        if (e01 != NO_DATA) return e01;
                        if (e11 != NO_DATA) return e11;
                        return NO_DATA;
                    }
                    return e00 * (1 - fx) * (1 - fy) + e10 * fx * (1 - fy)
                         + e01 * (1 - fx) * fy + e11 * fx * fy;
                }
            }
        }
        // Fall back to cached (lower-res) data
        return getElevation(lat, lon);
    }

    // ── Detect DTED level ───────────────────────────────────────────────

    private int detectDtedPosts(File file) {
        String name = file.getName().toLowerCase(Locale.ENGLISH);
        if (name.endsWith(".dt0")) return DTED0_POSTS;
        if (name.endsWith(".dt1")) return DTED1_POSTS;
        if (name.endsWith(".dt2")) return DTED2_POSTS;
        if (name.endsWith(".hgt")) {
            long size = file.length();
            if (size == (long) DTED2_POSTS * DTED2_POSTS * 2) return DTED2_POSTS;
            if (size == (long) DTED1_POSTS * DTED1_POSTS * 2) return DTED1_POSTS;
        }
        log.warn("Cannot detect DTED level for {}", file.getName());
        return -1;
    }

    // ── Build Cache ─────────────────────────────────────────────────────

    /**
     * Build a .dtcache file from a directory tree of DTED files (any level)
     * or from a flat directory of SRTM HGT files.
     * <p>
     * For DTED: expects {@code dtedRoot/eXXX/nYY.dtN} layout.<br>
     * For SRTM: expects {@code dtedRoot/NxxEyyy.hgt} flat layout.
     * </p>
     * <p>
     * Auto-detects the highest available DTED level. All tiles are stored
     * at the detected resolution in cache v2 format.
     * </p>
     *
     * @param dtedRoot  root directory containing DTED or HGT files
     * @param cacheFile output .dtcache file
     */
    public void buildCache(File dtedRoot, File cacheFile) {
        log.info("Building DTED cache from {} → {}", dtedRoot, cacheFile);

        // Detect available file format: prefer DT2 > DT1 > DT0 > HGT
        String dtExt = detectBestDtedExtension(dtedRoot);
        boolean isHgt = "hgt".equals(dtExt);
        int posts = postsForExtension(dtExt);

        log.info("Detected format: .{} ({} posts per tile)", dtExt, posts);

        // First pass: determine bounds
        int minLn = Integer.MAX_VALUE, maxLn = Integer.MIN_VALUE;
        int minLt = Integer.MAX_VALUE, maxLt = Integer.MIN_VALUE;
        int tileCount = 0;

        if (isHgt) {
            // HGT: flat directory of NxxEyyy.hgt files
            File[] hgtFiles = dtedRoot.listFiles(f ->
                    f.getName().matches("[NSns]\\d{2}[EWew]\\d{3}\\.hgt"));
            if (hgtFiles == null || hgtFiles.length == 0) {
                log.error("No HGT files found in {}", dtedRoot);
                return;
            }
            for (File f : hgtFiles) {
                int[] coords = parseHgtName(f.getName());
                minLt = Math.min(minLt, coords[0]);
                maxLt = Math.max(maxLt, coords[0]);
                minLn = Math.min(minLn, coords[1]);
                maxLn = Math.max(maxLn, coords[1]);
                tileCount++;
            }
        } else {
            // DTED: eXXX/nYY.dtN layout
            File[] lonDirs = dtedRoot.listFiles(f -> f.isDirectory() && f.getName().matches("[ewEW]\\d+"));
            if (lonDirs == null || lonDirs.length == 0) {
                log.error("No DTED directories found in {}", dtedRoot);
                return;
            }
            String pattern = "[nsNS]\\d+\\." + dtExt;
            for (File lonDir : lonDirs) {
                int lon = parseLonDir(lonDir.getName());
                File[] dtFiles = lonDir.listFiles(f -> f.getName().matches(pattern));
                if (dtFiles == null) continue;
                for (File dtFile : dtFiles) {
                    int lat = parseLatFile(dtFile.getName());
                    minLn = Math.min(minLn, lon);
                    maxLn = Math.max(maxLn, lon);
                    minLt = Math.min(minLt, lat);
                    maxLt = Math.max(maxLt, lat);
                    tileCount++;
                }
            }
        }

        log.info("Found {} tiles, bounds: lon [{}, {}], lat [{}, {}]",
                tileCount, minLn, maxLn, minLt, maxLt);

        int cols = maxLn - minLn + 1;
        int rows = maxLt - minLt + 1;

        // Second pass: read tiles and write cache (v2 with posts field)
        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(cacheFile)))) {

            // Header v2: magic(8) + 7 ints (minLon, minLat, maxLon, maxLat, cols, rows, tilePosts)
            dos.write(MAGIC_V2);
            dos.writeInt(minLn);
            dos.writeInt(minLt);
            dos.writeInt(maxLn);
            dos.writeInt(maxLt);
            dos.writeInt(cols);
            dos.writeInt(rows);
            dos.writeInt(posts);

            for (int c = 0; c < cols; c++) {
                int lon = minLn + c;
                for (int r = 0; r < rows; r++) {
                    int lat = minLt + r;

                    short[][] grid = null;

                    if (isHgt) {
                        String name = String.format(Locale.ENGLISH, "%s%02d%s%03d.hgt",
                                lat >= 0 ? "N" : "S", Math.abs(lat),
                                lon >= 0 ? "E" : "W", Math.abs(lon));
                        File hgtFile = new File(dtedRoot, name);
                        if (hgtFile.exists()) {
                            grid = readHgt(hgtFile);
                        }
                    } else {
                        String lonDirName = String.format(Locale.ENGLISH,
                                "%s%03d", lon >= 0 ? "e" : "w", Math.abs(lon));
                        String latFileName = String.format(Locale.ENGLISH,
                                "%s%02d.%s", lat >= 0 ? "n" : "s", Math.abs(lat), dtExt);
                        File dtFile = new File(dtedRoot, lonDirName + File.separator + latFileName);
                        if (dtFile.exists()) {
                            grid = readDtedFile(dtFile, posts);
                        }
                    }

                    if (grid != null) {
                        dos.writeBoolean(true);
                        for (int lc = 0; lc < posts; lc++) {
                            for (int lr = 0; lr < posts; lr++) {
                                dos.writeShort(grid[lc][lr]);
                            }
                        }
                    } else {
                        dos.writeBoolean(false);
                    }
                }
            }

            log.info("DTED cache built: {} bytes ({} posts), {} tiles present",
                    cacheFile.length(), posts, tileCount);

        } catch (IOException e) {
            log.error("Failed to build DTED cache: {}", e.getMessage(), e);
        }
    }

    // ── Load Cache ──────────────────────────────────────────────────────

    /**
     * Load a .dtcache file into memory for fast elevation queries.
     * Supports both v1 (DTCACHE1, always 121 posts) and v2 (DTCACHE2, variable posts).
     *
     * @param cacheFile the cache file
     * @return true if loaded successfully
     */
    public boolean loadCache(File cacheFile) {
        if (!cacheFile.exists()) {
            log.warn("DTED cache not found: {}", cacheFile);
            return false;
        }

        try (DataInputStream dis = new DataInputStream(
                new BufferedInputStream(new FileInputStream(cacheFile)))) {

            // Read magic
            byte[] magic = new byte[8];
            dis.readFully(magic);
            String magicStr = new String(magic);

            boolean isV2;
            if ("DTCACHE2".equals(magicStr)) {
                isV2 = true;
            } else if ("DTCACHE1".equals(magicStr)) {
                isV2 = false;
            } else {
                log.error("Invalid DTED cache format: {}", magicStr);
                return false;
            }

            minLon = dis.readInt();
            minLat = dis.readInt();
            maxLon = dis.readInt();
            maxLat = dis.readInt();
            gridCols = dis.readInt();
            gridRows = dis.readInt();

            if (isV2) {
                tilePosts = dis.readInt();
            } else {
                tilePosts = DTED0_POSTS; // v1 always 121
            }

            elevations = new short[gridCols][gridRows][];

            for (int c = 0; c < gridCols; c++) {
                for (int r = 0; r < gridRows; r++) {
                    boolean present = dis.readBoolean();
                    if (present) {
                        short[] data = new short[tilePosts * tilePosts];
                        for (int i = 0; i < data.length; i++) {
                            data[i] = dis.readShort();
                        }
                        elevations[c][r] = data;
                    }
                }
            }

            loaded = true;
            log.info("DTED cache loaded ({}): lon [{}, {}], lat [{}, {}], {}×{} grid, {} posts/tile",
                    isV2 ? "v2" : "v1", minLon, maxLon, minLat, maxLat, gridCols, gridRows, tilePosts);
            return true;

        } catch (IOException e) {
            log.error("Failed to load DTED cache: {}", e.getMessage(), e);
            return false;
        }
    }

    // ── Elevation Query ─────────────────────────────────────────────────

    /**
     * Get terrain elevation at a geographic position using bilinear interpolation.
     *
     * @param lat latitude in decimal degrees
     * @param lon longitude in decimal degrees
     * @return elevation in metres MSL, or NO_DATA if unavailable
     */
    public double getElevation(double lat, double lon) {
        if (!loaded) return NO_DATA;

        // Determine which tile
        int tileLon = (int) Math.floor(lon);
        int tileLat = (int) Math.floor(lat);

        int col = tileLon - minLon;
        int row = tileLat - minLat;

        if (col < 0 || col >= gridCols || row < 0 || row >= gridRows) {
            return NO_DATA;
        }

        short[] data = elevations[col][row];
        if (data == null) return NO_DATA;

        // Position within the tile (0.0 to 1.0)
        double fracLon = lon - tileLon;
        double fracLat = lat - tileLat;

        // Grid indices (0 to posts-1)
        double gx = fracLon * (tilePosts - 1);
        double gy = fracLat * (tilePosts - 1);

        int x0 = (int) Math.floor(gx);
        int y0 = (int) Math.floor(gy);
        int x1 = Math.min(x0 + 1, tilePosts - 1);
        int y1 = Math.min(y0 + 1, tilePosts - 1);

        double fx = gx - x0;
        double fy = gy - y0;

        // Bilinear interpolation
        short e00 = data[x0 * tilePosts + y0];
        short e10 = data[x1 * tilePosts + y0];
        short e01 = data[x0 * tilePosts + y1];
        short e11 = data[x1 * tilePosts + y1];

        if (e00 == NO_DATA || e10 == NO_DATA || e01 == NO_DATA || e11 == NO_DATA) {
            // Return nearest valid value
            if (e00 != NO_DATA) return e00;
            if (e10 != NO_DATA) return e10;
            if (e01 != NO_DATA) return e01;
            if (e11 != NO_DATA) return e11;
            return NO_DATA;
        }

        double elev = e00 * (1 - fx) * (1 - fy)
                    + e10 * fx * (1 - fy)
                    + e01 * (1 - fx) * fy
                    + e11 * fx * fy;

        return elev;
    }

    /**
     * Get elevation data within a circular boundary for rendering.
     *
     * @param centreLatDeg  centre latitude
     * @param centreLonDeg  centre longitude
     * @param radiusKm      radius in kilometres
     * @param resolution    grid resolution (number of samples per side)
     * @return elevation grid [x][y] with corresponding lat/lon bounds
     */
    public ElevationGrid getElevationGrid(double centreLatDeg, double centreLonDeg,
                                           double radiusKm, int resolution) {
        double radiusDegLat = radiusKm / 111.32;
        double radiusDegLon = radiusKm / (111.32 * Math.cos(Math.toRadians(centreLatDeg)));

        double latMin = centreLatDeg - radiusDegLat;
        double latMax = centreLatDeg + radiusDegLat;
        double lonMin = centreLonDeg - radiusDegLon;
        double lonMax = centreLonDeg + radiusDegLon;

        double[][] grid = new double[resolution][resolution];
        double stepLat = (latMax - latMin) / (resolution - 1);
        double stepLon = (lonMax - lonMin) / (resolution - 1);

        double elevMin = Double.MAX_VALUE;
        double elevMax = Double.MIN_VALUE;

        for (int x = 0; x < resolution; x++) {
            double lon = lonMin + x * stepLon;
            for (int y = 0; y < resolution; y++) {
                double lat = latMin + y * stepLat;

                // Check if within circular boundary
                double dLat = lat - centreLatDeg;
                double dLon = (lon - centreLonDeg) * Math.cos(Math.toRadians(centreLatDeg));
                double distKm = Math.sqrt(dLat * dLat + dLon * dLon) * 111.32;

                if (distKm <= radiusKm) {
                    double elev = getElevation(lat, lon);
                    grid[x][y] = elev;
                    if (elev != NO_DATA) {
                        if (elev < elevMin) elevMin = elev;
                        if (elev > elevMax) elevMax = elev;
                    }
                } else {
                    grid[x][y] = NO_DATA;
                }
            }
        }

        return new ElevationGrid(grid, latMin, latMax, lonMin, lonMax,
                elevMin == Double.MAX_VALUE ? 0 : elevMin,
                elevMax == Double.MIN_VALUE ? 0 : elevMax,
                resolution);
    }

    /**
     * Check if the cache is loaded and ready for queries.
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Get the geographic bounds of the loaded cache.
     */
    public double[] getBounds() {
        return new double[]{minLon, minLat, maxLon + 1, maxLat + 1};
    }

    /**
     * Get the number of elevation posts per tile side in the loaded cache.
     */
    public int getTilePosts() {
        return tilePosts;
    }

    // ── Elevation Grid Result ───────────────────────────────────────────

    /**
     * Container for a rectangular elevation grid with geographic bounds.
     */
    public static class ElevationGrid {
        private final double[][] data;
        private final double latMin, latMax, lonMin, lonMax;
        private final double elevMin, elevMax;
        private final int resolution;

        public ElevationGrid(double[][] data, double latMin, double latMax,
                             double lonMin, double lonMax,
                             double elevMin, double elevMax, int resolution) {
            this.data = data;
            this.latMin = latMin;
            this.latMax = latMax;
            this.lonMin = lonMin;
            this.lonMax = lonMax;
            this.elevMin = elevMin;
            this.elevMax = elevMax;
            this.resolution = resolution;
        }

        public double[][] getData()  { return data; }
        public double getLatMin()    { return latMin; }
        public double getLatMax()    { return latMax; }
        public double getLonMin()    { return lonMin; }
        public double getLonMax()    { return lonMax; }
        public double getElevMin()   { return elevMin; }
        public double getElevMax()   { return elevMax; }
        public int getResolution()   { return resolution; }
    }

    // ── Utilities ───────────────────────────────────────────────────────

    private int parseLonDir(String name) {
        char dir = name.charAt(0);
        int val = Integer.parseInt(name.substring(1));
        return (dir == 'w' || dir == 'W') ? -val : val;
    }

    private int parseLatFile(String name) {
        char dir = name.charAt(0);
        int val = Integer.parseInt(name.substring(1, name.indexOf('.')));
        return (dir == 's' || dir == 'S') ? -val : val;
    }

    /**
     * Parse an HGT filename like "N38E027.hgt" into [lat, lon].
     */
    private int[] parseHgtName(String name) {
        // N38E027.hgt
        char latDir = Character.toUpperCase(name.charAt(0));
        int lat = Integer.parseInt(name.substring(1, 3));
        if (latDir == 'S') lat = -lat;

        char lonDir = Character.toUpperCase(name.charAt(3));
        int lon = Integer.parseInt(name.substring(4, 7));
        if (lonDir == 'W') lon = -lon;

        return new int[]{lat, lon};
    }

    /**
     * Detect the best (highest resolution) DTED extension available in the directory.
     * Checks for dt2 > dt1 > dt0, then falls back to hgt.
     */
    private String detectBestDtedExtension(File root) {
        // Check for HGT files directly in root
        File[] hgtFiles = root.listFiles(f -> f.getName().matches("[NSns]\\d{2}[EWew]\\d{3}\\.hgt"));
        if (hgtFiles != null && hgtFiles.length > 0) return "hgt";

        // Check for DTED directories
        File[] lonDirs = root.listFiles(f -> f.isDirectory() && f.getName().matches("[ewEW]\\d+"));
        if (lonDirs == null || lonDirs.length == 0) return "dt0";

        // Check a sample lonDir for the highest available level
        for (File lonDir : lonDirs) {
            File[] dt2 = lonDir.listFiles(f -> f.getName().endsWith(".dt2"));
            if (dt2 != null && dt2.length > 0) return "dt2";

            File[] dt1 = lonDir.listFiles(f -> f.getName().endsWith(".dt1"));
            if (dt1 != null && dt1.length > 0) return "dt1";

            File[] dt0 = lonDir.listFiles(f -> f.getName().endsWith(".dt0"));
            if (dt0 != null && dt0.length > 0) return "dt0";
        }

        return "dt0";
    }

    /**
     * Return the number of posts per tile side for a given file extension.
     */
    private int postsForExtension(String ext) {
        switch (ext) {
            case "dt2": return DTED2_POSTS;
            case "dt1": return DTED1_POSTS;
            case "hgt": return DTED2_POSTS; // SRTM1 default; will correct when reading
            default:    return DTED0_POSTS;
        }
    }
}
