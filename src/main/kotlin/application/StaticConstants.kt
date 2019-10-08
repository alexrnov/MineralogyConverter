package application

import java.util.stream.Collectors

object StaticConstants {

  @JvmStatic val nameApplication = "Минералогия Micromine"

  @JvmStatic val mainLayoutPath = "/view/MainLayout.fxml"

  @JvmStatic val cancelTaskDialogPath = "/view/CancelDialogLayout.fxml"

  @JvmStatic val iconPath = "/images/icon.png"

  @JvmStatic val openDialogPath = "/images/open_dialog.png"

  @JvmStatic val defaultStyleTextField = "-fx-border-color: #b1b1b1;"

  @JvmStatic val errorStyleTextField = "-fx-border-color: #ff2781;"

  @JvmStatic val textRunButton = "Запустить"

  @JvmStatic val textCancelButton = "Отмена"

  /**
   * максимальная высота, это значение необходимо указывать, для того,
   * чтобы изменялась полоса прокрутки, когда anchorPane больше этого
   * значения, т.е. когда все элементы интерфейса не помещаются на панели.
   * Это значение необходимо изменить, если меняются размеры приложения.
   */
  @JvmStatic val maxHeightScrollPane = 460

  private val listOfMenuTasks: List<Pair<String, String>> = listOf(
          Pair("Пробы с МСА (web-сервис)", "/view/Task1Layout.fxml"),
          Pair("Пробы без МСА (web-сервис)", "/view/Task2Layout.fxml"),
          Pair("Все пробы (web-сервис)", "/view/Task3Layout.fxml"),
          Pair("Интервалы опробования в точки", "/view/Task4Layout.fxml"),
          Pair("Границы опробования в точки", "/view/Task5Layout.fxml"),
          Pair("Пробы (ИСИХОГИ)", "/view/Task6Layout.fxml"),
          Pair("Устье/забой скважин (ИСИХОГИ)", "/view/Task7Layout.fxml"),
          Pair("Кровля/подошва пласта (ИСИХОГИ)", "/view/Task8Layout.fxml"),
          Pair("Страт. пласты в точки (ИСИХОГИ)", "/view/Task9Layout.fxml"),
          Pair("Кровля цоколя в точки (ИСИХОГИ)", "/view/Task10Layout.fxml"))

  @JvmStatic val namesOfTasks: List<String> = listOfMenuTasks.stream()
          .map { it.component1() }
          .collect(Collectors.toList())

  @JvmStatic
  val pathsOfTasks: List<String> = listOfMenuTasks.stream()
          .map { it.component2() }
          .collect(Collectors.toList())
}