import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.fazecast.jSerialComm.SerialPort;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.chart.LineChart; // <--- Import for LineChart
import javafx.scene.chart.NumberAxis; // <--- Import for LineChart Axis
import javafx.scene.chart.XYChart; // <--- Import for LineChart Series


public class Main extends Application {

    // Sensor labels for three sensors
    private Label temperatureLabel1, humidityLabel1, mq135Label1, voltageLabel1, sensor1Label;
    private String latestTemperature1 = "";
    private String latestHumidity1 = "";
    private String latestMq135Value1 = "";
    private String latestVoltage1 = "";
    private Label temperatureLabel2, humidityLabel2, mq135Label2, voltageLabel2, sensor2Label;
    private String latestTemperature2 = "";
    private String latestHumidity2 = "";
    private String latestMq135Value2 = "";
    private String latestVoltage2 = "";
    private Label temperatureLabel3, humidityLabel3, mq135Label3, voltageLabel3, sensor3Label;
    private String latestTemperature3 = "";
    private String latestHumidity3 = "";
    private String latestMq135Value3 = "";
    private String latestVoltage3 = "";
    // Create buttons for LIB, SES, and SCE
    Button libButton = new Button("LIB");
    Button sesButton = new Button("SES");
    Button sceButton = new Button("SCE");
    private Label dateLabel, timeLabel;
    private TextArea logTextArea;
    // Scenes for the application
    private Scene welcomeScene, heatmapScene, sensorDataScene, logScene;
    private Stage mainStage;
    private volatile boolean running = true;
    private boolean isCelsius = true;
    private double lastTemperatureCelsius1 = 0.0, lastTemperatureCelsius2 = 0.0, lastTemperatureCelsius3 = 0.0;
    private double lastMQ135Val1 = 0.0, lastMQ135Val2 = 0.0, lastMQ135Val3 = 0.0;
    private Scene graphScene;
    private LineChart<Number, Number> lineChart;
    private XYChart.Series<Number, Number> temperatureSeries1;
    private XYChart.Series<Number, Number> temperatureSeries2;
    private XYChart.Series<Number, Number> temperatureSeries3;
    private LineChart<Number, Number> airQualityChart;
    private XYChart.Series<Number, Number> airQualitySeries1;
    private XYChart.Series<Number, Number> airQualitySeries2;
    private XYChart.Series<Number, Number> airQualitySeries3;
    private int timeElapsed = 0;
    private NumberAxis xAxis = new NumberAxis();
    private boolean showingAirQuality = false;

    @Override
    public void start(Stage primaryStage) {
        mainStage = primaryStage;
        primaryStage.setTitle("Air Map");

        dateLabel = new Label();
        timeLabel = new Label();
        updateDateTime();

        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateDateTime()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        Timeline timeline2 = new Timeline(new KeyFrame(Duration.seconds(3), e -> updateButtonColors()));
        timeline2.setCycleCount(Timeline.INDEFINITE);
        timeline2.play();

        prepareWelcomeScreen();
        prepareHeatmapScreen();
        prepareSensorDataScreen();
        prepareLogScreen();
        prepareGraphScene();

        primaryStage.setScene(welcomeScene);
        primaryStage.show();
    }

