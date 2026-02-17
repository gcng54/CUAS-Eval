package io.github.gcng54.cuaseval.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import io.github.gcng54.cuaseval.report.UmlRenderer;
import io.github.gcng54.cuaseval.report.UmlRenderer.RenderResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * UML diagrams panel — render PlantUML diagrams to PNG and display them.
 * <p>
 * Provides:
 * <ul>
 *   <li>List of available .puml diagrams</li>
 *   <li>Render All button to generate PNG images</li>
 *   <li>Preview of rendered diagrams</li>
 *   <li>Save location: docs/uml_pic/</li>
 * </ul>
 * </p>
 */
public class UmlPanel extends VBox {

    private static final String UML_SOURCE_DIR = "docs/uml";
    private static final String UML_OUTPUT_DIR = "docs/uml_pic";

    private final ListView<String> diagramList;
    private final Button renderAllButton;
    private final Button renderSelectedButton;
    private final Label statusLabel;
    private final ProgressBar progressBar;
    private final ScrollPane previewPane;
    private final ImageView previewImage;

    private final UmlRenderer renderer;

    public UmlPanel() {
        setSpacing(8);
        setPadding(new Insets(10));

        renderer = new UmlRenderer(new File(UML_SOURCE_DIR), new File(UML_OUTPUT_DIR));

        // Title
        Label title = new Label("UML Diagrams");
        title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1a237e;");

        // Diagram list
        diagramList = new ListView<>();
        diagramList.setPrefHeight(150);
        diagramList.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        diagramList.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, sel) -> showPreview(sel));
        refreshDiagramList();

        // Render buttons
        renderAllButton = new Button("Render All Diagrams");
        renderAllButton.setMaxWidth(Double.MAX_VALUE);
        renderAllButton.setStyle("-fx-base: #2196F3; -fx-text-fill: white;");
        renderAllButton.setOnAction(e -> renderAll());

        renderSelectedButton = new Button("Render Selected");
        renderSelectedButton.setMaxWidth(Double.MAX_VALUE);
        renderSelectedButton.setOnAction(e -> renderSelected());

        // Button to open output folder
        Button openFolderButton = new Button("Open Output Folder");
        openFolderButton.setMaxWidth(Double.MAX_VALUE);
        openFolderButton.setOnAction(e -> openOutputFolder());

        // Progress
        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setVisible(false);

        // Status
        statusLabel = new Label(String.format(Locale.ENGLISH,
                "%d diagrams found. Output: %s", renderer.listDiagrams().size(), UML_OUTPUT_DIR));
        statusLabel.setWrapText(true);
        statusLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

        // Preview
        previewImage = new ImageView();
        previewImage.setPreserveRatio(true);
        previewImage.setSmooth(true);

        previewPane = new ScrollPane(previewImage);
        previewPane.setFitToWidth(true);
        previewPane.setPrefHeight(300);
        previewPane.setStyle("-fx-background-color: white;");

        Label previewLabel = new Label("Preview:");
        previewLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #333;");

        // Layout
        HBox buttonBox = new HBox(4, renderAllButton, renderSelectedButton);
        HBox.setHgrow(renderAllButton, Priority.ALWAYS);
        HBox.setHgrow(renderSelectedButton, Priority.ALWAYS);
        renderAllButton.setMaxWidth(Double.MAX_VALUE);
        renderSelectedButton.setMaxWidth(Double.MAX_VALUE);

        getChildren().addAll(
                title,
                new Separator(),
                label("Available Diagrams:"),
                diagramList,
                buttonBox,
                openFolderButton,
                new Separator(),
                progressBar,
                statusLabel,
                new Separator(),
                previewLabel,
                previewPane
        );
    }

    private void refreshDiagramList() {
        List<String> diagrams = renderer.listDiagrams();
        diagramList.getItems().setAll(diagrams);
    }

    private void renderAll() {
        renderAllButton.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(-1);
        statusLabel.setText("Rendering all diagrams...");

        Thread thread = new Thread(() -> {
            List<RenderResult> results = renderer.renderAll();

            Platform.runLater(() -> {
                progressBar.setProgress(1);
                progressBar.setVisible(false);
                renderAllButton.setDisable(false);

                long ok = results.stream().filter(RenderResult::isSuccess).count();
                long fail = results.size() - ok;

                StringBuilder sb = new StringBuilder();
                sb.append(String.format(Locale.ENGLISH,
                        "Rendered %d/%d diagrams to %s\n", ok, results.size(), UML_OUTPUT_DIR));
                for (RenderResult r : results) {
                    sb.append(String.format(Locale.ENGLISH, "  %s %s: %s\n",
                            r.isSuccess() ? "\u2713" : "\u2717",
                            r.getSourceName(), r.getMessage()));
                }
                statusLabel.setText(sb.toString().trim());

                // Auto-show first rendered image
                if (!results.isEmpty() && results.get(0).isSuccess()) {
                    String firstName = results.get(0).getSourceName();
                    diagramList.getSelectionModel().select(firstName);
                }
            });
        }, "uml-renderer");
        thread.setDaemon(true);
        thread.start();
    }

    private void renderSelected() {
        String selected = diagramList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            statusLabel.setText("Select a diagram first.");
            return;
        }

        renderSelectedButton.setDisable(true);
        statusLabel.setText("Rendering " + selected + "...");

        Thread thread = new Thread(() -> {
            File pumlFile = new File(renderer.getSourceDir(), selected);
            RenderResult result = renderer.renderFile(pumlFile);

            Platform.runLater(() -> {
                renderSelectedButton.setDisable(false);
                statusLabel.setText(String.format(Locale.ENGLISH, "%s %s: %s",
                        result.isSuccess() ? "\u2713" : "\u2717",
                        result.getSourceName(), result.getMessage()));

                if (result.isSuccess()) {
                    loadPreviewImage(result.getOutputFile());
                }
            });
        }, "uml-renderer-single");
        thread.setDaemon(true);
        thread.start();
    }

    private void showPreview(String pumlName) {
        if (pumlName == null) return;

        // Try to find the rendered PNG
        String baseName = pumlName.replace(".puml", "");
        File pngFile = new File(renderer.getOutputDir(), baseName + ".png");

        if (pngFile.exists()) {
            loadPreviewImage(pngFile);
        } else {
            previewImage.setImage(null);
            statusLabel.setText("Not yet rendered. Click 'Render Selected' or 'Render All'.");
        }
    }

    private void loadPreviewImage(File pngFile) {
        try (FileInputStream fis = new FileInputStream(pngFile)) {
            Image img = new Image(fis);
            previewImage.setImage(img);
            previewImage.setFitWidth(Math.min(img.getWidth(), 800));
        } catch (IOException e) {
            statusLabel.setText("Failed to load preview: " + e.getMessage());
        }
    }

    private void openOutputFolder() {
        File dir = renderer.getOutputDir();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            // Open folder in system file manager
            Runtime.getRuntime().exec(new String[]{"explorer.exe", dir.getAbsolutePath()});
        } catch (IOException e) {
            statusLabel.setText("Cannot open folder: " + e.getMessage());
        }
    }

    private Label label(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 11px; -fx-text-fill: #333;");
        return l;
    }
}
