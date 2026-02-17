package io.github.gcng54.cuaseval.report;

import net.sourceforge.plantuml.SourceFileReader;
import net.sourceforge.plantuml.GeneratedImage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Renders PlantUML (.puml) diagrams to PNG images.
 * <p>
 * Uses the PlantUML library to parse and render diagram files.
 * Rendered images are saved to a configurable output directory.
 * </p>
 */
public class UmlRenderer {

    private static final Logger log = LoggerFactory.getLogger(UmlRenderer.class);

    private final File sourceDir;
    private final File outputDir;

    /**
     * Result of rendering a single diagram.
     */
    public static class RenderResult {
        private final String sourceName;
        private final File outputFile;
        private final boolean success;
        private final String message;

        public RenderResult(String sourceName, File outputFile, boolean success, String message) {
            this.sourceName = sourceName;
            this.outputFile = outputFile;
            this.success = success;
            this.message = message;
        }

        public String getSourceName() { return sourceName; }
        public File getOutputFile()   { return outputFile; }
        public boolean isSuccess()    { return success; }
        public String getMessage()    { return message; }
    }

    /**
     * Create a renderer.
     *
     * @param sourceDir directory containing .puml files
     * @param outputDir directory to save PNG images (will be created)
     */
    public UmlRenderer(File sourceDir, File outputDir) {
        this.sourceDir = sourceDir;
        this.outputDir = outputDir;
    }

    /**
     * Render all .puml files in the source directory to PNG in the output directory.
     *
     * @return list of render results
     */
    public List<RenderResult> renderAll() {
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File[] pumlFiles = sourceDir.listFiles(f ->
                f.isFile() && f.getName().endsWith(".puml"));

        if (pumlFiles == null || pumlFiles.length == 0) {
            log.warn("No .puml files found in {}", sourceDir);
            return Collections.emptyList();
        }

        // Sort for deterministic order
        Arrays.sort(pumlFiles, Comparator.comparing(File::getName));

        List<RenderResult> results = new ArrayList<>();
        for (File puml : pumlFiles) {
            results.add(renderFile(puml));
        }

        long ok = results.stream().filter(RenderResult::isSuccess).count();
        log.info("UML rendering complete: {}/{} diagrams rendered to {}",
                ok, results.size(), outputDir);

        return results;
    }

    /**
     * Render a single .puml file to PNG.
     *
     * @param pumlFile the .puml source file
     * @return render result
     */
    public RenderResult renderFile(File pumlFile) {
        String name = pumlFile.getName();
        log.info("Rendering UML diagram: {}", name);

        try {
            SourceFileReader reader = new SourceFileReader(pumlFile, outputDir);
            List<GeneratedImage> images = reader.getGeneratedImages();

            if (images.isEmpty()) {
                return new RenderResult(name, null, false, "No diagrams generated");
            }

            // PlantUML generates files with the diagram name from @startuml
            File generated = images.get(0).getPngFile();
            log.info("  → {} ({} bytes)", generated.getName(), generated.length());

            return new RenderResult(name, generated, true,
                    String.format("%d image(s) generated", images.size()));

        } catch (IOException e) {
            log.error("Failed to render {}: {}", name, e.getMessage());
            return new RenderResult(name, null, false, e.getMessage());
        }
    }

    /**
     * List all .puml files in the source directory.
     */
    public List<String> listDiagrams() {
        File[] files = sourceDir.listFiles(f ->
                f.isFile() && f.getName().endsWith(".puml"));
        if (files == null) return Collections.emptyList();
        Arrays.sort(files, Comparator.comparing(File::getName));
        List<String> names = new ArrayList<>();
        for (File f : files) {
            names.add(f.getName());
        }
        return names;
    }

    public File getSourceDir() { return sourceDir; }
    public File getOutputDir() { return outputDir; }
}
