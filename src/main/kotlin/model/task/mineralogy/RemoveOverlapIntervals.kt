package model.task.mineralogy

import model.exception.GeoTaskException
import model.file.MicromineTextFile
import model.task.GeoTaskOneFile
import java.io.BufferedReader
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths

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

  // названия необходимых атрибутов во входном/выходном файле
  private var namesOfAttributes: List<String> = ArrayList()

  private var numberWell = 0
  private var intervalWellsFile: MicromineTextFile

  init {
    checkInputParameters()
    intervalWellsFile = MicromineTextFile(outputFilePath)
  }

  @Throws(SecurityException::class, IOException::class)
  override fun getTableFromFile(): Collection<Any?> {
    val br: BufferedReader = Files.newBufferedReader(inputFilePath,
            Charset.forName("windows-1251"))
    val probesID = ArrayList<String>()
    var firstLineOfFile = true
    br.use { // try с ресурсами
      var noEnd = true
      var line: String?
      while (noEnd) {
        line = it.readLine()
        if (line != null) {
          val lineAsList = line.split(";")
          if (firstLineOfFile) {
            namesOfAttributes = lineAsList
            intervalWellsFile.writeTitle(namesOfAttributes) // записать в выходной файл названия атрибутов
            firstLineOfFile = false
          } else probesID.add(lineAsList[1])
        } else noEnd = false
      }
    }
    return HashSet(probesID) // вернуть набор уникальных id скважин
  }

  @Throws(GeoTaskException::class)
  override fun perform(any: Any?) {
    try {
      println(numberWell++)
      val idWell = any as String

      val br: BufferedReader = Files.newBufferedReader(inputFilePath,
              Charset.forName("windows-1251"))

      val probesForCurrentWell = ArrayList<Map<String, String>>()
      /*
      * Из файла считываются все пробы по текущей скважине. Чтение файла
      * для каждой скважины производится для того, чтобы не загружать
      * весь файл в память, т.к. он можен занимать несколько десятков мб
      */
      br.use { // try с ресурсами
        var noEnd = true
        var line: String?
        while (noEnd) {
          line = it.readLine()
          if (line != null) {
            val currentLineAsList: List<String> = line.split(";")
            if (currentLineAsList[1] == idWell) {
              val iterator = namesOfAttributes.iterator()
              // создать отображение с парами: название атрибута - его значение
              val v = currentLineAsList.associate { value -> Pair(iterator.next(), value)}
              probesForCurrentWell.add(v)
            }
          }
          else noEnd = false
        }
      }

      println("allProbes: ")
      probesForCurrentWell.forEach {
        println("${it["IDW"]} ${it["ID"]} ${it["От"]} ${it["До"]} ${it["Тип_пробы"]} ${it["Все_МСА"]}")
      }

      val probesWithMSD: List<Map<String, String>> = probesForCurrentWell.filter { (it["Все_МСА"]?.toDouble() ?: 0.0) > 0.0 }
      val emptyProbes = probesForCurrentWell.toMutableList()
      emptyProbes.removeAll(probesWithMSD)

      val resultIntervals: MutableList<Map<String, String>> = ArrayList()
      for (emptyProbe in emptyProbes) {
        val fromEmpty = emptyProbe["От"]?.toDouble() ?: 0.0
        val toEmpty = emptyProbe["До"]?.toDouble() ?: 0.0
        val res: HashSet<String> = overlap(fromEmpty, toEmpty, probesWithMSD)
        res.forEach {
          val intervals = it.split(";")
          if (!resultIntervals.any { probe -> probe["От"] == intervals[0] && probe["До"] == intervals[1] }) {
            val i = emptyProbe.toMutableMap()
            i["От"] = intervals[0]
            i["До"] = intervals[1]
            resultIntervals.add(i)
          }
        }
      }

      println("-")
      println("result empty intervals:")
      resultIntervals.forEach {
        println("${it["IDW"]} ${it["ID"]} ${it["От"]} ${it["До"]} ${it["Тип_пробы"]} ${it["Все_МСА"]}")
      }
      resultIntervals.addAll(probesWithMSD) // добавить к новым пустым интервалам пробы с находками МСА
      println("-")
      println("templateList: ")
      resultIntervals.forEach {
        println("${it["IDW"]} ${it["ID"]} ${it["От"]} ${it["До"]} ${it["Тип_пробы"]} ${it["Все_МСА"]}")
      }

      intervalWellsFile.writeContent(resultIntervals)

      println("------------------------")
    } catch(e: Exception) {
      throw GeoTaskException(e.message?.let { e.message } ?: "perform error")
    }
  }

  @Throws(SecurityException::class, IOException::class)
  override fun writeData() { }

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
    task.printConsole("")
  }

  override fun printReport() {
    task.printConsole("")
    task.printConsole("Преобразование завершено")
  }

  private fun overlap(pFromEmpty: Double, pToEmpty: Double,
                probesWithMSD: List<Map<String, String>>): HashSet<String> {
    var fromEmpty = pFromEmpty
    var toEmpty = pToEmpty

    val resultSet = HashSet<String>()

    for (probeWithMSD in probesWithMSD) {
      val fromMSD = probeWithMSD["От"]?.toDouble() ?: 0.0
      val toMSD = probeWithMSD["До"]?.toDouble() ?: 0.0
      when {
        (toMSD <= fromEmpty || fromMSD >= toEmpty) -> { } // интервал с МСА за пределами пустой пробы)
        (fromMSD <= fromEmpty && toMSD >= toEmpty) -> return resultSet // если проба с МСА полностью перекрывает пустой интевал, вернуть пустой список
        (fromMSD > fromEmpty && toMSD < toEmpty) -> { // интервал с МСА лежит внутри пустого интревала
          val set1 = overlap(fromEmpty, fromMSD, probesWithMSD)
          val set2 = overlap(toMSD, toEmpty, probesWithMSD)
          resultSet.addAll(set1)
          resultSet.addAll(set2)
        }
        (toMSD > fromEmpty && fromMSD <= fromEmpty && toMSD < toEmpty) -> fromEmpty = toMSD // интервал с МСА перекрывает пустую пробу сверху
        (fromMSD < toEmpty && toMSD >= toEmpty && fromMSD > fromEmpty) -> toEmpty = fromMSD // интервал с МСА перекрывает пустую пробу снизу
        else -> println("Другой случай")
      }
    }
    if (resultSet.size == 0) resultSet.add("$fromEmpty;$toEmpty")
    return resultSet
  }
}
