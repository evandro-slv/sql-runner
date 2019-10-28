package runner;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import runner.config.SceneService;
import runner.config.UserData;
import runner.config.UserDataRepository;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Config {
    private UserDataRepository userDataRepository = new UserDataRepository();
    private SceneService sceneService = new SceneService();

    public Button testConnection;
    public Button ok;
    public TextField connectionString;
    public Label success;

    @FXML
    public void initialize() {
        try {
            final UserData userData = userDataRepository.read();

            if(userData != null) {
                connectionString.setText(userData.getConnectionString());
            }
        } catch (ClassNotFoundException ignored) { }
    }

    private void showError(String title, String error) {
        Alert errorAlert = new Alert(Alert.AlertType.ERROR);
        errorAlert.setHeaderText(title);
        errorAlert.setContentText(error);
        errorAlert.showAndWait();
    }

    public void testConnection() {
        try {
            test(connectionString.getText());
            success.setText("Connection successful");
        } catch (Exception e) {
            showError("Error", getStackTrace(e));
        }
    }

    public void ok() {
        try {
            test(connectionString.getText());

            try {
                userDataRepository.save(new UserData(connectionString.getText()));
                sceneService.changeScene(SceneService.SCENE.runner, ok);
            } catch(IOException e) {
                showError("Error", getStackTrace(e));
            }
        } catch (Exception e) {
            showError("Error", getStackTrace(e));
        }
    }

    private void test(String connectionString) throws SQLException {
        DriverManager.getConnection(connectionString);
    }

    private String getStackTrace(Throwable t) {
        try (StringWriter sw = new StringWriter();
             PrintWriter pw = new PrintWriter(sw)) {
            t.printStackTrace(pw);
            return sw.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
