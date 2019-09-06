package model.task.mineralogy

import application.Mineralogy.logger
import model.constants.CommonConstants.nameOfDotWellsFile
import model.constants.CommonConstants.nameOfIntervalWellsFile
import model.constants.CommonConstants.nameOfTopWellsFile
import model.constants.IsihogyClientConstants.lithologyCodesSheetName
import model.constants.IsihogyClientConstants.mineralogySheetName
import model.constants.IsihogyClientConstants.nameOfAttributeAllMinerals
import model.constants.IsihogyClientConstants.nameOfAttributeID
import model.constants.IsihogyClientConstants.nameOfAttributeUIN
import model.constants.IsihogyClientConstants.observationsPointsSheetName
import model.constants.IsihogyClientConstants.stateDocumentationCodesSheetName
import model.constants.IsihogyClientConstants.stratigraphicCodesSheetName
import model.constants.IsihogyClientConstants.typeOfProbesCodesSheetName
import model.exception.DataException
import model.exception.ExcelException
import model.exception.GeoTaskException
import model.file.MicromineTextFile
import model.task.GeoTaskManyFiles
import model.utils.CollectionUtils.copyListWithSubMap
import model.utils.ExcelUtils
import model.utils.ExcelUtils.getCodesOfIsihogyClient
import model.utils.ExcelUtils.getSheetOfIsihogyClient
import model.utils.ExcelUtils.getTableOfIsihogyClient
import model.utils.ExcelUtils.getTitleTableOfIsihogyClient
import model.utils.IsihogyClientUtils.addAttributeOfAllMinerals
import model.utils.IsihogyClientUtils.checkOnMissDataXYZDAndFix
import model.utils.IsihogyClientUtils.checkOnMissDataFromTo
import model.utils.IsihogyClientUtils.decodingField
import model.utils.IsihogyClientUtils.deleteDecimalPart
import model.utils.IsihogyClientUtils.fixCoincidentCollarOfWell
import model.utils.IsihogyClientUtils.fixWhenIntervalMoreThanDepth
import model.utils.IsihogyClientUtils.getWellsWithProbes
import model.utils.IsihogyClientUtils.interchangeXY
import model.utils.IsihogyClientUtils.makeAmendment
import model.utils.IsihogyClientUtils.replaceNoDataToZero
import model.utils.IsihogyClientUtils.filterByFindsOfCrystals
import model.utils.IsihogyClientUtils.filterByGeologicalAge
import model.utils.IsihogyClientUtils.unionTablesForDotWells
import model.utils.averageZByInterval
import java.io.File
import java.io.File.separator as s
import java.io.IOException
import java.nio.file.Paths

/**
 * Задача для конвертации excel-файлов, загруженных из клиента ИСИХОГИ
 * в файлы для mineralogy. Из excel-файлов извлекаются данные
 * минералогических проб.
 */
class ProbesIsihogyClient

@Throws(IllegalArgumentException::class, SecurityException::class,
        IOException::class, ExcelException::class)
