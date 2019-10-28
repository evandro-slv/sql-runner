package runner;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.util.Duration;
import javafx.util.StringConverter;
import runner.config.SceneService;
import runner.config.UserData;
import runner.config.UserDataRepository;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Controller {
    private UserDataRepository userDataRepository = new UserDataRepository();
    private SceneService sceneService = new SceneService();

    public TextArea query;
    public TextArea result;
    public Button start;
    public Button clearChart;
    public Spinner<Integer> numRequests;
//    public Spinner<Integer> numThreads;
    public TextField elapsedTime;
    public TextField numCompleted;
    public TextField executionTime;
    public TextField cpuTime;
    public LineChart executionTimeChart;
    public LineChart cpuTimeChart;

    private Timeline timeline = new Timeline();
    private Instant startTime;
    private boolean isRunning = false;

    private float avgCpuTime = 0;
    private float avgExecutionTime = 0;

    private Connection conn;
    Task<Void> task;

    public Controller() {
        try {
            conn = connect();
        } catch (ClassNotFoundException | SQLException | IOException e) {
            e.printStackTrace();

            try (StringWriter sw = new StringWriter();
                 PrintWriter pw = new PrintWriter(sw)) {
                e.printStackTrace(pw);

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Connection error");
                alert.setHeaderText(null);
                alert.setContentText(sw.toString());
                alert.show();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    @FXML
    public void initialize() {
        numRequests.focusedProperty().addListener((s, ov, nv) -> {
            if (nv) return;
            commitEditorText(numRequests);
        });
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

    @FXML
    protected void clearChart() {
        executionTimeChart.getData().clear();
        cpuTimeChart.getData().clear();
    }

    @FXML
    protected void action() {
        if(isRunning) {
            start.setText("Start");
            start.setStyle("-fx-font-weight: normal");
            numRequests.setDisable(false);
//            numThreads.setDisable(false);
            query.setDisable(false);

            timeline.stop();

            task.cancel();
        } else {
            start.setText("Stop");
            start.setStyle("-fx-font-weight: bold");
            numRequests.setDisable(true);
//            numThreads.setDisable(true);
            query.setDisable(true);

            startTime = Instant.now();

            timeline.setCycleCount(Timeline.INDEFINITE);
            timeline.getKeyFrames().setAll(new KeyFrame(Duration.millis(50), e -> updateValues()));
            timeline.play();

            runQuery();
        }

        isRunning = !isRunning;
    }

    private Connection connect() throws ClassNotFoundException, SQLException, IOException {
        final UserData userData = userDataRepository.read();
        conn = DriverManager.getConnection(userData.getConnectionString());
        return conn;
    }

    private void updateValues() {
        Instant endTime = Instant.now();

        long hours = ChronoUnit.HOURS.between(startTime, endTime);
        long minutes = ChronoUnit.MINUTES.between(startTime, endTime) % 60;
        long seconds = ChronoUnit.SECONDS.between(startTime, endTime) % 60;
        long milis = ChronoUnit.MILLIS.between(startTime, endTime) % 1000;

        elapsedTime.setText(String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milis));
    }

    private void runQuery() {
        String splitChar = "#@#@#";
        String sql = query.getText();

        numCompleted.setText("0");
        result.setText("");

        avgCpuTime = 0;
        avgExecutionTime = 0;

        // chart data
        XYChart.Series series = new XYChart.Series();
        series.setName(query.getText());
        executionTimeChart.getData().add(series);

        XYChart.Series seriesCpu = new XYChart.Series();
        seriesCpu.setName(query.getText());
        cpuTimeChart.getData().add(seriesCpu);

        task = new Task<Void>() {
            @Override protected Void call() {
                int num = 1;
                updateMessage(String.valueOf(num));

                try (Statement statement = conn.createStatement()) {
                    statement.execute("SET STATISTICS IO ON; SET STATISTICS TIME ON");
                } catch (SQLException e) {
                    updateMessage(num + splitChar + getStackTrace(e));
                }

                boolean canceled = false;

                while(num <= numRequests.getValue()) {
                    if (isCancelled()) {
                        canceled = true;
                        break;
                    }

                    try (Statement statement = conn.createStatement()) {
                        statement.execute(sql);

                        while(statement.getMoreResults()) {}

                        SQLWarning warning = statement.getWarnings();
                        String warnings = "";

                        while (warning != null) {
                            warnings += warning.getMessage() + "|>\n";
                            warning = warning.getNextWarning();
                        }

                        updateMessage((num++) + splitChar + warnings);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        updateMessage("ERROR" + splitChar + getStackTrace(e));
                        break;
                    }
                }

                if (!canceled) {
                    action();
                }

                try (Statement statement = conn.createStatement()) {
                    statement.execute("SET STATISTICS IO OFF; SET STATISTICS TIME OFF");
                } catch (SQLException e) {
                    updateMessage(num + splitChar + getStackTrace(e));
                }

                return null;
            }
        };

        task.messageProperty().addListener((obs, oldMessage, newMessage) -> {
            String[] results = newMessage.split(splitChar);

            if(results.length >= 1) numCompleted.setText(results[0]);
            if(results.length >= 2) {
                result.setText(result.getText() + "\n" + results[1]);

                if(results[1].contains("SQL Server Execution Times:")) {
                    Pattern pattern = Pattern.compile("CPU time = (\\d+) ms,\\s\\selapsed time = (\\d+) ms");
                    Matcher matcher = pattern.matcher(results[1].split("SQL Server Execution Times:")[1]);

                    if (matcher.find()) {
                        avgCpuTime += Integer.parseInt(matcher.group(1));
                        avgExecutionTime += Integer.parseInt(matcher.group(2));

                        cpuTime.setText(String.format("%.3f", avgCpuTime / Integer.parseInt(results[0])));
                        executionTime.setText(String.format("%.3f", avgExecutionTime / Integer.parseInt(results[0])));
                    }
                }

                series.getData().add(new XYChart.Data(Float.parseFloat(results[0]), avgCpuTime / Integer.parseInt(results[0])));
                seriesCpu.getData().add(new XYChart.Data(Float.parseFloat(results[0]), avgExecutionTime / Integer.parseInt(results[0])));
            }
        });

        final Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    public void cleanBuffers(ActionEvent actionEvent) {
        try (Statement statement = conn.createStatement()) {
            statement.execute("CHECKPOINT;");
            statement.execute("DBCC DROPCLEANBUFFERS;");

            SQLWarning warning = statement.getWarnings();
            String warnings = "";

            while (warning != null) {
                warnings += warning.getMessage() + "|>\n";
                warning = warning.getNextWarning();
            }

            result.setText(result.getText() + "\n" + warnings);
        } catch (SQLException e) {
            result.setText(result.getText() + "\n" + getStackTrace(e));
        }
    }

    public void freeCache() {
        try (Statement statement = conn.createStatement()) {
            statement.execute("CHECKPOINT;");
            statement.execute("DBCC FREEPROCCACHE;");

            SQLWarning warning = statement.getWarnings();
            String warnings = "";

            while (warning != null) {
                warnings += warning.getMessage() + "|>\n";
                warning = warning.getNextWarning();
            }

            result.setText(result.getText() + "\n" + warnings);
        } catch (SQLException e) {
            result.setText(result.getText() + "\n" + getStackTrace(e));
        }
    }

    // Fix spinner text change
    private <T> void commitEditorText(Spinner<T> spinner) {
        if (!spinner.isEditable()) return;
        String text = spinner.getEditor().getText();
        SpinnerValueFactory<T> valueFactory = spinner.getValueFactory();
        if (valueFactory != null) {
            StringConverter<T> converter = valueFactory.getConverter();
            if (converter != null) {
                T value = converter.fromString(text);
                valueFactory.setValue(value);
            }
        }
    }

    public void configure() throws IOException {
        sceneService.changeScene(SceneService.SCENE.config, start);
    }
}
