package model.task.structure

import application.Mineralogy.logger
import model.constants.IsihogyClientConstants.lithologyCodesSheetName
import model.constants.IsihogyClientConstants.lithostratigraphicSheetName
import model.constants.IsihogyClientConstants.nameOfAttributeID
import model.constants.IsihogyClientConstants.nameOfAttributeLCodeAge
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

/**
 * Задача "Интервалы пластов в точки (ИСИХОГИ)". Из excel-файлов
 * извлекаются данные по стратиграфическим подразделениям. Затем
 * им присваиваится атрибут принадлежности к индексу
 * ("выделенный_пласт"), указанному во входных данных. Если
 * стратиграфических индекс соответствует индексу, указанному во
 * входном параметре, тогда присваивается атрибут со значением "1.0",
 * иначе "0.0". Это нужно для того, чтобы в Micromine можно было
 * создать блочную модель какого-либо стратиграфического пласта,
 * например дяхтарских отложений
 */
class StratigraphyIntervalsToPoints

@Throws(IllegalArgumentException::class, SecurityException::class,
        IOException::class, ExcelException::class)
constructor(parameters: Map<String, Any>): GeoTaskManyFiles(parameters) {
  /* входные параметры */
  private val inputFolder: String by parameters // каталог с входными файлами
  private val outputFile: String by parameters // выходной файл
  private val ageIndexes: String by parameters // стратиграфический индекс
  private val unionLayers: Boolean by parameters // нужно ли объединять пласты
  private val addPoints: Boolean by parameters // нужно ли объединять пачки
  private val useAmendment: Boolean by parameters // поправка ИСИХОГИ

  var ageIndexesAsSet: MutableSet<String> = HashSet()
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

  //private var titleDot = HashSet<String>()

  // атрибут для записи значения 1.0 или 0.0. Значение 1.0 обозначяет,
  // что индекс данного стратиграфического пласта совпал с индексом,
  // указанным во входном параметре, 0.0 - не совпал
  private val attributeOfBooleanStratigraphy = "Выделенный_пласт"

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

      if (unionLayers) { // объединить стратиграфические индексы
        val unionAgeLayers = UnionAgeLayers(stratigraphicTable)
        stratigraphicTable = unionAgeLayers.getTableWithUnionLayers()
      }
      // добавить атрибут совпадения/несовпадения страт. индекса
      stratigraphicTable.map {
        if (it[nameOfAttributeLCodeAge] in ageIndexesAsSet)
          it.put(attributeOfBooleanStratigraphy, "1.0")
        else it.put(attributeOfBooleanStratigraphy, "0.0")
      }

      println(stratigraphicTable[0])
      println(stratigraphicTable[1])
      println(stratigraphicTable[2])
      //println("---------------------")
      if (addPoints) {
        stratigraphicTable = addPointsToIntervals(stratigraphicTable)
        (0..23).forEach { println(stratigraphicTable[it]) }
      }

      // объединить таблицы с данными стратиграфии и данными точек наблюдений
      stratigraphicTable.forEach { row ->
        // если в таблице "Точки наблюдений" найдена скважина с таким же id
        // как и у стратиграфического слоя
        if (observationsPointsTable.any { it[nameOfAttributeID] == row[nameOfAttributeID] }) {
          // вставить все пары ключ-значение из листа "Точки наблюдений"
          // в таблицу со стратиграфией
          row.putAll(observationsPointsTable
                  .first { it[nameOfAttributeID] == row[nameOfAttributeID] })
        } else {
          task.printConsole("Для стратиграфического слоя нет данных по " +
                  "точке наблюдения")
          logger.info("For stratigraphic layer not information by observation point. " +
                  "Excel-file: " + file.name)
        }
      }

      if (addPoints) {
        pointsZOfAdditionalIntervals(stratigraphicTable)
        //(0..23).forEach { println(stratigraphicTable[it]["generateZ"] + " " +
                //stratigraphicTable[it]["Z"]) }
        //println(stratigraphicTable[0])
      } else {
        averageZByInterval(stratigraphicTable)
      }

      stratigraphicTable.forEach {
        it.keys.retainAll(setOf("X факт.", "Y факт.", "Z", attributeOfBooleanStratigraphy))
      }

      //stratigraphicTable.forEach { println(it)}

      task.printConsole("Из файла прочитано скважин: " +
              "${observationsPointsTable.size}")
      overallNumberDotWells += stratigraphicTable.size
      dotWellsFile.writeContentWithoutSomeKeys(stratigraphicTable)
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
    ageIndexesAsSet = ageIndexes.split(";")
            .map { it.trim() } // удалить лишние пробелы
            .filter { it.isNotEmpty() } // удалить пустые записи
            .toHashSet()
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

    //titleDot = HashSet(titleObservationsPoints)
    //titleDot.addAll(titleLithostratigraphic)
    //titleDot.add(attributeOfBooleanStratigraphy)

    dotWellsFile = MicromineTextFile(outputFilePath)
    //dotWellsFile.writeTitle(titleDot.toList())
    dotWellsFile.writeTitle(listOf("X факт.", "Y факт.",
            "Z", attributeOfBooleanStratigraphy))
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
    task.printConsole("Входные параметры: ")
    task.printConsole("Каталог с входными excel-файлами: ")
    task.printConsole(inputFolder)
    task.printConsole("Выходной файл для Micromine: ")
    task.printConsole("${outputFilePath.toAbsolutePath()}")
    task.printConsole("Стратиграфический индекс/индексы: ")
    task.printConsole(ageIndexes)
    task.printConsole("Объединить сопредельные пласты с одинаковым " +
            "стратиграфическим индексом: " + if (unionLayers) "Да" else "Нет")
    task.printConsole("Записать дополнительные точки" +
            if (addPoints) "Да" else "Нет")
    task.printConsole("Убрать поправку ИСИХОГИ для координат X и Y: " +
            if (useAmendment) "Да" else "Нет")
    task.printConsole("")
  }

  override fun printReport() {
    task.printConsole("")
    task.printConsole("Файл точек для Micromine, полученных по " +
            "стратиграфическим интервалам:")
    task.printConsole("${outputFilePath.toAbsolutePath()}")
    task.printConsole("Общее количество точек, записанных в точечный файл: " +
            overallNumberDotWells)
  }
}
