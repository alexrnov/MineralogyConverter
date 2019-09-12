package model.task.mineralogy

import application.Mineralogy.logger
import model.constants.CommonConstants.nameOfDotWellsFile
import model.constants.CommonConstants.nameOfIntervalWellsFile
import model.constants.CommonConstants.nameOfTopWellsFile
import model.constants.ProbesWithMSDConstants.indexAndNameOfColumns
import model.constants.ProbesWithMSDConstants.requiredKeysTopWell
import model.exception.DataException
import model.exception.ExcelException
import model.exception.GeoTaskException
import model.file.MicromineTextFile
import model.task.GeoTaskManyFiles
import model.utils.CollectionUtils
import model.utils.CollectionUtils.copyListWithSubMap
import model.utils.ExcelUtils
import model.utils.ExcelUtils.getNameOfObject
import model.utils.ExcelUtils.getSheetOfWebResource
import model.utils.ExcelUtils.getTableOfProbesWithMSD
import model.utils.ExcelUtils.getTableOfProbesWithoutMSD
import model.utils.ExcelUtils.isTableOfProbesWithoutMSD
import model.utils.WebServiceUtils.assignIDToIntervals
import model.utils.WebServiceUtils.checkOnMissSpatialData
import model.utils.WebServiceUtils.checkSequenceIntervals
import model.utils.WebServiceUtils.defineDepthOfWells
import model.utils.WebServiceUtils.fixCoincidentCollarOfWell
import model.utils.WebServiceUtils.getWellsWithUniqueNames
import model.utils.WebServiceUtils.makeAmendment
import model.utils.WebServiceUtils.replaceCommaForWells
import model.utils.WebServiceUtils.selectionByGeologicalAge
import model.utils.WebServiceUtils.updateCrystalNumberWithMSD
import model.utils.WebServiceUtils.updateCrystalNumberWithoutMSD
import model.utils.averageZByInterval
import model.utils.checkWorkingObjects
import java.io.File
import java.io.File.separator
import java.io.IOException
import java.nio.file.Paths
import java.util.*

/**
 * Задача для конвертации excel-файлов, загруженных с веб-сервиса
 * "МСА по всем объектам", в файлы для mineralogy. Из excel-файлов
 * извлекаются данные минералогических проб.
 */
class ProbesWithAllMSD

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
  private var inputFolderWithoutMSD: String = ""
  private var inputFolderWithMSD: String = ""
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

  private var isTableWithoutMSD = false
  private var nameOfObject = ""

  init {
    checkInputParameters()
    inputFiles = ExcelUtils.listOfExcelFiles(inputFolderWithoutMSD)
    inputFiles.addAll(ExcelUtils.listOfExcelFiles(inputFolderWithMSD))
    requiredKeysIntervalWell.add(0, "IDW")
    // атрибут нужен для того, чтобы производить расчет блочной
    // модели по наличию/отсутсвию находок
    requiredKeysIntervalWell.add("находки")
    createOutputFiles()
  }

  override fun perform(file: File) {
    try {
      val sheet = getSheetOfWebResource(file)
      if (isTableOfProbesWithoutMSD(sheet)) {
        nameOfObject = getNameOfObject(sheet)
        isTableWithoutMSD = true
      } else {
        isTableWithoutMSD = false
      }
      var table: MutableList<MutableMap<String, String>>
      table = if (isTableWithoutMSD) {
        getTableOfProbesWithoutMSD(sheet)
      } else {
        getTableOfProbesWithMSD(sheet)
      }
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
      if (isTableWithoutMSD) {
        topWells.forEach {
          it["Объект"] = nameOfObject
        }
      }

      intervalWells = copyListWithSubMap(table)
      assignIDToIntervals(topWells, intervalWells)
      checkSequenceIntervals(intervalWells)

      defineDepthOfWells(topWells, intervalWells)

      if (useReferenceVolume) {
        if (isTableWithoutMSD) {
          updateCrystalNumberWithoutMSD(intervalWells, probeVolume)
        } else {
          updateCrystalNumberWithMSD(intervalWells, probeVolume)
        }
      }

      checkSequenceIntervals(intervalWells)
      // сортировать сначала по ID, потом по отметке кровли пробы
      intervalWells = intervalWells.sortedWith(
              compareBy({ it["IDW"]!!.toInt() }, { it["От"]!!.toDouble() })).toMutableList()

      if (isTableWithoutMSD) {
        intervalWells.forEach {
          it["Объект"] = nameOfObject
          it["находки"] = "0.0"
        }
      } else {
        intervalWells.forEach {
          it["находки"] = "1.0"
        }
      }

      task.printConsole("Из файла прочитано скважин: ${topWells.size}")
      task.printConsole("Из файла прочитано интервалов: ${intervalWells.size}")
      overallNumberTopWells += topWells.size
      overallNumberIntervalWells += intervalWells.size
      topWellsFile.writeContent(topWells)

      if (isTableWithoutMSD) {
        intervalWellsFile.writeContentWithoutSomeKeys(intervalWells)
      } else {
        intervalWellsFile.writeContent(intervalWells)
      }
      if (createDotFile) {
        dotWells = copyListWithSubMap(intervalWells)
        averageZByInterval(dotWells)
        task.printConsole("Из файла прочитано точек: ${dotWells.size}")
        overallNumberDotWells += dotWells.size
        if (isTableWithoutMSD) {
          dotWellsFile.writeContentWithoutSomeKeys(dotWells)
        } else {
          dotWellsFile.writeContent(dotWells)
        }
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

    if (parameters.containsKey("inputFolderWithoutMSD")
            && parameters["inputFolderWithoutMSD"] is String) {
      inputFolderWithoutMSD = parameters["inputFolderWithoutMSD"] as String
    } else throw IllegalArgumentException("$ep input folder without MSD")

    if (parameters.containsKey("inputFolderWithMSD")
            && parameters["inputFolderWithMSD"] is String) {
      inputFolderWithMSD = parameters["inputFolderWithMSD"] as String
    } else throw IllegalArgumentException("$ep input folder with MSD")

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

  @Throws(IOException::class)
  private fun createOutputFiles() {
    val topWellsPath = Paths.get(outputFolder + separator + nameOfTopWellsFile)
    topWellsFile = MicromineTextFile(topWellsPath)
    topWellsFile.writeTitle(requiredKeysTopWell)

    val intervalWellsPath = Paths.get(outputFolder + separator + nameOfIntervalWellsFile)
    intervalWellsFile = MicromineTextFile(intervalWellsPath)
    intervalWellsFile.writeTitle(requiredKeysIntervalWell)

    if (createDotFile) {
      val dotWellsPath = Paths.get(outputFolder + separator + nameOfDotWellsFile)
      dotWellsFile = MicromineTextFile(dotWellsPath)
      dotWellsFile.writeTitle(requiredKeysIntervalWell)
    }
  }

  override fun printIntro() {
    task.printConsole("Входные параметры: ")
    task.printConsole("Каталог с входными excel-файлами ${"Пробы без МСА"}: ")
    task.printConsole(inputFolderWithoutMSD)
    task.printConsole("Каталог с входными excel-файлами ${"Пробы с МСА"}: ")
    task.printConsole(inputFolderWithMSD)
    task.printConsole("Каталог с выходными файлами micromine: ")
    task.printConsole(outputFolder)
    if (useReferenceVolume) {
      task.printConsole("Пересчитать количество зерен для всех минералов: Да")
      task.printConsole("Объем пробы: $probeVolume л; ")
    } else {
      task.printConsole("Пересчитать количество кристаллов всех минералов: Нет")
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
}