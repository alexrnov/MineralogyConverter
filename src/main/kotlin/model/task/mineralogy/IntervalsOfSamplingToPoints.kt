package model.task.mineralogy

import model.exception.GeoTaskException
import model.file.MicromineTextFile
import model.task.GeoTaskOneFile
import model.utils.CollectionUtils
import model.utils.addPointsToIntervals
import model.utils.calculateAbsZForAdditionalPoints
import java.io.BufferedReader
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

typealias AddAttributes = (MutableMap<String, String>, List<String>) -> Unit
typealias CalculationsTask = (List<MutableMap<String, String>>) -> Unit

// количество атрибутов во входном файле интервалов со всеми пробами
// (с находками МСА и без таковых). В этом файле на одно атрибутивное
// поле больше("находки"), но оно расположено в самом конце и не влияет
// на порядок индексов предшедствующх атрибутов
const val numberAttributesAllProbes = 268

// колчество атрибутов во входном файле интервалов только для непустых проб
const val numberAttributesNonEmptyProbes = numberAttributesAllProbes - 1

/**
 * Задача "Интервалы опробования в точки".
 */
class IntervalsOfSamplingToPoints

@Throws(IllegalArgumentException::class)
constructor(parameters: Map<String, Any>): GeoTaskOneFile(parameters) {

  private val inputFile: String by parameters
  private val outputFile: String by parameters
  private val frequency: Int by parameters
  private val taskName: String by parameters

  private lateinit var inputFilePath: Path
  private lateinit var outputFilePath: Path

  // коллекция с пробами, которые были считаны из файла.
  // Для проб остаются только необходимые атрибуты
  private val simpleProbes = ArrayList<MutableMap<String, String>>()

  private var currentPoints: List<MutableMap<String, String>> = ArrayList()
  val getCurrentPoints get() = CollectionUtils.copyListWithSubMap(currentPoints.toList())
  val allPoints: MutableList<MutableMap<String, String>> = ArrayList()
  var test = false

  // названия атрибутов во входном файле
  private var namesOfAttributes: List<String> = ArrayList()

  // функция выполняет выделение точек со стратиграфическим индексом, указанном
  // во входных параметрах и с наличием находок МСА. Это может быть необходимо
  // для Micromine - когда формирование шлихоминералогических ореолов выполняется по возрастам
  private var calculationsTask: CalculationsTask = { }

  // функция добавляет из исходного файла только те атрибуты, которые нужны для вычислений
  private var addAttributes: AddAttributes = { _, _ -> }
  private var dotWellsFile: MicromineTextFile
  private var firstWell = true
  init {
    checkInputParameters()
    dotWellsFile = MicromineTextFile(outputFilePath)
  }

  @Throws(SecurityException::class, IOException::class)
  override fun getTableFromFile(): Collection<Any?> {
    val br: BufferedReader = Files.newBufferedReader(inputFilePath,
            Charset.forName("windows-1251"))

    var firstLine = true
    br.use { // try с ресурсами
      var noEnd = true
      var line: String?
      while (noEnd) {
        line = it.readLine()
        if (line!= null) {
          val lineAsString = line.split(";")
          if (lineAsString.size !in numberAttributesNonEmptyProbes..numberAttributesAllProbes)
            throw IOException("Неверный формат входного файла")
          if (firstLine) {
            namesOfAttributes = lineAsString.toList()
            val algorithm = TypeOfCalculationsTasks(taskName, namesOfAttributes).getAlgorithm()
            addAttributes = algorithm.first // передать алгоритм добавления атрибутов
            calculationsTask = algorithm.second // алогитм для вычислений для текущей задачи
            //dotWellsFile.writeTitle(namesOfAttributes)
            firstLine = false
          } else simpleProbes.add(probeWithNecessaryAttributes(lineAsString, addAttributes))
        } else noEnd = false
      }
    }

    if (simpleProbes.size < 2) throw IOException("Неверный формат входного файла")

    return simpleProbes.stream()
            .map { it[namesOfAttributes[1]] }
            .collect(Collectors.toSet()) // вернуть набор уникальных id скважин
  }

  @Throws(GeoTaskException::class)
  override fun perform(any: Any?) {
    try {
      val idWell = any as String
      val layersForCurrentWell: List<MutableMap<String, String>> = simpleProbes.filter { it[namesOfAttributes[1]] == idWell }
      calculationsTask.invoke(layersForCurrentWell) // Как паттерн ШАБЛОННЫЙ МЕТОД (заменяемая часть алгоритма)
      currentPoints = addPointsToIntervals(layersForCurrentWell, frequency)
      calculateAbsZForAdditionalPoints(currentPoints)

      if (firstWell && currentPoints.isNotEmpty()) {
        dotWellsFile.writeTitle(currentPoints[0].keys.toList())
        firstWell = false
      }
      if (test) allPoints.addAll(currentPoints)
      dotWellsFile.writeContent(currentPoints)
    } catch(e: Exception) {
      throw GeoTaskException(e.message?.let { e.message } ?: "perform error")
    }
  }

  @Throws(SecurityException::class, IOException::class)
  override fun writeData() {
    //val title = dotWells[0].namesOfAttributes.toList()
    //val dotWellsFile = MicromineTextFile(outputFilePath)
    //dotWellsFile.writeTitle(title)
    //dotWellsFile.writeContent(dotWells)
  }

  @Throws(IllegalArgumentException::class)
  private fun checkInputParameters() {
    try {
      inputFilePath = Paths.get(inputFile)
      outputFilePath = Paths.get(outputFile)
    } catch (e: InvalidPathException) {
      throw IllegalArgumentException("invalid path input or output file")
    }
  }

  override fun printIntro() {
    task.printConsole("Входные параметры: ")
    task.printConsole("Входной файл: ${ inputFilePath.toAbsolutePath() }")
    task.printConsole("Выходной файл: ${ outputFilePath.toAbsolutePath() }")
    var s = "Дополнительные вычисления: "
    s += if (taskName.isNotEmpty()) taskName else "нет"
    task.printConsole(s)
    task.printConsole("")
  }

  override fun printReport() {
    task.printConsole("")
    task.printConsole("В выходной файл записано точек: ")
  }

  /**
   * Функция читает пробы, и возвращает коллекцию упрощенных
   * проб (т.е. имеющих меньшее количество атрибутов, необходимое для
   * требуемых вычислительных задач)
   */
  @Throws(IOException::class)
  private inline fun probeWithNecessaryAttributes(currentProbeList: List<String>, addAttributes: AddAttributes):
          MutableMap<String, String> {
    val simpleProbeMap = HashMap<String, String>()
    simpleProbeMap[namesOfAttributes[1]] = currentProbeList[1] // ID
    simpleProbeMap[namesOfAttributes[7]] = currentProbeList[7] // east
    simpleProbeMap[namesOfAttributes[8]] = currentProbeList[8] // north
    simpleProbeMap[namesOfAttributes[9]] = currentProbeList[9] // z
    simpleProbeMap[namesOfAttributes[11]] = currentProbeList[11] // from
    simpleProbeMap[namesOfAttributes[12]] = currentProbeList[12] // to
    simpleProbeMap[namesOfAttributes[23]] = currentProbeList[23] // all MSD
    // добавить набор атрибутов, необходимых в рамках решаемой задачи
    addAttributes.invoke(simpleProbeMap, currentProbeList) // Как паттерн ШАБЛОННЫЙ МЕТОД (заменяемая часть алгоритма)
    return simpleProbeMap
  }
}
