package model.task.structure

import application.Mineralogy.logger
import model.constants.IsihogyClientConstants.lithologyCodesSheetName
import model.constants.IsihogyClientConstants.lithostratigraphicSheetName
import model.constants.IsihogyClientConstants.nameOfAttributeCodeTypeTN
import model.constants.IsihogyClientConstants.nameOfAttributeID
import model.constants.IsihogyClientConstants.nameOfAttributeLCodeAge
import model.constants.IsihogyClientConstants.nameOfAttributeLCodeDescription
import model.constants.IsihogyClientConstants.nameOfAttributeLCodeLithology
import model.constants.IsihogyClientConstants.nameOfAttributeX
import model.constants.IsihogyClientConstants.nameOfAttributeY
import model.constants.IsihogyClientConstants.nameOfAttributeZ
import model.constants.IsihogyClientConstants.observationsPointsSheetName
import model.constants.IsihogyClientConstants.stateDocumentationCodesSheetName
import model.constants.IsihogyClientConstants.stratigraphicCodesSheetName
import model.constants.IsihogyClientConstants.typeOfWellCodesSheetName
import model.exception.DataException
import model.exception.ExcelException
import model.exception.GeoTaskException
import model.file.MicromineTextFile
import model.task.GeoTaskManyFiles
import model.utils.*
import model.utils.CollectionUtils.copyListWithSubMap
import model.utils.ExcelUtils.getCodesOfIsihogyClient
import model.utils.ExcelUtils.getSheetOfIsihogyClient
import model.utils.ExcelUtils.getTableOfIsihogyClient
import model.utils.ExcelUtils.getTitleTableOfIsihogyClient
import model.utils.IsihogyClientUtils.absOfFromTo
import model.utils.IsihogyClientUtils.checkOnMissDataXYZDAndFix
import model.utils.IsihogyClientUtils.decodingField
import model.utils.IsihogyClientUtils.deleteDecimalPart
import model.utils.IsihogyClientUtils.interchangeXY
import model.utils.IsihogyClientUtils.makeAmendment
import java.io.File
import java.io.File.separator as s
import java.io.IOException
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

class RoofOfBaseLayersToPoints

@Throws(IllegalArgumentException::class, SecurityException::class,
        IOException::class, ExcelException::class)
