package controller;

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

import static application.Mineralogy.logger;
import static application.StaticConstants.*;

/** Контроллер для интерфейса задачи "ABS верхняя/нижняя пробы". */
public class Task5Layout extends TaskLayout {

  @FXML private TextField inputFileTextField;
  @FXML private TextField outputFileTextField;
  @FXML private Button inputFileButton;
  @FXML private Button outputFileButton;
  @FXML private Button runTaskButton;
  @FXML private Button cancelTaskButton;
  @FXML private TextArea consoleTextArea;
  @FXML private ProgressBar progressBar;
  @FXML private Label processPercentLabel;

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

    inputFileTextField.focusedProperty().addListener((arg, oldValue, newValue) -> {
      if (newValue) {
        defaultStyle(inputFileTextField);
      }
    });

    outputFileTextField.focusedProperty().addListener((arg, oldValue, newValue) -> {
      if (newValue) {
        defaultStyle(outputFileTextField);
      }
    });

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

    if (threadTask != null && threadTask.isRunning()) {
      return;
    }

    if (!checkInputParameters()) {
      return;
    }

    //if (threadTask == null || threadTask.isDone())
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("inputFile", inputFileTextField.getText());
    parameters.put("outputFile", outputFileTextField.getText());

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

    return b;
  }

  private void defaultStyle(TextField textField) {
    if (textField.getStyle().contains(getErrorStyleTextField())) {
      textField.setStyle(getDefaultStyleTextField());
    }
  }
}
