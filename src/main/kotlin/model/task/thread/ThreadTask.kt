package model.task.thread

import javafx.concurrent.Task

/**
 * Данный абстрактный класс содержит некоторые общие настройки для управления
 * выводом информации на консоль, а также для управления индикатором
 * выполнения. Этот абстрактный класс должен являться супер-классом для
 * всех реализаций отдельных потоков JavaFX, управляющих ходом выполнения
 * геозадач, консолью и другими элементами пользовательского интерфейса,
 * задействованными в процессе выполнения геозадачи.
 */
abstract class ThreadTask : Task<Boolean>() {
  private var information = StringBuilder()

  init {
    clearAll()
  }

  private fun clearAll() {
    clearProgressBar()
    updateMessage("")
  }

  fun clearProgressBar() {
    updateProgress(0.0, 1.0)
    updateTitle("0%")
  }

  fun printConsole(message: String?) {
    information.append(message)
    information.append("\n")
    updateMessage(information.toString())
  }
}