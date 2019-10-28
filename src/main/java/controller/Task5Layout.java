package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import model.task.thread.OneFileThreadTask;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static application.StaticConstants.*;

/** Контроллер для интерфейса задачи "Интервалы опробования в точки" */
public class Task5Layout extends TaskLayout {

  @FXML private TextField inputFileTextField;
  @FXML private TextField outputFileTextField;
  @FXML private TextField stratigraphicTextField;
  @FXML private Button inputFileButton;
  @FXML private Button outputFileButton;
  @FXML private Button runTaskButton;
  @FXML private Button cancelTaskButton;
  @FXML private ComboBox<Integer> frequencyComboBox;

  @FXML private RadioButton highlightByFindRadioButton;
  @FXML private RadioButton highlightByFindAndAgeRadioButton;
  @FXML private RadioButton commonSafetyRadioButton;
  @FXML private TextArea consoleTextArea;
  @FXML private ProgressBar progressBar;
  @FXML private Label processPercentLabel;

  private ToggleGroup toggleGroup = new ToggleGroup(); // группа радио-кнопок
  private ButtonAnimation inputFileAnimation;
  private ButtonAnimation outputFileAnimation;
  private ButtonAnimation runTaskAnimation;
  private ButtonAnimation cancelTaskAnimation;

  @FXML
  private void initialize() {
    consoleTextArea.setEditable(false);
    consoleTextArea.setWrapText(true); // автоперенос строк в консоли

    createInputFileButton(new ImageView(getOpenDialogPath()));
    createOutputFileButton(new ImageView(getOpenDialogPath()));
    createButtonRunTask();
    createButtonCancelTask();

    stratigraphicTextField.setDisable(true);
    highlightByFindRadioButton.setToggleGroup(toggleGroup);
    highlightByFindRadioButton.setUserData("highlightByFind");
    highlightByFindRadioButton.setSelected(true);
    highlightByFindAndAgeRadioButton.setToggleGroup(toggleGroup);
    highlightByFindAndAgeRadioButton.setUserData("highlightByFindAndAge");
    commonSafetyRadioButton.setToggleGroup(toggleGroup);
    commonSafetyRadioButton.setUserData("commonSafety");

    // слушатель событий переключения радио-кнопок в группе радио-кнопок
    toggleGroup.selectedToggleProperty().addListener((observable, oldToggle, newToggle) -> {
      if (newToggle.getUserData().toString().equals("highlightByFindAndAge")) {
        stratigraphicTextField.setDisable(false);
      }
      if (oldToggle.getUserData().toString().equals("highlightByFindAndAge")) {
        stratigraphicTextField.setDisable(true);
        defaultStyle(stratigraphicTextField);
      }
    });

    inputFileTextField.focusedProperty().addListener((arg, oldValue, newValue) -> {
      if (newValue) defaultStyle(inputFileTextField);
    });

    outputFileTextField.focusedProperty().addListener((arg, oldValue, newValue) -> {
      if (newValue) defaultStyle(outputFileTextField);
    });

    stratigraphicTextField.focusedProperty().addListener((arg, oldValue, newValue) -> {
      if (newValue) defaultStyle(stratigraphicTextField);
    });

    ObservableList<Integer> volumes = FXCollections.observableArrayList(1, 2, 3, 4, 5);
    frequencyComboBox.getItems().addAll(volumes);
    frequencyComboBox.setValue(1);
  }

  private void createInputFileButton(ImageView openDialogPathImage) {
    inputFileButton.setGraphic(openDialogPathImage);
    inputFileAnimation = new ButtonAnimation(inputFileButton,
            "5 5 5 5", true);
    inputFileButton.setOnAction(e -> {
      File f = mineralogy.txtFileOpenDialog();
      if (f != null) {
        inputFileTextField.setText(f.getPath());
        defaultStyle(inputFileTextField);
      }
    });
    inputFileButton.addEventHandler(MouseEvent.MOUSE_ENTERED,
            e -> inputFileAnimation.mouseEntered());
    inputFileButton.addEventHandler(MouseEvent.MOUSE_EXITED,
            e -> inputFileAnimation.mouseExited());
  }

  private void createOutputFileButton(ImageView openDialogPathImage) {
    outputFileButton.setGraphic(openDialogPathImage);
    outputFileAnimation = new ButtonAnimation(outputFileButton,
            "5 5 5 5", true);
    outputFileButton.setOnAction(e -> {
      File f = mineralogy.txtFileSaveDialog();
      if (f != null) {
        outputFileTextField.setText(f.getPath());
        defaultStyle(outputFileTextField);
      }
    });
    outputFileButton.addEventHandler(MouseEvent.MOUSE_ENTERED,
            e -> outputFileAnimation.mouseEntered());
    outputFileButton.addEventHandler(MouseEvent.MOUSE_EXITED,
            e -> outputFileAnimation.mouseExited());
  }