    private void prepareWelcomeScreen() {
        // Create a label for the welcome message at the top
        Label welcomeLabel = new Label("Welcome to Air Map");
        welcomeLabel.setStyle("-fx-font-size: 34; -fx-padding: 10;");
        welcomeLabel.setAlignment(Pos.TOP_CENTER);

        // Create date and time labels for the bottom horizontal box
        dateLabel.setStyle("-fx-padding: 5; -fx-font-size: 16;");
        timeLabel.setStyle("-fx-padding: 5; -fx-font-size: 16;");
        HBox footerLayout = new HBox(5, dateLabel, timeLabel);
        footerLayout.setAlignment(Pos.CENTER);

        // Button in the middle of the screen
        Button startButton = new Button("See Data ->");
        startButton.setOnAction(e -> mainStage.setScene(heatmapScene));
        startButton.setStyle("-fx-padding: 8;");

        // Set up the background image
        Image backgroundImage = new Image(getClass().getResource("/UIC_Trees.jpg").toExternalForm());
        ImageView imageView = new ImageView(backgroundImage);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(mainStage.getWidth());
        imageView.setFitHeight(mainStage.getHeight());

        // Main layout to stack image, welcome label, center button, and footer layout
        StackPane welcomeRoot = new StackPane();
        VBox mainLayout = new VBox(10, startButton);
        VBox mainLayout2 = new VBox(10, welcomeLabel, footerLayout);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.setTranslateY(110);
        startButton.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-padding: 12 18; -fx-font-size: 22; -fx-font-weight: bold");
        mainLayout2.setAlignment(Pos.TOP_CENTER);

        // Position welcomeLabel at the top and footer at the bottom
        StackPane.setAlignment(welcomeLabel, Pos.TOP_CENTER);
        StackPane.setAlignment(footerLayout, Pos.CENTER);

        // Add elements to root
        welcomeRoot.getChildren().addAll(imageView, mainLayout2, mainLayout);

        // Set up the scene
        welcomeScene = new Scene(welcomeRoot, 800, 550);

        // Ensure the image resizes with the window
        mainStage.widthProperty().addListener((obs, oldVal, newVal) -> imageView.setFitWidth(newVal.doubleValue()));
        mainStage.heightProperty().addListener((obs, oldVal, newVal) -> imageView.setFitHeight(newVal.doubleValue()));
    }

