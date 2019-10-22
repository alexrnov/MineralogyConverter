package model.task.mineralogy

import model.constants.CommonConstants
import model.exception.GeoTaskException
import model.file.MicromineTextFile
import model.task.GeoTaskOneFile
import model.utils.CollectionUtils
import model.utils.addPointsToIntervals
import model.utils.calculateAbsZForAdditionalPoints
import java.io.BufferedReader
import java.io.File
import java.io.File.separator
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

  private var numberWell = 0
  private var intervalWellsFile: MicromineTextFile
  init {
    checkInputParameters()


    outputFilePath = Paths.get(outputFile)
    intervalWellsFile = MicromineTextFile(outputFilePath)

  }

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
            intervalWellsFile.writeTitle(keys)
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
      println(numberWell++)
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
      println("probesWithMSD: ")
      probesWithMSD.forEach {
        println("${it["ID"]} ${it["От"]} ${it["До"]} ${it["Тип_пробы"]} ${it["Все_МСА"]}")
      }
      println("-")
      println("emptyProbes: ")
      emptyProbes.forEach {
        println("${it["ID"]} ${it["От"]} ${it["До"]} ${it["Тип_пробы"]} ${it["Все_МСА"]}")
      }

      val resultSet = HashSet<String>()
      for (emptyProbe in emptyProbes) {
        val fromEmpty = emptyProbe["От"]?.toDouble() ?: 0.0
        val toEmpty = emptyProbe["До"]?.toDouble() ?: 0.0
        resultSet.addAll(f(fromEmpty, toEmpty, probesWithMSD))
      }


      println("-")
      println("result empty intervals:")
      resultSet.forEach { println(it) }

      val templateList: MutableList<Map<String, String>> = ArrayList()
      if (probesForCurrentWell.isNotEmpty()) {
        resultSet.forEach {
          val a = it.split("-")
          if (a.size > 1) {
            val template = probesForCurrentWell[0].toMutableMap()
            template["От"] = a[0]
            template["До"] = a[1]
            template["Все_МСА"] = "0.0"
            template["находки"] = "0"
            templateList.add(template)
          }
        }
      }
      templateList.addAll(probesWithMSD)

      println("-")
      println("templateList: ")
      templateList.forEach {
        println("${it["ID"]} ${it["От"]} ${it["До"]} ${it["Тип_пробы"]} ${it["Все_МСА"]}")
      }

      intervalWellsFile.writeContent(templateList)

      //println("------------------------")
    } catch(e: Exception) {
      throw GeoTaskException(e.message?.let { e.message } ?: "perform error")
    }
  }

  @Throws(SecurityException::class, IOException::class)
  override fun writeData() {
    /*
    val title = dotWells[0].keys.toList()
    val dotWellsFile = MicromineTextFile(outputFilePath)
    dotWellsFile.writeTitle(title)
    dotWellsFile.writeContent(dotWells)
    */
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

  private fun f(pFromEmpty: Double, pToEmpty: Double,
                probesWithMSD: List<Map<String, String>>): HashSet<String> {
    var fromEmpty = pFromEmpty
    var toEmpty = pToEmpty

    //println("Empty[$fromEmpty-$toEmpty]")
    val list = HashSet<String>()

    for (probeWithMSD in probesWithMSD) {
      val fromMSD = probeWithMSD["От"]?.toDouble() ?: 0.0
      val toMSD = probeWithMSD["До"]?.toDouble() ?: 0.0
      when {
        (toMSD <= fromEmpty || fromMSD >= toEmpty) -> {
          //println("1. MSD[$fromMSD-$toMSD], Интервал с МСА за пределами пустой пробы")
        }
        (fromMSD <= fromEmpty && toMSD >= toEmpty) -> {
          //println("2. MSD[$fromMSD-$toMSD], Интервал с МСА полностью перекрывает пустой интервал")
          return list // если проба с МСА полностью перекрывает пустой интевал, вернуть пустой список
        }
        (fromMSD > fromEmpty && toMSD < toEmpty) -> {
          //println("3. MSD[$fromMSD-$toMSD], Интервал с МСА лежит внутри пустого интревала")
          val list2 = f(fromEmpty, fromMSD, probesWithMSD)
          val list3 = f(toMSD, toEmpty, probesWithMSD)
          list.addAll(list2)
          list.addAll(list3)
        }
        (toMSD > fromEmpty && fromMSD <= fromEmpty && toMSD < toEmpty) -> {
          //println("4. MSD[$fromMSD-$toMSD], Интервал с МСА перекрывает пустую пробу сверху")
          fromEmpty = toMSD
        }
        (fromMSD < toEmpty && toMSD >= toEmpty && fromMSD > fromEmpty) -> {
          //println("5. MSD[$fromMSD-$toMSD], Интервал с МСА перекрывает пустую пробу снизу")
          toEmpty = fromMSD
        }
        else -> println("Другой случай")
      }
    }
    if (list.size == 0) list.add("$fromEmpty-$toEmpty")
    return list
  }

}
