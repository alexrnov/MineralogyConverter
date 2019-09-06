package model.task.structure

import application.Mineralogy.logger
import model.constants.IsihogyClientConstants.nameOfAttributeBottomWell
import model.constants.IsihogyClientConstants.nameOfAttributeID
import model.constants.IsihogyClientConstants.observationsPointsSheetName
import model.constants.IsihogyClientConstants.stateDocumentationCodesSheetName
import model.exception.DataException
import model.exception.ExcelException
import model.exception.GeoTaskException
import model.file.MicromineTextFile
import model.task.GeoTaskManyFiles
import model.utils.CollectionUtils.copyListWithSubMap
import model.utils.CollectionUtils.retainRequiredFields
import model.utils.ExcelUtils
import model.utils.ExcelUtils.getCodesOfIsihogyClient
import model.utils.ExcelUtils.getSheetOfIsihogyClient
import model.utils.ExcelUtils.getTableOfIsihogyClient
import model.utils.ExcelUtils.getTitleTableOfIsihogyClient
import model.utils.IsihogyClientUtils.checkOnMissDataXYZDAndFix
import model.utils.IsihogyClientUtils.decodingField
import model.utils.IsihogyClientUtils.deleteDecimalPart
import model.utils.IsihogyClientUtils.interchangeXY
import model.utils.IsihogyClientUtils.makeAmendment
import model.utils.IsihogyClientUtils.putBottomOfWellValue
import java.io.File
import java.io.File.separator as s
import java.io.IOException
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Задача "Устье/забой скважин" (ИСИХОГИ). Из excel-файлов
 * ИСИХОГИ, извлекаются данные абсолютных отметок устьев
 * скважин и глубины скважин.
 */
class TopAndBottomOfWells

@Throws(IllegalArgumentException::class, SecurityException::class,
        IOException::class, ExcelException::class)
constructor(parameters: Map<String, Any>): GeoTaskManyFiles(parameters) {
  /* входные параметры */
  private val inputFolder: String by parameters
  private val outputFile: String by parameters
  // экспортировать все атрибуты не нужно, когда необходимо в Micromine
  // конвертировать точки в слой shape, поскольку shp повреждается
  // при наличии атрибутивных полей, записанных кириллицей
  private val exportAllFields: Boolean by parameters // экспортировать все атрибуты
  private val useAmendment: Boolean by parameters // поправка ИСИХОГИ

  /* объект для записи точечных данных в файл micromine */
  private lateinit var dotWellsFile: MicromineTextFile

  // список строк для записи в файл точек (на базе текущего excel-файла)
  private var observationsPointsTable: List<MutableMap<String, String>> = ArrayList()
  val getObservationsPointsTable get() = copyListWithSubMap(observationsPointsTable)

  /* общее количество точек для файла точек */
  private var overallNumberDotWells = 0
  private val requiredFields = setOf("ID ТН", "X факт.", "Y факт.", "Z", "D", "UIN")
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
      putBottomOfWellValue(observationsPointsTable)
      interchangeXY(observationsPointsTable)

      if (useAmendment) makeAmendment(observationsPointsTable)
      if (!exportAllFields) {
        retainRequiredFields(observationsPointsTable, requiredFields)
      }
      task.printConsole("Из файла прочитано скважин: " +
              "${observationsPointsTable.size}")
      overallNumberDotWells += observationsPointsTable.size
      dotWellsFile.writeContent(observationsPointsTable)
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
  }

  @Throws(ExcelException::class, IOException::class)
  private fun createOutputFile(file: File) {
    val titleObservationsPoints = getTitleTableOfIsihogyClient(
            getSheetOfIsihogyClient(file, observationsPointsSheetName))
    if (titleObservationsPoints.isEmpty()) {
      throw ExcelException("Нет названий полей в листе для точек наблюдений")
    }
    val titleDot = ArrayList(titleObservationsPoints)
    titleDot.add(nameOfAttributeBottomWell)
    if (!exportAllFields) titleDot.retainAll(requiredFields)
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
    decodingField(observationsPointsTable, stateDocumentationCodes)
  }

  override fun printIntro() {
    task.printConsole("Входные параметры: ")
    task.printConsole("Каталог с входными excel-файлами: ")
    task.printConsole(inputFolder)
    task.printConsole("Выходной файл для Micromine: ")
    task.printConsole("${outputFilePath.toAbsolutePath()}")
    task.printConsole("Экспортировать все атрибуты: " +
            if (exportAllFields) "Да" else "Нет")
    task.printConsole("Убрать поправку ИСИХОГИ для координат X и Y: " +
            if (useAmendment) "Да" else "Нет")
    task.printConsole("")
  }

  override fun printReport() {
    task.printConsole("")
    task.printConsole("Файл точек с абсолютными отметками по " +
            "устью/подошве скважин для Micromine:")
    task.printConsole("${dotWellsFile.file.toAbsolutePath()}")
    task.printConsole("Общее количество точек, записанных в точечный файл: " +
              overallNumberDotWells)
  }
}
