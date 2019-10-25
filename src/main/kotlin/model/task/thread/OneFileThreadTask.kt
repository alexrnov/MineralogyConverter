package model.task.thread

import application.Mineralogy.logger
import model.exception.GeoTaskException
import model.task.GeoTask
import model.task.GeoTaskOneFile
import model.task.TypeOfGeoTask
import java.io.IOException

/**
 * Реализация отдельного потока JavaFX для геозадачи, которая
 * считывает данные из одного исходного файла. Основное назначение
 * данного класса - управление выводом информации на консоль;
 * контроль и обработка ошибок, которые могут возникать при
 * выпольнении геозадачи; управление индикатором выполнения.
 * [nameOfTask] - название задачи
 * [parameters] - список параметров, необходимых для выполнения
 * текщей задачи
 */
class OneFileThreadTask(private val nameOfTask: String,
                        private val parameters: Map<String, Any>):
        ThreadTask() {

  @Throws(Exception::class)
  override fun call(): Boolean {
    val geoTask: GeoTask
    try {
      geoTask = TypeOfGeoTask.getType(nameOfTask, parameters)
      if (geoTask !is GeoTaskOneFile) {
        printConsole("Задача не реализована")
        logger.info("Task not realized")
        return false
      }
      geoTask.setThreadingTask(this)
      geoTask.printIntro()
    } catch (e: IllegalArgumentException) {
      printConsole("Входные параметры неверны") // или неверное название задачи
      logger.info(e.message)
      return false
    } catch (e: Exception) {
      printConsole("Не удалось запустить задачу. " +
              "См. описание ошибки в лог-файле")
      logger.info(e.message)
      return false
    }

    printConsole("Чтение исходного файла")
    val table: Collection<Any?>
    try {
      table = geoTask.getTableFromFile()
    } catch (e: SecurityException) {
      printConsole("Доступ к файлам ограничен")
      logger.info("access to the starting file is denies")
      return false
    } catch (e: IOException) {
      printConsole("Не удалось прочитать входной файл")
      logger.info("no read input file : ${e.message}")
      return false
    }

    updateProgress(0.1, 1.0)
    updateTitle("10%")

    val increment: Double = 0.9 / table.size
    var performed = 0.1
    printConsole("Вычисление задачи...")
    table.forEach { line ->
      if (this.isCancelled) { // если задача была остановлена извне
        logger.info("Canceled task")
        printConsole("Выполнение задачи остановлено")
        clearProgressBar()
        return false // принудительно выйти (чтобы не проходить все итерации)
      }

      try {
        geoTask.perform(line)
      } catch (e: GeoTaskException) {
        printConsole("Ошибка вычислений. См. описание ошибки в лог-файле")
        logger.warning(e.message)
        return false
      }
      performed += increment
      updateProgress(performed, 1.0)
      updateTitle("${(performed * 100).toInt()}%")
    }
    printConsole("Запись результатов в файл")

    try {
      geoTask.writeData()
    } catch (e: SecurityException) {
      printConsole("Доступ к файловой системе ограничен")
      logger.info("access to the file system is denies")
      return false
    } catch (e: IOException) {
      printConsole("Не удалось записать выходной файл")
      logger.info("output file no created: ${e.message}")
      return false
    }

    geoTask.printReport()
    updateProgress(1.0, 1.0)
    updateTitle("100%")
    return true
  }
}