    private void prepareHeatmapScreen() {
        // Load the heat map image
        Image heatmapImage = new Image(getClass().getResource("/UIC_HeatMap.png").toExternalForm());
        ImageView heatmapImageView = new ImageView(heatmapImage);
        heatmapImageView.setPreserveRatio(true);
        heatmapImageView.setFitWidth(700);
        heatmapImageView.setFitHeight(450);

        // Create the label and position it above the image
        Label heatmapLabel = new Label("Heatmap of UIC East Campus");
        heatmapLabel.setStyle("-fx-font-size: 16; -fx-text-fill: white; -fx-padding: 5; -fx-font-weight: bold;");

        Label firstLabel = new Label("Sensor 1 (Library)");
        Label secondLabel = new Label("Sensor 2 (Student Center East)");
        Label thirdLabel = new Label("Sensor 3 (Science & Engineering South)");

        // Style the sensor labels with semi-transparent background
        String labelStyle = "-fx-background-color: rgba(255, 255, 255, 0.8); -fx-padding: 5; -fx-background-radius: 5;";
        firstLabel.setStyle(labelStyle);
        secondLabel.setStyle(labelStyle);
        thirdLabel.setStyle(labelStyle);

        // Style the location buttons with hover effect
        String locationButtonBaseStyle = "-fx-font-size: 14; -fx-background-color: #4a90e2; -fx-text-fill: white; " +
                "-fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;";
        String locationButtonHoverStyle = "-fx-font-size: 14; -fx-background-color: #357abd; -fx-text-fill: white; " +
                "-fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);";

        libButton.setStyle(locationButtonBaseStyle);
        sesButton.setStyle(locationButtonBaseStyle);
        sceButton.setStyle(locationButtonBaseStyle);

        // Add hover effects for location buttons
        libButton.setOnMouseEntered(e -> libButton.setStyle(locationButtonHoverStyle));
        libButton.setOnMouseExited(e -> libButton.setStyle(locationButtonBaseStyle));
        sesButton.setOnMouseEntered(e -> sesButton.setStyle(locationButtonHoverStyle));
        sesButton.setOnMouseExited(e -> sesButton.setStyle(locationButtonBaseStyle));
        sceButton.setOnMouseEntered(e -> sceButton.setStyle(locationButtonHoverStyle));
        sceButton.setOnMouseExited(e -> sceButton.setStyle(locationButtonBaseStyle));

        // Set actions for the location buttons
        libButton.setOnAction(e -> showPopup("LIB", firstSensor()));
        sceButton.setOnAction(e -> showPopup("SCE", secondSensor()));
        sesButton.setOnAction(e -> showPopup("SES", thirdSensor()));

        // Style the navigation buttons with hover effect
        Button toSensorData = new Button("View Sensor Data");
        Button toLog = new Button("View Log");
        Button toGraph = new Button("View Graph");

        String navButtonBaseStyle = "-fx-background-color: #DC143C; -fx-text-fill: white; -fx-font-size: 14; " +
                "-fx-padding: 10 20; -fx-background-radius: 5; -fx-cursor: hand;";
        String navButtonHoverStyle = "-fx-background-color: #b30000; -fx-text-fill: white; -fx-font-size: 14; " +
                "-fx-padding: 10 20; -fx-background-radius: 5; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);";

        String logButtonBaseStyle = "-fx-background-color: #00FFFF; -fx-text-fill: black; -fx-font-size: 14; " +
                "-fx-padding: 10 20; -fx-background-radius: 5; -fx-cursor: hand;";
        String logButtonHoverStyle = "-fx-background-color: #00cccc; -fx-text-fill: black; -fx-font-size: 14; " +
                "-fx-padding: 10 20; -fx-background-radius: 5; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 2);";

        toSensorData.setStyle(navButtonBaseStyle);
        toLog.setStyle(logButtonBaseStyle);

        // Add hover effects for navigation buttons
        toSensorData.setOnMouseEntered(e -> toSensorData.setStyle(navButtonHoverStyle));
        toSensorData.setOnMouseExited(e -> toSensorData.setStyle(navButtonBaseStyle));
        toLog.setOnMouseEntered(e -> toLog.setStyle(logButtonHoverStyle));
        toLog.setOnMouseExited(e -> toLog.setStyle(logButtonBaseStyle));

        // Set actions for navigation buttons
        toSensorData.setOnAction(e -> mainStage.setScene(sensorDataScene));
        toLog.setOnAction(e -> mainStage.setScene(logScene));
        toGraph.setOnAction(e -> mainStage.setScene(graphScene));

        // Layout arrangement remains the same
        HBox leftButton = new HBox(10, firstLabel, libButton);
        leftButton.setAlignment(Pos.CENTER);
        VBox leftLayout = new VBox(10, leftButton);
        leftLayout.setAlignment(Pos.CENTER_LEFT);
        leftLayout.setTranslateX(-275);
        leftLayout.setTranslateY(15);
        leftLayout.setPadding(new Insets(0, 10, 0, 10));

        HBox rightHLsce = new HBox(10, sceButton, secondLabel);
        rightHLsce.setAlignment(Pos.CENTER);
        rightHLsce.setTranslateY(-47);

        HBox rightHLses = new HBox(10, sesButton, thirdLabel);
        rightHLses.setAlignment(Pos.CENTER);
        rightHLses.setTranslateY(155);

        VBox rightLayout = new VBox(10, leftLayout, rightHLsce, rightHLses);
        rightLayout.setAlignment(Pos.CENTER_RIGHT);
        rightLayout.setTranslateX(160);
        rightLayout.setPadding(new Insets(0, 10, 0, 10));

        HBox bottomLayout = new HBox(10, toSensorData, toLog, toGraph);
        bottomLayout.setAlignment(Pos.CENTER);
        bottomLayout.setPadding(new Insets(20, 0, 20, 0));

        BorderPane borderPane = new BorderPane();
        borderPane.setStyle("-fx-background-color: linear-gradient(to bottom, #2c3e50, #3498db);");
        borderPane.setTop(heatmapLabel);
        StackPane mainStackPane = new StackPane();
        mainStackPane.getChildren().addAll(heatmapImageView, rightLayout);
        borderPane.setCenter(mainStackPane);
        borderPane.setBottom(bottomLayout);
        BorderPane.setAlignment(heatmapLabel, Pos.CENTER);
        BorderPane.setAlignment(bottomLayout, Pos.CENTER);

        heatmapScene = new Scene(borderPane, 800, 550);
    }

    // Method to show a popup
    private void showPopup(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);

        // Set title and content
        alert.setTitle(title);
        alert.setHeaderText(null);

        // Custom DialogPane with increased width
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setMinWidth(400); // Adjust width for better appearance
        dialogPane.setStyle(
                "-fx-background-color: linear-gradient(to bottom, #BBDEFB, #64B5F6);"
                        + "-fx-padding: 20px;"
                        + "-fx-border-color: #7E57C2;"
                        + "-fx-border-width: 2px;"
                        + "-fx-border-radius: 10px;"
                        + "-fx-background-radius: 10px;"
        );

        // Customize the text message and style
        Label contentLabel = new Label(message);
        contentLabel.setStyle(
                "-fx-font-size: 16px;"
                        + "-fx-text-fill: #311B92;"
                        + "-fx-font-weight: bold;"
                        + "-fx-wrap-text: true;"
        );
        contentLabel.setWrapText(true); // Ensures text wraps within the dialog
        contentLabel.setPrefWidth(350); // Sets the content width

