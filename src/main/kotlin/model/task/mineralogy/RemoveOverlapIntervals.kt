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
    val probesID = ArrayList<String>()
    var firstLine = true
    br.use { // try с ресурсами
      var noEnd = true
      var line: String?
      while (noEnd) {
        line = it.readLine()
        if (line != null) {
          val lineAsList = line.split(";")
          if (firstLine) {
            keys = lineAsList
            firstLine = false
          } else probesID.add(lineAsList[1])
        } else noEnd = false
      }
    }
    return HashSet(probesID) // вернуть набор уникальных id скважин
  }

  @Throws(GeoTaskException::class)
  override fun perform(any: Any?) {
    try {
      val idWell = any as String

      val br: BufferedReader = Files.newBufferedReader(inputFilePath,
              Charset.forName("windows-1251"))

      val probesForCurrentWell = ArrayList<Map<String, String>>()
      br.use { // try с ресурсами
        var noEnd = true
        var line: String?
        while (noEnd) {
          line = it.readLine()
          if (line != null) {
            val currentLineAsList: List<String> = line.split(";")
            if (currentLineAsList[1] == idWell) {
              val key = keys.iterator()
              val v = currentLineAsList.associate { value -> Pair(key.next(), value)}
              probesForCurrentWell.add(v)
            }
          }
          else noEnd = false
        }
      }

      probesForCurrentWell.forEach {
        println("${it["ID"]} ${it["От"]} ${it["До"]} ${it["Тип_пробы"]} ${it["Все_МСА"]}")
      }

      val probesWithMSD: List<Map<String, String>> = probesForCurrentWell.filter { (it["Все_МСА"]?.toDouble() ?: 0.0) > 0.0 }
      val emptyProbes = probesForCurrentWell.toMutableList()
      emptyProbes.removeAll(probesWithMSD)

      println("-")
      probesWithMSD.forEach {
        println("${it["ID"]} ${it["От"]} ${it["До"]} ${it["Тип_пробы"]} ${it["Все_МСА"]}")
      }
      println("-")
      emptyProbes.forEach {
        println("${it["ID"]} ${it["От"]} ${it["До"]} ${it["Тип_пробы"]} ${it["Все_МСА"]}")
      }
      println("-------------------------------")

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
    task.printConsole(s)
    task.printConsole("")
  }

  override fun printReport() {
    task.printConsole("")
    task.printConsole("В выходной файл записано точек: ${dotWells.size}")
  }
}
