package io.github.gcng54.cuaseval.terrain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal ESRI Shapefile (.shp) reader for rendering country boundaries.
 * Supports Polygon (type 5) and PolyLine (type 3) shape types.
 * <p>
 * Does not require external GIS libraries — reads the binary format directly.
 * </p>
 */
public class ShapefileReader {

    private static final Logger log = LoggerFactory.getLogger(ShapefileReader.class);

    /** A polygon or polyline consisting of one or more parts (rings). */
    public static class ShapeRecord {
        private final int shapeType;
        private final double minX, minY, maxX, maxY;
        private final List<double[][]> parts; // each part is double[numPoints][2] (lon, lat)

        public ShapeRecord(int shapeType, double minX, double minY,
                           double maxX, double maxY, List<double[][]> parts) {
            this.shapeType = shapeType;
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
            this.parts = parts;
        }

        public int getShapeType()       { return shapeType; }
        public double getMinX()         { return minX; }
        public double getMinY()         { return minY; }
        public double getMaxX()         { return maxX; }
        public double getMaxY()         { return maxY; }
        public List<double[][]> getParts() { return parts; }
    }

    /**
     * Read all polygon/polyline records from a .shp file.
     *
     * @param shpFile the .shp file path
     * @return list of shape records
     */
    public List<ShapeRecord> read(File shpFile) {
        List<ShapeRecord> records = new ArrayList<>();

        try (RandomAccessFile raf = new RandomAccessFile(shpFile, "r")) {
            // ── File header (100 bytes) ──
            byte[] header = new byte[100];
            raf.readFully(header);

            ByteBuffer hdr = ByteBuffer.wrap(header);

            // File code (big-endian) = 9994
            hdr.order(ByteOrder.BIG_ENDIAN);
            int fileCode = hdr.getInt(0);
            if (fileCode != 9994) {
                log.error("Invalid shapefile: file code = {}", fileCode);
                return records;
            }

            // File length in 16-bit words (big-endian)
            int fileLengthWords = hdr.getInt(24);
            long fileLength = fileLengthWords * 2L;

            // Shape type (little-endian)
            hdr.order(ByteOrder.LITTLE_ENDIAN);
            int shapeType = hdr.getInt(32);
            log.info("Shapefile: type={}, length={} bytes", shapeType, fileLength);

            // ── Read records ──
            long pos = 100;
            while (pos < fileLength) {
                raf.seek(pos);

                // Record header (8 bytes, big-endian)
                byte[] recHdrBytes = new byte[8];
                if (raf.read(recHdrBytes) < 8) break;

                ByteBuffer recHdr = ByteBuffer.wrap(recHdrBytes);
                recHdr.order(ByteOrder.BIG_ENDIAN);
                int recNum = recHdr.getInt(0);
                int contentLengthWords = recHdr.getInt(4);
                int contentLength = contentLengthWords * 2;

                // Record content
                byte[] content = new byte[contentLength];
                raf.readFully(content);
                ByteBuffer buf = ByteBuffer.wrap(content);
                buf.order(ByteOrder.LITTLE_ENDIAN);

                int recShapeType = buf.getInt(0);

                if (recShapeType == 5 || recShapeType == 3) {
                    // Polygon or PolyLine
                    ShapeRecord rec = parsePolygon(buf, recShapeType);
                    if (rec != null) records.add(rec);
                }
                // Skip null shapes (type 0) and others

                pos += 8 + contentLength;
            }

            log.info("Read {} shape records from {}", records.size(), shpFile.getName());

        } catch (IOException e) {
            log.error("Failed to read shapefile: {}", e.getMessage(), e);
        }

        return records;
    }

    private ShapeRecord parsePolygon(ByteBuffer buf, int shapeType) {
        // Bounding box
        double minX = buf.getDouble(4);
        double minY = buf.getDouble(12);
        double maxX = buf.getDouble(20);
        double maxY = buf.getDouble(28);

        int numParts = buf.getInt(36);
        int numPoints = buf.getInt(40);

        // Part indices
        int[] partStarts = new int[numParts];
        int offset = 44;
        for (int i = 0; i < numParts; i++) {
            partStarts[i] = buf.getInt(offset);
            offset += 4;
        }

        // Points (X = lon, Y = lat)
        double[][] allPoints = new double[numPoints][2];
        for (int i = 0; i < numPoints; i++) {
            allPoints[i][0] = buf.getDouble(offset);      // X (lon)
            allPoints[i][1] = buf.getDouble(offset + 8);   // Y (lat)
            offset += 16;
        }

        // Split into parts
        List<double[][]> parts = new ArrayList<>();
        for (int p = 0; p < numParts; p++) {
            int start = partStarts[p];
            int end = (p + 1 < numParts) ? partStarts[p + 1] : numPoints;
            int length = end - start;
            double[][] part = new double[length][2];
            System.arraycopy(allPoints, start, part, 0, length);
            parts.add(part);
        }

        return new ShapeRecord(shapeType, minX, minY, maxX, maxY, parts);
    }
}
