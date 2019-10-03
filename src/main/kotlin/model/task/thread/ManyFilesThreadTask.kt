package model.task.thread

import application.Mineralogy.logger
import model.exception.ExcelException
import model.exception.GeoTaskException
import model.task.GeoTask
import model.task.GeoTaskManyFiles
import model.task.TypeOfGeoTask
import java.io.IOException

/**
 * Реализация отдельного потока JavaFX для геозадачи, которая считывает данные
 * из нескольких исходных файлов. Основное назначение данного класса -
 * управление выводом информации на консоль; контроль и обработка ошибок,
 * которые могут возникать при выпольнении геозадачи; управление индикатором
 * выполнения.
 * [nameOfTask] - название задачи
 * [parameters] - список параметров, необходимых для выполнения текщей задачи
 */
class ManyFilesThreadTask(private val nameOfTask: String,
                          private val parameters: Map<String, Any>):
        ThreadTask() {

  @Throws(Exception::class)
  public override fun call(): Boolean {
    val geoTask: GeoTask
    try {
      geoTask = TypeOfGeoTask.getType(nameOfTask, parameters)
      if (geoTask !is GeoTaskManyFiles) {
        printConsole("Задача не реализована")
        logger.info("Task not realized")
        return false
      }
      if (geoTask.inputFiles.isEmpty()) {
        printConsole("В каталоге нет подходящих файлов")
        logger.info("In folder not contain appropriate files")
        return false
      }
      geoTask.setThreadingTask(this)
      geoTask.printIntro()
    } catch (e: IllegalArgumentException) {
      printConsole("Входные параметры неверны") // или неверное название задачи
      logger.info(e.message)
      return false
    } catch (e: SecurityException) {
      printConsole("Доступ к файлам ограничен")
      logger.info("access to the starting file is denies")
      return false
    } catch (e: IOException) {
      printConsole("Не удалось создать выходной файл")
      logger.info("file no created: ${e.message}")
      return false
    } catch (e: ExcelException) {
      printConsole("Не удалось создать выходной файл из-за ошибок формата" +
              " исходных данных")
      logger.info("file no created because error of format input excel data")
      return false
    } catch (e: Exception) {
      printConsole("Не удалось запустить задачу. " +
              "См. описание ошибки в лог-файле")
      logger.info(e.message)
      return false
    }
    val increment: Double = 1.0 / geoTask.inputFiles.size
    var performed = 0.0

    for (excelFile in geoTask.inputFiles) {
      if (this.isCancelled) { // если задача была остановлена извне
        logger.info("Canceled task")
        printConsole("Выполнение задачи остановлено")
        clearProgressBar()
        return false // принудительно выйти (чтобы не проходить все итерации)
      }
      printConsole("Чтение файла: ${excelFile.name}")

      try {
        geoTask.perform(excelFile)
      } catch (e: GeoTaskException) {
        printConsole(e.message)
      }
      performed += increment
      updateProgress(performed, 1.0)
      updateTitle("${(performed * 100).toInt()}%")
    }
    geoTask.printReport()

    updateProgress(1.0, 1.0)
    updateTitle("100%")
    return true
  }
}