  private void createButtonRunTask() {
    runTaskAnimation = new ButtonAnimation(runTaskButton,
            "10 15 10 15", true);
    runTaskButton.setText(getTextRunButton());
    runTaskButton.setOnAction(event -> {
      runTaskAnimation.mouseOnClick();
      runTask();
    });
    runTaskButton.addEventHandler(MouseEvent.MOUSE_ENTERED,
            e -> runTaskAnimation.mouseEntered());
    runTaskButton.addEventHandler(MouseEvent.MOUSE_EXITED,
            e -> runTaskAnimation.mouseExited());
  }

  private void createButtonCancelTask() {
    cancelTaskAnimation = new ButtonAnimation(cancelTaskButton,
            "10 24 10 24", false);
    cancelTaskButton.setText(getTextCancelButton());
    cancelTaskButton.setOnAction(event -> {
      cancelTaskAnimation.mouseOnClick();
      if (threadTask != null && threadTask.isRunning()) {
        createCancelTaskDialog(false);
      }
    });
    cancelTaskButton.addEventHandler(MouseEvent.MOUSE_ENTERED,
            e -> cancelTaskAnimation.mouseEntered());
    cancelTaskButton.addEventHandler(MouseEvent.MOUSE_EXITED,
            e -> cancelTaskAnimation.mouseExited());
  }

  private void runTask() {

    if (threadTask != null && threadTask.isRunning()) return;

    if (!checkInputParameters()) return;

    //if (threadTask == null || threadTask.isDone())
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("inputFile", inputFileTextField.getText());
    parameters.put("outputFile", outputFileTextField.getText());
    parameters.put("frequency", frequencyComboBox.getValue());

    if (highlightByFindAndAgeRadioButton.isSelected()) {
      parameters.put("taskName", "highlightByFindAndAge" + ";;" + stratigraphicTextField.getText());
    } else {
      parameters.put("taskName", toggleGroup.getSelectedToggle().getUserData().toString());
    }

    threadTask = new OneFileThreadTask(mainLayout.getNameOfCurrentTask(),
            parameters);

    threadTask.setOnRunning(event -> {
      mainLayout.blockListView(true);
      mainLayout.changeStyleListView();
      runTaskAnimation.activation(false);
      cancelTaskAnimation.activation(true);
    });

    threadTask.setOnSucceeded(event -> {
      mainLayout.blockListView(false);
      mainLayout.defaultStyleListView();
      runTaskAnimation.activation(true);
      cancelTaskAnimation.activation(false);
      if (cancelDialogController != null) {
        cancelDialogController.noActivateYesButton();
      }
      //threadTask.getValue());
    });

    threadTask.setOnCancelled(event -> {
      mainLayout.blockListView(false);
      mainLayout.defaultStyleListView();
      threadTask.clearProgressBar();
      runTaskAnimation.activation(true);
      cancelTaskAnimation.activation(false);
    });

    threadTask.setOnFailed(event -> {
      mainLayout.blockListView(false);
      mainLayout.defaultStyleListView();
      threadTask.clearProgressBar();
      runTaskAnimation.activation(true);
      cancelTaskAnimation.activation(false);
    });

    progressBar.progressProperty().bind(threadTask.progressProperty());
    consoleTextArea.textProperty().bind(threadTask.messageProperty());
    processPercentLabel.textProperty().bind(threadTask.titleProperty());

    ExecutorService executorService = Executors.newFixedThreadPool(1);
    executorService.execute(threadTask);
    executorService.shutdown();
  }

  /**
   * Реализация метода должна определять способ проверки входных
   * параметров для каждой задачи.
   */
  private boolean checkInputParameters() {
    boolean b = true;
    File inputFile = new File(inputFileTextField.getText());
    if (!inputFile.isFile()) {
      inputFileTextField.setStyle(getErrorStyleTextField());
      b = false;
    }

    String outputFile = outputFileTextField.getText();
    if (outputFile.length() <= 4 || !outputFile.substring(outputFile.length() - 4,
            outputFile.length()).equals(".txt")) {
      outputFileTextField.setStyle(getErrorStyleTextField());
      b = false;
    }

    if (toggleGroup.getSelectedToggle().getUserData().toString().equals("highlightByFindAndAge")
            && stratigraphicTextField.getText().trim().isEmpty()) {
      stratigraphicTextField.setStyle(getErrorStyleTextField());
      b = false;
    }

    return b;
  }

  private void defaultStyle(TextField textField) {
    if (textField.getStyle().contains(getErrorStyleTextField())) {
      textField.setStyle(getDefaultStyleTextField());
    }
  }
}