constructor(parameters: Map<String, Any>): GeoTaskManyFiles(parameters) {
  /* входные параметры */
  private var inputFolder: String = ""
  private var outputFolder: String = ""
  private var findsOfCrystals: String = "" // тип выборки по наличию находок МСА
  private var useAmendment = false // поправка ИСИХОГИ
  private var createDotFile = false // создать ли файл точек по пробам
  private var typeOfSelectionAge: String = "" // тип выборки по возрастам

  // список строк для записи в файл устьев скважин (на базе текущего excel-файла)
  private var observationsPointsTable: List<MutableMap<String, String>> = ArrayList()
  val getObservationsPointsTable get() = copyListWithSubMap(observationsPointsTable)

  // список строк для записи в файл интервалов (на базе текущего excel-файла)
  private var mineralogyTable: List<MutableMap<String, String>> = ArrayList()
  val getMineralogyTable get() = copyListWithSubMap(mineralogyTable)

  // список строк для записи в файл точек по пробам (на базе текущего excel-файла)
  private var dotWells: List<MutableMap<String, String>> = java.util.ArrayList()
  val getDotWells get() = copyListWithSubMap(dotWells.toMutableList())

  /* объекты для записи файлов micromine */
  private lateinit var topWellsFile: MicromineTextFile
  private lateinit var intervalWellsFile: MicromineTextFile
  private lateinit var dotWellsFile: MicromineTextFile

  /* список с названиями числовых атрибутов (находки МСА) */
  private lateinit var attributesOfMineralogy: MutableList<String>

  /* общее количество строк для файлов устьев, интервалов и точек */
  private var overallNumberTopWells = 0
  private var overallNumberIntervalWells = 0
  private var overallNumberDotWells = 0

  init {
    checkInputParameters()
    inputFiles = ExcelUtils.listOfExcelFiles(inputFolder)
    if (inputFiles.isNotEmpty()) {
      // взять заголовок для выходного файла из первого входного файла
      createOutputFiles(inputFiles[0])
      attributesOfMineralogy.removeAll(listOf("ID ТН", "Номер пробы", "ID пробы",
        "Код Типа_пробы", "От", "До", "L_Code возраста", "L_Code породы", "UIN"))
    }
  }

  @Throws(GeoTaskException::class)
  override fun perform(file: File) {
    try {
      getTablesAndDecodeFields(file)

      deleteDecimalPart(nameOfAttributeID, observationsPointsTable)
      deleteDecimalPart(nameOfAttributeID, mineralogyTable)

      val mistakes = checkOnMissDataXYZDAndFix(observationsPointsTable)
      mistakes.addAll(checkOnMissDataFromTo(mineralogyTable))

      if (mistakes.isNotEmpty()) {
        task.printConsole("Ошибки отсутствия данных:")
        mistakes.forEach { task.printConsole(it) }
      }

      mineralogyTable = filterByGeologicalAge(mineralogyTable, typeOfSelectionAge)
      replaceNoDataToZero(mineralogyTable, attributesOfMineralogy)
      addAttributeOfAllMinerals(mineralogyTable, attributesOfMineralogy)
      mineralogyTable = filterByFindsOfCrystals(mineralogyTable, findsOfCrystals)

      observationsPointsTable = getWellsWithProbes(observationsPointsTable,
              mineralogyTable)

      if (observationsPointsTable.isEmpty()) {
        logger.info("Wells for probes are not found")
        throw DataException("Скважины для проб не найдены")
      }

      interchangeXY(observationsPointsTable)

      if (useAmendment) makeAmendment(observationsPointsTable)

      fixCoincidentCollarOfWell(observationsPointsTable)
      fixWhenIntervalMoreThanDepth(observationsPointsTable, mineralogyTable)

      task.printConsole("Из файла прочитано скважин: " +
              "${observationsPointsTable.size}")
      task.printConsole("Из файла прочитано интервалов: " +
              "${mineralogyTable.size}")

      overallNumberTopWells += observationsPointsTable.size
      overallNumberIntervalWells += mineralogyTable.size

      topWellsFile.writeContent(observationsPointsTable)
      intervalWellsFile.writeContent(mineralogyTable)

      if (createDotFile) {
        dotWells = unionTablesForDotWells(observationsPointsTable,
                mineralogyTable)
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

    if (parameters.containsKey("findsOfCrystals")
            && parameters["findsOfCrystals"] is String) {
      findsOfCrystals = parameters["findsOfCrystals"] as String
    } else throw IllegalArgumentException("$ep type of selection finds of crystals")

    if (parameters.containsKey("useAmendment")
            && parameters["useAmendment"] is Boolean) {
      useAmendment = parameters["useAmendment"] as Boolean
    } else throw IllegalArgumentException("$ep amendment")

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

  @Throws(ExcelException::class, IOException::class)
  private fun createOutputFiles(file: File) {
    val titleObservationsPoints = getTitleTableOfIsihogyClient(
            getSheetOfIsihogyClient(file, observationsPointsSheetName))
    if (titleObservationsPoints.isEmpty()) {
      throw ExcelException("Нет названий полей в листе для точек наблюдений")
    }

    val titleMineralogy = getTitleTableOfIsihogyClient(
            getSheetOfIsihogyClient(file, mineralogySheetName)).toMutableList()
    if (titleMineralogy.isEmpty()) {
      throw ExcelException("Нет названий полей в листе с данными минералогии")
    }
    attributesOfMineralogy = ArrayList(titleMineralogy)

    val topWellsPath = Paths.get(outputFolder + s + nameOfTopWellsFile)
    topWellsFile = MicromineTextFile(topWellsPath)
    topWellsFile.writeTitle(titleObservationsPoints)

    val intervalWellsPath = Paths.get(outputFolder + s + nameOfIntervalWellsFile)
    intervalWellsFile = MicromineTextFile(intervalWellsPath)
    // добавить в заголовок атрибут рассчитываемого параметра "Все МСА"
    titleMineralogy.add(nameOfAttributeAllMinerals)
    intervalWellsFile.writeTitle(titleMineralogy)

    if (createDotFile) {
      val titleDot = ArrayList(titleObservationsPoints)
      // из таблицы минералогии добавляются все атрибуты кроме
      // nameOfAttributeID и nameOfAttributeUIN, поскольку они
      // уже есть в таблице точек наблюдений
      titleDot.addAll(titleMineralogy.filter
        { it != nameOfAttributeID && it != nameOfAttributeUIN })
      val dotWellsPath = Paths.get(outputFolder + s + nameOfDotWellsFile)
      dotWellsFile = MicromineTextFile(dotWellsPath)
      dotWellsFile.writeTitle(titleDot)
      }
  }

  @Throws(ExcelException::class, DataException::class)
  private fun getTablesAndDecodeFields(file: File) {
    var sheet = getSheetOfIsihogyClient(file, observationsPointsSheetName)
    observationsPointsTable = getTableOfIsihogyClient(sheet)
    if (observationsPointsTable.isEmpty()) {
      logger.info("The excel list for points of observations is empty")
      throw DataException("Лист excel с точками наблюдений - пуст")
    }

    sheet = getSheetOfIsihogyClient(file, mineralogySheetName)
    mineralogyTable = getTableOfIsihogyClient(sheet)
    if (mineralogyTable.isEmpty()) {
      logger.info("The excel list for mineralogy is empty")
      throw DataException("Лист excel с данными минералогии - пуст")
    }

    sheet = getSheetOfIsihogyClient(file, stateDocumentationCodesSheetName)
    val stateDocumentationCodes = getCodesOfIsihogyClient(sheet)
    if (stateDocumentationCodes.isEmpty()) {
      logger.info("The excel list for codes of state documentation is empty")
      throw DataException("Лист excel с кодами состояния документирования - пуст")
    }

    sheet = getSheetOfIsihogyClient(file, typeOfProbesCodesSheetName)
    val typeOfProbesCodes = getCodesOfIsihogyClient(sheet)
    if (typeOfProbesCodes.isEmpty()) {
      logger.info("The excel list for type of probes is empty")
      throw DataException("Лист excel с кодами типов проб - пуст")
    }

    sheet = getSheetOfIsihogyClient(file, stratigraphicCodesSheetName)
    val stratigraphicCodes = getCodesOfIsihogyClient(sheet)
    if (stratigraphicCodes.isEmpty()) {
      logger.info("The excel list for codes of stratigraphic is empty")
      throw DataException("Лист excel с кодами стратиграфии - пуст")
    }

    sheet = getSheetOfIsihogyClient(file, lithologyCodesSheetName)
    val lithologyCodes = getCodesOfIsihogyClient(sheet)
    if (lithologyCodes.isEmpty()) {
      logger.info("The excel list for codes of lithology is empty")
      throw DataException("Лист excel с кодами литологии - пуст")
    }

    decodingField(observationsPointsTable, stateDocumentationCodes)
    decodingField(mineralogyTable, typeOfProbesCodes)
    decodingField(mineralogyTable, stratigraphicCodes)
    decodingField(mineralogyTable, lithologyCodes)
  }

  override fun printIntro() {
    task.printConsole("Входные параметры: ")
    task.printConsole("Каталог с входными excel-файлами: ")
    task.printConsole(inputFolder)
    task.printConsole("Каталог с выходными файлами micromine: ")
    task.printConsole(outputFolder)
    task.printConsole("Выборка по стратиграфии: $typeOfSelectionAge")
    task.printConsole("Выборка по находкам МСА: $findsOfCrystals")
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
