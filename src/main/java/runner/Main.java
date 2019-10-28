package runner;

import javafx.application.Application;
import javafx.stage.Stage;
import runner.config.SceneService;
import runner.config.UserData;
import runner.config.UserDataRepository;

import java.sql.DriverManager;
import java.sql.SQLException;

public class Main extends Application {
    private SceneService sceneService = new SceneService();
    private UserDataRepository userDataRepository = new UserDataRepository();

    @Override
    public void start(Stage stage) throws Exception {
        final UserData userData = userDataRepository.read();
        boolean successful = true;

        if(userData != null) {
            try {
                DriverManager.getConnection(userData.getConnectionString());
            } catch (SQLException e) {
                successful = false;
            }
        }

        if (userData == null || !successful) {
            sceneService.showScene(SceneService.SCENE.config, stage);
        } else {
            sceneService.showScene(SceneService.SCENE.runner, stage);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
