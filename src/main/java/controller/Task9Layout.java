package controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import model.task.thread.ManyFilesThreadTask;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static application.StaticConstants.*;

/** Контроллер для интерфейса задачи "Кровля/подошва пласта (ИСИХОГИ)" */
public class Task9Layout extends TaskLayout {

  @FXML private TextField inputFolderTextField;
  @FXML private TextField outputFileTextField;
  @FXML private TextField stratigraphicTextField;
  @FXML private Button inputFolderButton;
  @FXML private Button outputFileButton;
  @FXML private Button runTaskButton;
  @FXML private Button cancelTaskButton;
  @FXML private TextArea consoleTextArea;
  @FXML private ProgressBar progressBar;
  @FXML private Label processPercentLabel;
  @FXML private CheckBox unionLayersCheckBox;
  @FXML private CheckBox unionPacketsCheckBox;
  @FXML private CheckBox amendmentCheckBox;

  private ButtonAnimation inputFolderAnimation;
  private ButtonAnimation outputFileAnimation;
  private ButtonAnimation runTaskAnimation;
  private ButtonAnimation cancelTaskAnimation;

  @FXML
  private void initialize() {
    consoleTextArea.setEditable(false);
    consoleTextArea.setWrapText(true); // автоперенос строк в консоли

    createInputFolderButton(new ImageView(getOpenDialogPath()));
    createOutputFolderButton(new ImageView(getOpenDialogPath()));
    createButtonRunTask();
    createButtonCancelTask();

    inputFolderTextField.focusedProperty().addListener((arg, oldValue, newValue) -> {
      if (newValue) {
        defaultStyle(inputFolderTextField);
      }
    });

    outputFileTextField.focusedProperty().addListener((arg, oldValue, newValue) -> {
      if (newValue) {
        defaultStyle(outputFileTextField);
      }
    });

    stratigraphicTextField.focusedProperty().addListener((arg, oldValue, newValue) -> {
      if (newValue) {
        defaultStyle(stratigraphicTextField);
      }
    });

    // слушатель для чекбокса объединения сопредельных стратиграфических
    // пластов с одинаковым индексом; если чекбокс выбран, тогда
    // активируется checkbox для объеинения пачек, в противном случае он
    // деактивируется, поскольку не играет самостоятельной роли
    unionLayersCheckBox.selectedProperty().addListener((arg, oldValue, newValue) -> {
      if (unionLayersCheckBox.isSelected()) {
        unionPacketsCheckBox.setDisable(false);
      } else {
        unionPacketsCheckBox.setDisable(true);
      }
    });

    unionLayersCheckBox.setSelected(true);
    unionPacketsCheckBox.setSelected(true);
    amendmentCheckBox.setSelected(true);

    // задать предел длины текстового поля для ввода стратиграфического индекса
    stratigraphicTextField.textProperty().addListener((ov, oldValue, newValue) -> {
      final byte maxLength = 20;
      if (stratigraphicTextField.getText().length() > maxLength) {
        String s = stratigraphicTextField.getText().substring(0, maxLength);
        stratigraphicTextField.setText(s);
      }
    });
  }

  private void createInputFolderButton(ImageView openDialogPathImage) {
    inputFolderButton.setGraphic(openDialogPathImage);
    inputFolderAnimation = new ButtonAnimation(inputFolderButton,
            "5 5 5 5", true);
    inputFolderButton.setOnAction(e -> {
      File f = mineralogy.directoryChooser();
      if (f != null) {
        inputFolderTextField.setText(f.getPath());
        defaultStyle(inputFolderTextField);
      }
    });
    inputFolderButton.addEventHandler(MouseEvent.MOUSE_ENTERED,
            e -> inputFolderAnimation.mouseEntered());
    inputFolderButton.addEventHandler(MouseEvent.MOUSE_EXITED,
            e -> inputFolderAnimation.mouseExited());
  }

  private void createOutputFolderButton(ImageView openDialogPathImage) {
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
    parameters.put("inputFolder", inputFolderTextField.getText());
    parameters.put("outputFile", outputFileTextField.getText());
    parameters.put("ageIndex", stratigraphicTextField.getText());
    parameters.put("unionLayers", unionLayersCheckBox.isSelected());
    parameters.put("unionPackets", unionPacketsCheckBox.isSelected());
    parameters.put("useAmendment", amendmentCheckBox.isSelected());

    threadTask = new ManyFilesThreadTask(mainLayout.getNameOfCurrentTask(),
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
    File inputFolder = new File(inputFolderTextField.getText());
    if (!inputFolder.isDirectory()) {
      inputFolderTextField.setStyle(getErrorStyleTextField());
      b = false;
    }
    String outputFile = outputFileTextField.getText();
    if (outputFile.length() <= 4 || !outputFile.substring(outputFile.length() - 4,
            outputFile.length()).equals(".txt")) {
      outputFileTextField.setStyle(getErrorStyleTextField());
      b = false;
    }
    if (stratigraphicTextField.getText().trim().length() == 0) {
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
