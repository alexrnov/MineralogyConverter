package model.task.mineralogy

import application.Mineralogy.logger
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
 * Реализация задачи "Убрать наложение проб". В интервалах пустых проб
 * удаляются те интервалы, которые перекрываются с интервалами непустых проб".
 */
class RemoveOverlapIntervals

@Throws(IllegalArgumentException::class)
constructor(parameters: Map<String, Any>): GeoTaskOneFile(parameters) {

  private val inputFile: String by parameters
  private val outputFile: String by parameters

  private lateinit var inputFilePath: Path
  private lateinit var outputFilePath: Path

  // названия необходимых атрибутов во входном/выходном файлах
  private var namesOfAttributes: List<String> = ArrayList()

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
    /* Сначала из файла считываются все уникальные id скважин */
    br.use { // try с ресурсами
      var noEnd = true
      var line: String?
      while (noEnd) {
        line = it.readLine()
        if (line != null) {
          val lineAsList = line.split(";")
          if (lineAsList.size < 2) throw IOException("${this.javaClass.simpleName}: error format file")
          if (firstLineOfFile) {
            namesOfAttributes = lineAsList // namesOfAttributes останется неизменной, поскольку дальнейшее присваивание коллекции lineAsList новых значений не повреждает коллекцию namesOfAttributes
            intervalWellsFile.writeTitle(namesOfAttributes) // записать в выходной файл названия атрибутов
            firstLineOfFile = false
          } else probesID.add(lineAsList[1])
        } else noEnd = false
      }
    }
    if (probesID.isEmpty()) throw IOException("${this.javaClass.simpleName}: file is empty")
    return HashSet(probesID) // вернуть набор уникальных id скважин
  }

  @Throws(GeoTaskException::class)
  override fun perform(any: Any?) {
    try {
      val idWell = any as String

      val br: BufferedReader = Files.newBufferedReader(inputFilePath,
              Charset.forName("windows-1251"))

      val probesForCurrentWell = ArrayList<Map<String, String>>()
      /*
      * Из файла считываются все пробы по текущей скважине. Чтение файла
      * для каждой скважины производится для того, чтобы не загружать
      * весь файл в память, т.к. он может занимать несколько десятков мб
      */
      br.use { // try с ресурсами
        var noEnd = true
        var line: String?
        while (noEnd) {
          line = it.readLine()
          if (line != null) {
            val currentLineAsList: List<String> = line.split(";")
            if (currentLineAsList.size != namesOfAttributes.size) {
              throw GeoTaskException("${this.javaClass.simpleName}: Amount values for current line " +
                      "not correspond with amount fields of attributes")
            }
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

      val probesWithMSD: List<Map<String, String>> = probesForCurrentWell.filter { (it["Все_МСА"]?.toDouble() ?: 0.0) > 0.0 }
      val emptyProbes = probesForCurrentWell.toMutableList()
      emptyProbes.removeAll(probesWithMSD)

      val resultIntervals: MutableList<Map<String, String>> = ArrayList()
      for (emptyProbe in emptyProbes) { // перебор пустых проб для текущей скважины
        val fromEmpty = emptyProbe["От"]?.toDouble() ?: 0.0
        val toEmpty = emptyProbe["До"]?.toDouble() ?: 0.0
        // перебор неперекрывающихся интервалов, полученных после проверки текущего пустого интервала
        overlap(fromEmpty, toEmpty, probesWithMSD).forEach {
          val intervals = it.split(";")
          // если в результирующую коллекцию еще не добавлена пустая проба с такими промежутками "От" и "До", добавить эту пробу
          if (!resultIntervals.any { probe -> probe["От"] == intervals[0] && probe["До"] == intervals[1] }) {
            val p = emptyProbe.toMutableMap()
            p["От"] = intervals[0]
            p["До"] = intervals[1]
            resultIntervals.add(p)
          }
        }
      }
      resultIntervals.addAll(probesWithMSD) // добавить к новым пустым интервалам пробы с находками МСА
      intervalWellsFile.writeContent(resultIntervals)
    } catch(e: Exception) {
      throw GeoTaskException(e.message?.let { e.message } ?: "perform error")
    }
  }

  @Throws(SecurityException::class, IOException::class)
  override fun writeData() { } // запись в файл производится в perform()

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

  /**
   * Пустая проба сопоставляется с непустыми пробами на предмет пространственного
   * наложения. Если пустая проба делится на две, из-за того что ее пересекает
   * непустая проба, тогда каждая новая проба рекурсивно проверяется на наличие
   * наложений и затем возвращает новый список неперекрывающихся проб обратно.
   * При этом могут возникать дублирования, чтобы этого не происходило - новые интервалы
   * добавляются в хеш-множество.
   */
  private fun overlap(pFromEmpty: Double, pToEmpty: Double,
                probesWithMSD: List<Map<String, String>>): HashSet<String> {
    var fromEmpty = pFromEmpty // изначальные границы пустого интервала, которые могут быть впоследствии укорочены
    var toEmpty = pToEmpty

    val resultSet = HashSet<String>()
    for (probeWithMSD in probesWithMSD) { // сопоставить пустую пробу с непустыми пробами
      val fromMSD = probeWithMSD["От"]?.toDouble() ?: 0.0
      val toMSD = probeWithMSD["До"]?.toDouble() ?: 0.0
      when {
        (toMSD <= fromEmpty || fromMSD >= toEmpty) -> { } // интервал с МСА за пределами пустой пробы - пустая проба остается без изменений)
        (fromMSD <= fromEmpty && toMSD >= toEmpty) -> return resultSet // если проба с МСА полностью перекрывает пустой интевал - пустой интервал удаляется
        (fromMSD > fromEmpty && toMSD < toEmpty) -> { // интервал с МСА лежит внутри пустого интревала - разделить пустой интервал на два
          val set1 = overlap(fromEmpty, fromMSD, probesWithMSD) // рекурсивно проверить первый новый интервал на оверлап
          val set2 = overlap(toMSD, toEmpty, probesWithMSD) // рекурсивно проверить второй новый интервал на оверлап
          resultSet.addAll(set1) // добавить новые неперекрывающие интервалы по первому пустому интервалу
          resultSet.addAll(set2) // добавить новые неперекрывающие интервалы по второму пустому интервалу
        }
        (toMSD > fromEmpty && fromMSD <= fromEmpty && toMSD < toEmpty) -> fromEmpty = toMSD // интервал с МСА перекрывает пустую пробу сверху - укоротить пустую пробу сверху
        (fromMSD < toEmpty && toMSD >= toEmpty && fromMSD > fromEmpty) -> toEmpty = fromMSD // интервал с МСА перекрывает пустую пробу снизу - укоротить пустую пробу снизу
        else -> logger.info("${this.javaClass.simpleName}: unknown case for overlaps intervals. ID of probe: ${probeWithMSD["ID"]}")
      }
    }
    if (resultSet.size == 0) resultSet.add("$fromEmpty;$toEmpty")
    return resultSet
  }
}
