package model.task.mineralogy

import application.Mineralogy.logger
import model.constants.CommonConstants
import model.constants.ProbesWithMSDConstants.requiredKeysTopWell
import model.constants.ProbesWithoutMSDConstants.indexAndNameOfColumns
import model.exception.DataException
import model.exception.ExcelException
import model.exception.GeoTaskException
import model.file.MicromineTextFile
import model.task.GeoTaskManyFiles
import model.utils.CollectionUtils
import model.utils.CollectionUtils.copyListWithSubMap
import model.utils.ExcelUtils
import model.utils.ExcelUtils.getSheetOfWebResource
import model.utils.ExcelUtils.getTableOfProbesWithoutMSD
import model.utils.WebServiceUtils.assignIDToIntervals
import model.utils.WebServiceUtils.selectionByGeologicalAge
import model.utils.WebServiceUtils.checkOnMissSpatialData
import model.utils.WebServiceUtils.checkSequenceIntervals
import model.utils.WebServiceUtils.defineDepthOfWells
import model.utils.WebServiceUtils.fixCoincidentCollarOfWell
import model.utils.WebServiceUtils.getWellsWithUniqueNames
import model.utils.WebServiceUtils.makeAmendment
import model.utils.WebServiceUtils.replaceCommaForWells
import model.utils.WebServiceUtils.updateCrystalNumberWithoutMSD
import model.utils.averageZByInterval
import model.utils.checkWorkingObjects
import java.io.File
import java.io.File.separator
import java.io.IOException
import java.nio.file.Paths
import java.util.ArrayList

/**
 * Задача для конвертации excel-файлов, загруженных с веб-сервиса
 * "Таблица результатов минералогических анализов проб без МСА,
 * с ассоциациями", в файлы для mineralogy. Из excel-файлов
 * извлекаются данные минералогических проб.
 */
class ProbesWithoutMSD

