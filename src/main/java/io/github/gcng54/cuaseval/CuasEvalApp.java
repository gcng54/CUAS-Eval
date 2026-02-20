package io.github.gcng54.cuaseval;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import io.github.gcng54.cuaseval.ui.MainView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CUAS-Eval — Counter-UAS Evaluation Application.
 * <p>
 * JavaFX desktop application implementing the CWA 18150 (COURAGEOUS)
 * Counter-UAS testing and evaluation methodology.
 * </p>
 * <p>
 * Architecture overview:
 * <ul>
 *   <li><b>DTI Pipeline:</b> Detection → Tracking → Identification evaluation</li>
 *   <li><b>Generators:</b> Test targets, environments, and scenarios</li>
 *   <li><b>Evaluator:</b> Metrics computation &amp; requirement compliance</li>
 *   <li><b>Reports:</b> HTML reports and KML (Google Earth) export</li>
 *   <li><b>UI:</b> JavaFX map display, scenario config, results view</li>
 * </ul>
 * </p>
 *
 * @author CUAS-Eval Project
 * @version 1.0
 * @see <a href="https://www.cen.eu">CEN CWA 18150</a>
 */
public class CuasEvalApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(CuasEvalApp.class);

    @Override
    public void start(Stage primaryStage) {
        log.info("    CUAS-Eval v2.0 — CWA 18150 Evaluator   ");
        log.info("    Counter-UAS DTI Performance Evaluation ");
        log.info("    Gokhan Cengiz - 2026                   ");

        // Create main view
        MainView mainView = new MainView();

        // Create scene
        Scene scene = new Scene(mainView, 1400, 900);

        // Configure stage
        primaryStage.setTitle("CUAS-Eval — CWA 18150 Counter-UAS Evaluator. Gökhan Cengiz - 2026");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);

        primaryStage.setOnCloseRequest(e -> {
            log.info("CUAS-Eval shutting down.");
        });

        primaryStage.show();
        log.info("Application started successfully.");
    }

    /**
     * Application entry point.
     * Launches the JavaFX application.
     */
    public static void main(String[] args) {
        log.info("Starting CUAS-Eval...");
        launch(args);
    }
}