        dialogPane.setContent(contentLabel);

        // Show the alert
        alert.showAndWait();
    }

    private void prepareSensorDataScreen() {
        temperatureLabel1 = new Label("Temperature (Sensor 1): ");
        humidityLabel1 = new Label("Humidity (Sensor 1): ");
        mq135Label1 = new Label("MQ135 Sensor Value (Sensor 1): ");
        voltageLabel1 = new Label("Voltage (Sensor 1): ");
        sensor1Label = new Label("Sensor 1");
        VBox sensorOne = new VBox(10, temperatureLabel1, humidityLabel1, mq135Label1, voltageLabel1, sensor1Label);
        sensorOne.setAlignment(Pos.TOP_LEFT);
        sensorOne.setStyle("-fx-background-color: #FFFACD");

        temperatureLabel2 = new Label("Temperature (Sensor 2): ");
        humidityLabel2 = new Label("Humidity (Sensor 2): ");
        mq135Label2 = new Label("MQ135 Sensor Value (Sensor 2): ");
        voltageLabel2 = new Label("Voltage (Sensor 2): ");
        sensor2Label = new Label("Sensor 2");
        VBox sensortwo = new VBox(10, temperatureLabel2, humidityLabel2, mq135Label2, voltageLabel2, sensor2Label);
        sensortwo.setStyle("-fx-background-color: #FFFACD");

        temperatureLabel3 = new Label("Temperature (Sensor 3): ");
        humidityLabel3 = new Label("Humidity (Sensor 3): ");
        mq135Label3 = new Label("MQ135 Sensor Value (Sensor 3): ");
        voltageLabel3 = new Label("Voltage (Sensor 3): ");
        sensor3Label = new Label("Sensor 3");
        VBox sensorthree = new VBox(10, temperatureLabel3, humidityLabel3, mq135Label3, voltageLabel3, sensor3Label);
        sensorthree.setStyle("-fx-background-color: #FFFACD");

        HBox sensorDataLayout = new HBox(90, sensorOne, sensortwo, sensorthree);
        sensorDataLayout.setPadding(new Insets(10));
        sensorDataLayout.setAlignment(Pos.TOP_CENTER);

        Button unitChangeButton = new Button("Change Units");
        unitChangeButton.setStyle("-fx-background-color: #DC143C");
        unitChangeButton.setOnAction(e -> toggleTemperatureUnit());

        Button toHeatmap = new Button("View Heatmap");
        toHeatmap.setStyle("-fx-background-color: #FAEBD7");
        Button toLog = new Button("View Log");
        toLog.setStyle("-fx-background-color: #00FFFF");
        toHeatmap.setOnAction(e -> mainStage.setScene(heatmapScene));
        toLog.setOnAction(e -> mainStage.setScene(logScene));


        HBox navigation = new HBox(10, unitChangeButton, toHeatmap, toLog);
        navigation.setAlignment(Pos.CENTER);

        VBox layout = new VBox(10, sensorDataLayout, navigation);
        layout.setAlignment(Pos.CENTER);

        sensorDataScene = new Scene(layout, 800, 550);

        new Thread(() -> startArduinoCommunication("COM6")).start(); // Start data reading
    }

    private void prepareLogScreen() {
        Label logLabel = new Label("Log Screen - Sensor Data Transmission Log");
        logLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #37474F;");

        // Log area with styling to preserve data display
        logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setWrapText(true);
        logTextArea.setPrefHeight(350); // Adjust for desired height
        logTextArea.setStyle(
                "-fx-control-inner-background: #FAFAFA; -fx-border-color: #B0BEC5; " +
                        "-fx-border-radius: 5; -fx-padding: 8; -fx-font-size: 14; -fx-font-family: 'Consolas';"
        );

        // Improved button styling
        Button toHeatmap = new Button("View Heatmap");
        toHeatmap.setStyle(
                "-fx-background-color: linear-gradient(#64B5F6, #1976D2); -fx-text-fill: white; " +
                        "-fx-background-radius: 8; -fx-padding: 8 16; -fx-font-size: 14;"
        );
        Button toSensorData = new Button("View Sensor Data");
        toSensorData.setStyle(
                "-fx-background-color: linear-gradient(#FF8A80, #D32F2F); -fx-text-fill: white; " +
                        "-fx-background-radius: 8; -fx-padding: 8 16; -fx-font-size: 14;"
        );

        // Navigation button actions
        toHeatmap.setOnAction(e -> mainStage.setScene(heatmapScene));
        toSensorData.setOnAction(e -> mainStage.setScene(sensorDataScene));

        // Layout for the buttons
        HBox buttonLayout = new HBox(15, toHeatmap, toSensorData);
        buttonLayout.setAlignment(Pos.CENTER);
        buttonLayout.setPadding(new Insets(10));

        // Main layout for the log screen
        VBox layout = new VBox(15, logLabel, logTextArea, buttonLayout);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(15));
        layout.setStyle("-fx-background-color: #E3F2FD;"); // Light blue background for the layout

        redirectSystemOutput();
        logScene = new Scene(layout, 800, 550);
    }

    private void redirectSystemOutput() {
        OutputStream out = new OutputStream() {
            @Override
            public void write(int b) {
                Platform.runLater(() -> logTextArea.appendText(String.valueOf((char) b)));
            }
            @Override
            public void write(byte[] b, int off, int len) {
                String text = new String(b, off, len);
                Platform.runLater(() -> logTextArea.appendText(text));
            }
        };
        System.setOut(new PrintStream(out, true));
        System.setErr(new PrintStream(out, true));
    }

    private void startArduinoCommunication(String portName) {
        SerialPort serialPort = SerialPort.getCommPort(portName);
        serialPort.setBaudRate(9600);

        if (serialPort.openPort()) {
            System.out.println("Port opened: " + portName);
            try (Scanner scanner = new Scanner(serialPort.getInputStream())) {
                while (running) {
                    if (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        System.out.println("Received: " + line);
                        updateUI(line);
                    } else {
                        System.out.println("No data received.");
                    }
                    Thread.sleep(1000);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                serialPort.closePort();
            }
        } else {
            System.out.println("Failed to open port: " + portName);
        }
    }

    private void updateUI(String line) {
        Platform.runLater(() -> {
            if (line.startsWith("Sensor 1")) {
                // Extract the sensor data from the line for Sensor 1
                String[] parts = line.split(",");
                latestTemperature1 = parts[1].split(":")[1].trim();
                latestHumidity1 = parts[2].split(":")[1].trim();
                latestMq135Value1 = parts[3].split(":")[1].trim();
                latestVoltage1 = parts[4].split(":")[1].trim();

                lastTemperatureCelsius1 = Double.parseDouble(latestTemperature1);
                lastMQ135Val1 = Double.parseDouble(latestMq135Value1);
                temperatureLabel1.setText("Temperature: " + (isCelsius ? String.format("%.2f", lastTemperatureCelsius1) + " °C" : String.format("%.2f", (lastTemperatureCelsius1 * 9 / 5) + 32) + " °F"));
                humidityLabel1.setText("Humidity: " + latestHumidity1 + " %");
                mq135Label1.setText("MQ135 Sensor Value: " + latestMq135Value1);
                voltageLabel1.setText("Voltage: " + latestVoltage1);
            }

            if (line.startsWith("Sensor 2")) {
                // Extract the sensor data from the line for Sensor 2
                String[] parts = line.split(",");
                latestTemperature2 = parts[1].split(":")[1].trim();
                latestHumidity2 = parts[2].split(":")[1].trim();
                latestMq135Value2 = parts[3].split(":")[1].trim();
                latestVoltage2 = parts[4].split(":")[1].trim();

                lastTemperatureCelsius2 = Double.parseDouble(latestTemperature2);
                lastMQ135Val2 = Double.parseDouble(latestMq135Value2);
                temperatureLabel2.setText("Temperature: " + (isCelsius ? String.format("%.2f", lastTemperatureCelsius2) + " °C" : String.format("%.2f", (lastTemperatureCelsius2 * 9 / 5) + 32) + " °F"));
                humidityLabel2.setText("Humidity: " + latestHumidity2 + " %");
                mq135Label2.setText("MQ135 Sensor Value: " + latestMq135Value2);
                voltageLabel2.setText("Voltage: " + latestVoltage2);
            }

            if (line.startsWith("Sensor 3")) {
                // Extract the sensor data from the line for Sensor 3
                String[] parts = line.split(",");
                latestTemperature3 = parts[1].split(":")[1].trim();
                latestHumidity3 = parts[2].split(":")[1].trim();
                latestMq135Value3 = parts[3].split(":")[1].trim();
                latestVoltage3 = parts[4].split(":")[1].trim();

                lastTemperatureCelsius3 = Double.parseDouble(latestTemperature3);
                lastMQ135Val3 = Double.parseDouble(latestMq135Value3);
                temperatureLabel3.setText("Temperature: " + (isCelsius ? String.format("%.2f", lastTemperatureCelsius3) + " °C" : String.format("%.2f", (lastTemperatureCelsius3 * 9 / 5) + 32) + " °F"));
                humidityLabel3.setText("Humidity: " + latestHumidity3 + " %");
                mq135Label3.setText("MQ135 Sensor Value: " + latestMq135Value3);
                voltageLabel3.setText("Voltage: " + latestVoltage3);
            }
            updateGraphScene();
        });
    }

    private void toggleTemperatureUnit() {
        isCelsius = !isCelsius;
        updateUIWithCurrentTemperatures();
    }

    private void updateUIWithCurrentTemperatures() {
        Platform.runLater(() -> {
            if (isCelsius) {
                // Display temperatures in Celsius for all sensors
                temperatureLabel1.setText("Temperature: " + String.format("%.2f", lastTemperatureCelsius1) + " °C");
                temperatureLabel2.setText("Temperature: " + String.format("%.2f", lastTemperatureCelsius2) + " °C");
                temperatureLabel3.setText("Temperature: " + String.format("%.2f", lastTemperatureCelsius3) + " °C");
            } else {
                // Convert to Fahrenheit and display for all sensors
                double lastTemperatureFahrenheit1 = (lastTemperatureCelsius1 * 9 / 5) + 32;
                double lastTemperatureFahrenheit2 = (lastTemperatureCelsius2 * 9 / 5) + 32;
                double lastTemperatureFahrenheit3 = (lastTemperatureCelsius3 * 9 / 5) + 32;

                temperatureLabel1.setText("Temperature: " + String.format("%.2f", lastTemperatureFahrenheit1) + " °F");
                temperatureLabel2.setText("Temperature: " + String.format("%.2f", lastTemperatureFahrenheit2) + " °F");
                temperatureLabel3.setText("Temperature: " + String.format("%.2f", lastTemperatureFahrenheit3) + " °F");
            }
        });
    }


    private void updateDateTime() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
        dateLabel.setText(dateFormat.format(new Date()));
        timeLabel.setText(timeFormat.format(new Date()));
    }

    private String firstSensor() {
        return "Temperature: " + latestTemperature1 + " °C, \n"
                + "Humidity: " + latestHumidity1 + " , \n"
                + "MQ135 Sensor Value: " + latestMq135Value1 + ", \n"
                + "Voltage: " + latestVoltage1;
    }

    private String secondSensor() {
        return "Temperature: " + latestTemperature2 + " °C, \n"
                + "Humidity: " + latestHumidity2 + " , \n"
                + "MQ135 Sensor Value: " + latestMq135Value2 + ", \n"
                + "Voltage: " + latestVoltage2;
    }

    private String thirdSensor() {
        return "Temperature: " + latestTemperature3 + " °C, \n"
                + "Humidity: " + latestHumidity3 + " , \n"
                + "MQ135 Sensor Value: " + latestMq135Value3 + ", \n"
                + "Voltage: " + latestVoltage3;
    }

    private void updateButtonColors() {
        Platform.runLater(() -> {
            updateButtonColor(libButton, latestMq135Value1);
            updateButtonColor(sesButton, latestMq135Value2);
            updateButtonColor(sceButton, latestMq135Value3);
        });
    }

    private void updateButtonColor(Button button, String mq135Value) {
        try {
            int mq135 = Integer.parseInt(mq135Value);
            if (mq135 < 126) {
                button.setStyle("-fx-background-color: green;");
            } else {
                button.setStyle("-fx-background-color: red;");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid MQ135 value: " + mq135Value);
        }
    }

    private void prepareGraphScene() {
        // Create axes for the temperature chart
        NumberAxis xAxisTemp = new NumberAxis();
        xAxisTemp.setLabel("Time (s)");
        NumberAxis yAxisTemp = new NumberAxis();
        yAxisTemp.setLabel("Temperature (°C)");

        // Create the temperature line chart
        LineChart<Number, Number> temperatureChart = new LineChart<>(xAxisTemp, yAxisTemp);
        temperatureChart.setTitle("Temperature Data Over Time");

        // Initialize temperature series for each sensor
        temperatureSeries1 = new XYChart.Series<>();
        temperatureSeries1.setName("Sensor 1");
        temperatureSeries2 = new XYChart.Series<>();
        temperatureSeries2.setName("Sensor 2");
        temperatureSeries3 = new XYChart.Series<>();
        temperatureSeries3.setName("Sensor 3");

        // Add temperature series to the temperature chart
        temperatureChart.getData().addAll(temperatureSeries1, temperatureSeries2, temperatureSeries3);

        // Create axes for the air quality chart
        NumberAxis xAxisAirQuality = new NumberAxis();
        xAxisAirQuality.setLabel("Time (s)");
        NumberAxis yAxisAirQuality = new NumberAxis();
        yAxisAirQuality.setLabel("Air Quality Value");

        // Create the air quality line chart
        airQualityChart = new LineChart<>(xAxisAirQuality, yAxisAirQuality);
        airQualityChart.setTitle("Air Quality Data Over Time");

        // Initialize air quality series for each sensor
        airQualitySeries1 = new XYChart.Series<>();
        airQualitySeries1.setName("Sensor 1");
        airQualitySeries2 = new XYChart.Series<>();
        airQualitySeries2.setName("Sensor 2");
        airQualitySeries3 = new XYChart.Series<>();
        airQualitySeries3.setName("Sensor 3");

        // Add air quality series to the air quality chart
        airQualityChart.getData().addAll(airQualitySeries1, airQualitySeries2, airQualitySeries3);

        // Button to navigate back to the heatmap scene
        Button backToHeatmap = new Button("Back to Heatmap");
        backToHeatmap.setOnAction(e -> mainStage.setScene(heatmapScene));

        // Button to toggle between temperature and air quality graphs
        Button toggleView = new Button("Toggle to Air Quality View");
        toggleView.setOnAction(e -> toggleGraphView(temperatureChart, airQualityChart, toggleView));

        // Layout to hold the charts and buttons
        VBox layout = new VBox(10, temperatureChart, backToHeatmap, toggleView);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(10));

        graphScene = new Scene(layout, 800, 550);
    }

    private void toggleGraphView(LineChart<Number, Number> temperatureChart, LineChart<Number, Number> airQualityChart, Button toggleView) {
        if (showingAirQuality) {
            // Switch to temperature view
            Button backToHeatmap = new Button("Back to Heatmap");
            VBox layout = new VBox(10, temperatureChart, backToHeatmap, toggleView);
            backToHeatmap.setOnAction(e -> mainStage.setScene(heatmapScene));
            layout.setAlignment(Pos.CENTER);
            layout.setPadding(new Insets(10));
            graphScene.setRoot(layout);
            toggleView.setText("Toggle to Air Quality View");
        } else {
            // Switch to air quality view
            Button backToHeatmap = new Button("Back to Heatmap");
            VBox layout2 = new VBox(10, airQualityChart, backToHeatmap, toggleView);
            backToHeatmap.setOnAction(e -> mainStage.setScene(heatmapScene));
            layout2.setAlignment(Pos.CENTER);
            layout2.setPadding(new Insets(10));
            graphScene.setRoot(layout2);
            toggleView.setText("Toggle to Temperature View");
        }
        showingAirQuality = !showingAirQuality;
    }

    private void updateGraphScene() {
        // Add new points to the temperature graph using the latest temperature readings and elapsed time
        temperatureSeries1.getData().add(new XYChart.Data<>(timeElapsed, lastTemperatureCelsius1));
        temperatureSeries2.getData().add(new XYChart.Data<>(timeElapsed, lastTemperatureCelsius2));
        temperatureSeries3.getData().add(new XYChart.Data<>(timeElapsed, lastTemperatureCelsius3));

        // Add new points to the air quality graph using the latest MQ135 values and elapsed time
        airQualitySeries1.getData().add(new XYChart.Data<>(timeElapsed, lastMQ135Val1));
        airQualitySeries2.getData().add(new XYChart.Data<>(timeElapsed, lastMQ135Val2));
        airQualitySeries3.getData().add(new XYChart.Data<>(timeElapsed, lastMQ135Val3));

        // Increment the time for the next point
        timeElapsed++;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