constructor(parameters: Map<String, Any>): GeoTaskManyFiles(parameters) {
  /* входные параметры */
  private val inputFolder: String by parameters // каталог с входными файлами
  private val outputFile: String by parameters // выходной файл
  private val ageIndexes: String by parameters // стратиграфический индекс
  private val useAmendment: Boolean by parameters // поправка ИСИХОГИ

  var ageIndexesAsList: List<String> = ArrayList()
    private set

  /* объект для записи точечных данных в файл micromine */
  private lateinit var dotWellsFile: MicromineTextFile

  // список строк из листа "Точки наблюдений" (на базе текущего excel-файла)
  private var observationsPointsTable: List<MutableMap<String, String>> = ArrayList()

  // список строк из листа "Стратиграфия" (на базе текущего excel-файла)
  private var stratigraphicTable: List<MutableMap<String, String>> = ArrayList()
  val getStratigraphicTable get() = copyListWithSubMap(stratigraphicTable)

  /* общее количество точек для файла точек */
  private var overallNumberDotWells = 0

  private lateinit var outputFilePath: Path

  init {
    checkInputParameters()
    inputFiles = ExcelUtils.listOfExcelFiles(inputFolder)
    if (inputFiles.isNotEmpty()) {
      // взять заголовок для выходного файла из первого входного файла
      createOutputFile(inputFiles[0])
    }
  }

  @Throws(GeoTaskException::class)
  override fun perform(file: File) {
    try {
      getTablesAndDecodeFields(file)
      deleteDecimalPart(nameOfAttributeID, observationsPointsTable)
      val mistakes = checkOnMissDataXYZDAndFix(observationsPointsTable)
      if (mistakes.isNotEmpty()) {
        task.printConsole("Ошибки отсутствия данных:")
        mistakes.forEach { task.printConsole(it) }
      }
      interchangeXY(observationsPointsTable)
      if (useAmendment) makeAmendment(observationsPointsTable)
      deleteDecimalPart(nameOfAttributeID, stratigraphicTable)
      stratigraphicTable = stratigraphicTable.leavingTopBaseLayers()
      stratigraphicTable.forEach { layer ->
        if (observationsPointsTable.any {it[nameOfAttributeID] == layer[nameOfAttributeID]}) {
          val necessaryAttributes = observationsPointsTable.first { it[nameOfAttributeID] == layer[nameOfAttributeID] }
          necessaryAttributes.keys.retainAll(listOf(nameOfAttributeX, nameOfAttributeY,
                  nameOfAttributeZ, nameOfAttributeCodeTypeTN))
          // вставить необходимые пары ключ-значение из листа "Точки наблюдений"
          // в таблицу со стратиграфией
          layer.putAll(necessaryAttributes)
        } else {
          task.printConsole("Для стратиграфического слоя нет данных по " +
                  "точке наблюдения")
          logger.info("For stratigraphic layer not information by observation point. " +
                  "Excel-file: " + file.name)
        }
      }
      absOfFromTo(stratigraphicTable) // деструктурирование
      // удалить некоторые атрибуты
      stratigraphicTable.forEach {
        it.keys.removeAll(setOf(nameOfAttributeLCodeLithology, nameOfAttributeLCodeDescription))
      }
      task.printConsole("Из файла прочитано скважин: " +
              "${observationsPointsTable.size}")
      if (stratigraphicTable.isNotEmpty()) {
        overallNumberDotWells += stratigraphicTable.size
        dotWellsFile.writeContent(stratigraphicTable)
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
    try {
      outputFilePath = Paths.get(outputFile)
    } catch (e: InvalidPathException) {
      throw IllegalArgumentException("invalid path output file")
    }
    ageIndexesAsList = ageIndexes.split(";")
            .map { it.trim() } // удалить лишние пробелы
            .filter { it.isNotEmpty() } // удалить пустые записи
  }

  @Throws(ExcelException::class, IOException::class)
  private fun createOutputFile(file: File) {

    val titleObservationsPoints = getTitleTableOfIsihogyClient(
            getSheetOfIsihogyClient(file, observationsPointsSheetName))
    if (titleObservationsPoints.isEmpty()) {
      throw ExcelException("Нет названий полей в листе для точек наблюдений")
    }

    val titleLithostratigraphic = getTitleTableOfIsihogyClient(
            getSheetOfIsihogyClient(file, lithostratigraphicSheetName))
    if (titleLithostratigraphic.isEmpty()) {
      throw ExcelException("Нет названий полей в листе для литостратиграфии")
    }

    val titleDot = ArrayList(titleLithostratigraphic)
    titleDot.addAll(listOf(nameOfAttributeX, nameOfAttributeY,
            nameOfAttributeZ, nameOfAttributeCodeTypeTN))
    titleDot.removeAll(setOf(nameOfAttributeLCodeLithology, nameOfAttributeLCodeDescription))

    dotWellsFile = MicromineTextFile(outputFilePath)
    dotWellsFile.writeTitle(titleDot)
  }

  @Throws(ExcelException::class, DataException::class)
  private fun getTablesAndDecodeFields(file: File) {
    var sheet = getSheetOfIsihogyClient(file, observationsPointsSheetName)
    observationsPointsTable = getTableOfIsihogyClient(sheet)
    if (observationsPointsTable.isEmpty()) {
      logger.info("The excel list for points of observations is empty")
      throw DataException("Лист excel с точками наблюдений - пуст")
    }

    sheet = getSheetOfIsihogyClient(file, stateDocumentationCodesSheetName)
    val stateDocumentationCodes = getCodesOfIsihogyClient(sheet)
    if (stateDocumentationCodes.isEmpty()) {
      logger.info("The excel list for codes of state documentation is empty")
      throw DataException("Лист excel с кодами состояния документирования - пуст")
    }

    sheet = getSheetOfIsihogyClient(file, typeOfWellCodesSheetName)
    val typeOfWellCodes = getCodesOfIsihogyClient(sheet)
    if (typeOfWellCodes.isEmpty()) {
      logger.info("The excel list for codes of type of well is empty")
      throw DataException("Лист excel с кодами типов скважин - пуст")
    }
    // Из-за несоответствий названий атрибута в таблице "Точки наблюдений"
    // ("Код типа ТН") и в таблице кодовых значений
    // ("Код типа Точки наблюдений"), приходится дополнительно
    // заменить значение nameOfCodeAttribute, возвращенное методом.
    typeOfWellCodes["nameOfCodeAttribute"] = "Код типа ТН"

    decodingField(observationsPointsTable, stateDocumentationCodes)
    decodingField(observationsPointsTable, typeOfWellCodes)

    sheet = getSheetOfIsihogyClient(file, lithostratigraphicSheetName)
    stratigraphicTable = getTableOfIsihogyClient(sheet)
    if (stratigraphicTable.isEmpty()) {
      logger.info("The excel list for stratigraphic is empty")
      throw DataException("Лист excel со стратиграфией - пуст")
    }

    sheet = getSheetOfIsihogyClient(file, stratigraphicCodesSheetName)
    val stratigraphicCodes = getCodesOfIsihogyClient(sheet)
    if (stratigraphicCodes.isEmpty()) {
      logger.info("The excel list for stratigraphic codes is empty")
      throw DataException("Лист excel с кодами стратиграфии - пуст")
    }

    sheet = getSheetOfIsihogyClient(file, lithologyCodesSheetName)
    val lithologyCodes = getCodesOfIsihogyClient(sheet)
    if (lithologyCodes.isEmpty()) {
      logger.info("The excel list for lithology codes is empty")
      throw DataException("Лист excel с кодами литологии - пуст")
    }

    decodingField(stratigraphicTable, stratigraphicCodes)
    decodingField(stratigraphicTable, lithologyCodes)
  }

  override fun printIntro() {
    task.printConsole("Входные параметры:")
    task.printConsole("Каталог с входными excel-файлами: ")
    task.printConsole(inputFolder)
    task.printConsole("Выходной файл для Micromine: ")
    task.printConsole("${outputFilePath.toAbsolutePath()}")
    task.printConsole("Стратиграфический индекс/индексы: $ageIndexes")
    task.printConsole("Убрать поправку ИСИХОГИ для координат X и Y: " +
            if (useAmendment) "Да" else "Нет")
    task.printConsole("")
  }

  override fun printReport() {
    task.printConsole("")
    task.printConsole("Файл точек для Micromine, полученных по " +
            "кровле кимберлитовмещающих отложений:")
    task.printConsole("${outputFilePath.toAbsolutePath()}")
    task.printConsole("Общее количество точек, записанных в точечный файл: " +
            overallNumberDotWells)
  }

  /* функция возвращает список слоев, являющихся первыми (верхними) вмещающими отложениями  */
  /* другие реализации алгоритма по поиску первого индекса вмещающих отложений
  run Find@{ ageIndexesAsList.forEach { ageIndex ->
      firstBaseLayer = layersForCurrentWell.firstOrNull { it[nameOfAttributeLCodeAge] == ageIndex }
      if (firstBaseLayer != null) return@Find
    }
  }
  var i = 0
  while (i < ageIndexesAsList.size && firstBaseLayer == null) {
    firstBaseLayer = layersForCurrentWell.firstOrNull { it[nameOfAttributeLCodeAge] == ageIndexesAsList[i] }
    i++
  }
  val ageIndex = ageIndexesAsList.iterator()
  while (ageIndex.hasNext() && firstBaseLayer == null) {
    val index = ageIndex.next()
    firstBaseLayer = layersForCurrentWell.firstOrNull { it[nameOfAttributeLCodeAge] == index }
  } */
  private fun List<MutableMap<String, String>>.leavingTopBaseLayers(): List<MutableMap<String, String>> {
    val baseLayers = ArrayList<MutableMap<String, String>>()
    val ids = this.stream() // получить набор уникальных id скважин
            .map { it[nameOfAttributeID] }
            .collect(Collectors.toSet())
    ids.forEach { idWell -> // перебор скважин
      val layersForCurrentWell = this.filter { it[nameOfAttributeID] == idWell }
      val firstBaseLayer: Map<String, String>?
      // найти первое совпадение с индексом вмещающих отложений
       // перебор всех индексов вмещающих отложений
        firstBaseLayer = layersForCurrentWell.firstOrNull {
          val currentAgeIndex = it[nameOfAttributeLCodeAge] ?: ""
          var b = false
          // если текущий индекс соответсвтует одному из индексов вмещающих отложений
          for (ageIndex in ageIndexesAsList) {
            // проверяется вложенность индексов (т.е. O1ol и O1or будут приравнены к O)
            b = currentAgeIndex.contains(ageIndex)
            // при поиске точного соответсвия следует использовать условие:
            // currentAgeIndex = ageIndex
            if (b) break // если совпадение найдено - выйти из цикла
          }
          b
        }


      firstBaseLayer?.let { baseLayers.add(firstBaseLayer.toMutableMap())}
    }
    return baseLayers
  }
}
