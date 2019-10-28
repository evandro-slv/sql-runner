package runner.config;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class SceneService {
    public enum SCENE {
        config("/config.fxml", "SQL Runner Configuration", 1028, 176),
        runner("/runner.fxml", "SQL Runner", 1043, 915);

        private final String name;
        private final String title;
        private final int width;
        private final int height;

        SCENE(String name, String title, int width, int height) {
            this.name = name;
            this.title = title;
            this.width = width;
            this.height = height;
        }
    }

    public SceneService() {
    }

    public void changeScene(SCENE scene, Node node) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(scene.name));
        Parent root = fxmlLoader.load();
        Stage stage = new Stage();
        stage.setTitle(scene.title);
        stage.setScene(new Scene(root, scene.width, scene.height));
        stage.show();

        Stage thisStage = (Stage) node.getScene().getWindow();
        thisStage.close();
    }

    public void showScene(SCENE scene, Stage stage) throws IOException {
        Parent config = FXMLLoader.load(getClass().getResource(scene.name));
        stage.setTitle(scene.title);
        stage.setScene(new Scene(config, scene.width, scene.height));
        stage.show();
    }
}
