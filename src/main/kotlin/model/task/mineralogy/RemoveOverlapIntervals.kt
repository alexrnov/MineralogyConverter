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

/**
 * Задача "В интервалах пустых проб удалить те интервалы, которые
 * перекрываются с интервалами непустых проб".
 */
class RemoveOverlapIntervals

@Throws(IllegalArgumentException::class)
constructor(parameters: Map<String, Any>): GeoTaskOneFile(parameters) {

  private val inputFile: String by parameters
  private val outputFile: String by parameters
  private val taskName: String by parameters

  private lateinit var inputFilePath: Path
  private lateinit var outputFilePath: Path

  // коллекция с пробами, которые были считаны из файла.
  // Для проб остаются только необходимые атрибуты
  private val simpleProbes = ArrayList<MutableMap<String, String>>()

  // коллекция содержит точки с данными по кровле верхней пробы
  // и подошве нижней пробы. Эти данные записываются в
  // выходной текстовый файл
  private val dotWells: MutableList<MutableMap<String, String>> = ArrayList()
  val getDotWells get() = CollectionUtils.copyListWithSubMap(dotWells.toList())

  // названия необходимых атрибутов во входном/выходном файле
  private var keys: List<String> = ArrayList()

  // функция выполняет выделение точек со стратиграфическим индексом,
  // указанном во входных параметрах и с наличием находок МСА.
  // Это может быть необходимо для Micromine - когда формирование
  // шлихоминералогических ореолов выполняется по возрастам
  private var calculationsTask: CalculationsTask = { }

  init { checkInputParameters() }

  @Throws(SecurityException::class, IOException::class)
  override fun getTableFromFile(): Collection<Any?> {
    val br: BufferedReader = Files.newBufferedReader(inputFilePath,
            Charset.forName("windows-1251"))
    val probes = ArrayList<String>()
    br.use { // try с ресурсами
      var noEnd = true
      var line: String?
      while (noEnd) {
        line = it.readLine()
        noEnd = line?.let { probes.add(line) } ?: false
      }
    }

    if (probes.size < 2) throw IOException("Неверный формат входного файла")
    keys = probes[0].split(";")
    // или if (keys.size != numberAttributesNonEmptyProbes && keys.size != numberAttributesAllProbes)
    if (keys.size !in numberAttributesNonEmptyProbes..numberAttributesAllProbes)
      throw IOException("Неверный формат входного файла")

    val algorithm = TypeOfCalculationsTasks(taskName, keys).getAlgorithm()
    probes.fillSimpleProbes(algorithm.first) // передать алгоритм добавления атрибутов
    calculationsTask = algorithm.second // алогитм для вычислений для текущей задачи

    //simpleProbes[0].forEach { println(it) }
    return simpleProbes.stream()
            .map { it[keys[1]] }
            .collect(Collectors.toSet()) // вернуть набор уникальных id скважин
  }

  @Throws(GeoTaskException::class)
  override fun perform(any: Any?) {
    try {
      val idWell = any as String
      val layersForCurrentWell: List<MutableMap<String, String>> = simpleProbes.filter { it[keys[1]] == idWell }
      calculationsTask.invoke(layersForCurrentWell) // Как паттерн ШАБЛОННЫЙ МЕТОД (заменяемая часть алгоритма)
      val list = addPointsToIntervals(layersForCurrentWell)
      calculateAbsZForAdditionalPoints(list)
      dotWells.addAll(list)
    } catch(e: Exception) {
      throw GeoTaskException(e.message?.let { e.message } ?: "perform error")
    }
  }

  @Throws(SecurityException::class, IOException::class)
  override fun writeData() {
    val title = dotWells[0].keys.toList()
    val dotWellsFile = MicromineTextFile(outputFilePath)
    dotWellsFile.writeTitle(title)
    dotWellsFile.writeContent(dotWells)
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
    task.printConsole("В выходной файл записано точек: ${dotWells.size}")
  }

  /**
   * Функция расширения для списка проб, которая читает эти пробы, и
   * заполняет на основе прочитанных данных коллекцию с упрощенными
   * пробами (т.е. имеющими меньшее количество атрибутов)
   */
  @Throws(IOException::class)
  private inline fun List<String>.fillSimpleProbes(addAttributes: AddAttributes) {
    for (i in 1 until this.size) {
      val currentProbeList = this[i].split(";")
      if (currentProbeList.size != keys.size)
        throw IOException("Неверный формат входного файла")
      val simpleProbeMap = HashMap<String, String>()
      simpleProbeMap[keys[1]] = currentProbeList[1] // ID
      simpleProbeMap[keys[7]] = currentProbeList[7] // east
      simpleProbeMap[keys[8]] = currentProbeList[8] // north
      simpleProbeMap[keys[9]] = currentProbeList[9] // z
      simpleProbeMap[keys[11]] = currentProbeList[11] // from
      simpleProbeMap[keys[12]] = currentProbeList[12] // to
      simpleProbeMap[keys[23]] = currentProbeList[23] // all MSD
      // добавить набор атрибутов, необходимых в рамках решаемой задачи
      addAttributes.invoke(simpleProbeMap, currentProbeList) // Как паттерн ШАБЛОННЫЙ МЕТОД (заменяемая часть алгоритма)
      simpleProbes.add(simpleProbeMap)
    }
  }
}