@Throws(IllegalArgumentException::class, SecurityException::class, IOException::class)
constructor(parameters: Map<String, Any>): GeoTaskManyFiles(parameters) {

  // список строк для записи в файл устьев скважин (на базе текущего excel-файла)
  private var topWells: MutableList<MutableMap<String, String>> = ArrayList()
  val getTopWells get() = CollectionUtils.copyListWithSubMap(topWells)

  // список строк для записи в файл интервалов (на базе текущего excel-файла)
  private var intervalWells: MutableList<MutableMap<String, String>> = ArrayList()
  val getIntervalWells get() = CollectionUtils.copyListWithSubMap(intervalWells)

  // список строк для записи в файл точек по пробам (на базе текущего excel-файла)
  private var dotWells: List<MutableMap<String, String>> = ArrayList()
  val getDotWells get() = CollectionUtils.copyListWithSubMap(dotWells.toMutableList())

  // id для последующей связи скважин в файлах Micromine
  private var idWell: Int = 0

  /* входные параметры */
  private var inputFolder: String = ""
  private var outputFolder: String = ""
  private var useAmendment = false // поправка ИСИХОГИ
  private var useReferenceVolume = false // пересчет на эталонный объем
  private var probeVolume: Byte = 0 // объем пробы
  private var createDotFile = false // создать ли файл точек по пробам
  private var typeOfSelectionAge: String = "" // тип выборки по возрастам

  /* общее количество строк для файлов устьев, интервалов и точек */
  private var overallNumberTopWells = 0
  private var overallNumberIntervalWells = 0
  private var overallNumberDotWells = 0

  /* объекты для записи файлов micromine */
  private lateinit var topWellsFile: MicromineTextFile
  private lateinit var intervalWellsFile: MicromineTextFile
  private lateinit var dotWellsFile: MicromineTextFile

  private val requiredKeysIntervalWell = ArrayList(indexAndNameOfColumns.values)
  private var nameOfObject = ""

  init {
    checkInputParameters()
    inputFiles = ExcelUtils.listOfExcelFiles(inputFolder)
    requiredKeysIntervalWell.add(0, "IDW")
    requiredKeysIntervalWell.add("Объект")
    createOutputFiles()
  }

  override fun perform(file: File) {
    try {
      val sheet = getSheetOfWebResource(file)
      nameOfObject = ExcelUtils.getNameOfObject(sheet)
      var table = getTableOfProbesWithoutMSD(sheet)
      checkWorkingObjects(file, table)

      table = selectionByGeologicalAge(table, typeOfSelectionAge)
      if (table.isEmpty()) {
        logger.info("Probes with stratigraphic index are not found")
        throw DataException("Проб с указанной выборкой по стратиграфии не найдено")
      }
      val mistakes = checkOnMissSpatialData(table)
      if (mistakes.isNotEmpty()) {
        task.printConsole("Ошибки отсутствия данных:")
        mistakes.forEach { task.printConsole(it) }
      }
      replaceCommaForWells(table)
      if (useAmendment) makeAmendment(table)

      topWells = copyListWithSubMap(table)
      topWells.forEach { it.keys.retainAll(requiredKeysTopWell) }
      topWells = getWellsWithUniqueNames(topWells)
      topWells.forEach { well ->
        well["IDW"] = idWell.toString()
        idWell++
      }
      fixCoincidentCollarOfWell(topWells)
      topWells.forEach { it["Объект"] = nameOfObject }
      intervalWells = copyListWithSubMap(table)
      assignIDToIntervals(topWells, intervalWells)
      checkSequenceIntervals(intervalWells)
      defineDepthOfWells(topWells, intervalWells)
      if (useReferenceVolume) updateCrystalNumberWithoutMSD(intervalWells, probeVolume)
      // сортировать сначала по ID, потом по отметке кровли пробы
      intervalWells = intervalWells.sortedWith(
              compareBy({ it["IDW"]!!.toInt() }, { it["От"]!!.toDouble() })).toMutableList()

      intervalWells.forEach { it["Объект"] = nameOfObject }
      task.printConsole("Из файла прочитано скважин: ${topWells.size}")
      task.printConsole("Из файла прочитано интервалов: ${intervalWells.size}")
      overallNumberTopWells += topWells.size
      overallNumberIntervalWells += intervalWells.size
      topWellsFile.writeContent(topWells)
      intervalWellsFile.writeContent(intervalWells)
      if (createDotFile) {
        dotWells = copyListWithSubMap(intervalWells)
        averageZByInterval(dotWells)
        task.printConsole("Из файла прочитано точек: ${dotWells.size}")
        overallNumberDotWells += dotWells.size
        dotWellsFile.writeContent(dotWells)
      }
    } catch (e: ExcelException) {
      throw GeoTaskException(e.message!!)
    } catch (e: DataException) {
      throw GeoTaskException(e.message!!)
    } catch (e: IOException) {
      logger.info("Error write data to output file: ${e.message}")
      throw GeoTaskException("Ошибка записи данных в файл")
    } catch (e: Exception) {
      logger.info(e.message)
      throw GeoTaskException("Неизвестная ошибка")
    }
  }

  @Throws(IllegalArgumentException::class)
  private fun checkInputParameters() {
    val ep = "Incorrect parameter for"

    if (parameters.containsKey("inputFolder")
            && parameters["inputFolder"] is String) {
      inputFolder = parameters["inputFolder"] as String
    } else throw IllegalArgumentException("$ep input folder")

    if (parameters.containsKey("outputFolder")
            && parameters["outputFolder"] is String) {
      outputFolder = parameters["outputFolder"] as String
    } else throw IllegalArgumentException("$ep output folder")

    if (parameters.containsKey("probeVolume")
            && parameters["probeVolume"] is String) {
      val s = parameters["probeVolume"] as String
      probeVolume = s.substring(0, s.length - 2).toByte()
    } else throw IllegalArgumentException("$ep probe volume")

    if (parameters.containsKey("useAmendment")
            && parameters["useAmendment"] is Boolean) {
      useAmendment = parameters["useAmendment"] as Boolean
    } else throw IllegalArgumentException("$ep amendment")

    if (parameters.containsKey("useReferenceVolume")
            && parameters["useReferenceVolume"] is Boolean) {
      useReferenceVolume = parameters["useReferenceVolume"] as Boolean
    } else throw IllegalArgumentException("$ep reference volume")

    if (parameters.containsKey("createDotFile")
            && parameters["createDotFile"] is Boolean) {
      createDotFile = parameters["createDotFile"] as Boolean
    } else throw IllegalArgumentException("$ep create dot file")

    if (parameters.containsKey("typeOfSelectionAge")
            && parameters["typeOfSelectionAge"] is String) {
      typeOfSelectionAge = parameters["typeOfSelectionAge"] as String
    } else throw IllegalArgumentException("$ep type of selection age")

    when {
      typeOfSelectionAge.contains("Указать возраст:") -> {
        val list = typeOfSelectionAge.split(":")
        if (list.size == 2 && list[1].isNotEmpty()) {
          typeOfSelectionAge = list[1]
        } else throw IllegalArgumentException("$ep type of selection age")
      }
      typeOfSelectionAge != "По всем возрастам"
              && typeOfSelectionAge != "Без возрастов"
              && typeOfSelectionAge != "Все пробы" -> {
        throw IllegalArgumentException("$ep type of selection age")
      }
    }
  }

  override fun printIntro() {
    task.printConsole("Входные параметры: ")
    task.printConsole("Каталог с входными excel-файлами: ")
    task.printConsole(inputFolder)
    task.printConsole("Каталог с выходными файлами micromine: ")
    task.printConsole(outputFolder)
    if (useReferenceVolume) {
      task.printConsole("Пересчитать количество минералов: Да")
      task.printConsole("Объем пробы: $probeVolume л; ")
    } else {
      task.printConsole("Пересчитать количество минералов: Нет")
    }
    task.printConsole("Выборка по стратиграфии: $typeOfSelectionAge")
    if (createDotFile) {
      task.printConsole("Создать файл точек по пробам: Да")
    } else {
      task.printConsole("Создать файл точек по пробам: Нет")
    }
    task.printConsole("Убрать поправку ИСИХОГИ для координат X и Y: " +
            if (useAmendment) "Да" else "Нет")
    task.printConsole("")
  }

  override fun printReport() {
    task.printConsole("")
    task.printConsole("Файл устьев скважин для Micromine:")
    task.printConsole("${topWellsFile.file.toAbsolutePath()}")
    task.printConsole("Файл интервалов скважин для Micromine:")
    task.printConsole("${intervalWellsFile.file.toAbsolutePath()}")
    if (createDotFile) {
      task.printConsole("Файл точек по интервалам для Micromine:")
      task.printConsole("${dotWellsFile.file.toAbsolutePath()}")
    }
    task.printConsole("Общее количество скважин, записанных в файл устьев: " +
            overallNumberTopWells)
    task.printConsole("Общее количество проб, записанных в файл интервалов: " +
            overallNumberIntervalWells)
    if (createDotFile) {
      task.printConsole("Общее количество точек, записанных в точечный файл: " +
              overallNumberDotWells)
    }
  }

  @Throws(IOException::class)
  private fun createOutputFiles() {
    val topWellsPath = Paths.get(outputFolder + separator + CommonConstants.nameOfTopWellsFile)
    topWellsFile = MicromineTextFile(topWellsPath)
    topWellsFile.writeTitle(requiredKeysTopWell)

    val intervalWellsPath = Paths.get(outputFolder + separator + CommonConstants.nameOfIntervalWellsFile)
    intervalWellsFile = MicromineTextFile(intervalWellsPath)
    intervalWellsFile.writeTitle(requiredKeysIntervalWell)

    if (createDotFile) {
      val dotWellsPath = Paths.get(outputFolder + separator + CommonConstants.nameOfDotWellsFile)
      dotWellsFile = MicromineTextFile(dotWellsPath)
      dotWellsFile.writeTitle(requiredKeysIntervalWell)
    }
  }
}