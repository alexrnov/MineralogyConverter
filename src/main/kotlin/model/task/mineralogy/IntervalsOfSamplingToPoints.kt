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

/**
 * Задача "Интервалы опробования в точки".
 */
class IntervalsOfSamplingToPoints

@Throws(IllegalArgumentException::class)
constructor(parameters: Map<String, Any>): GeoTaskOneFile(parameters) {
  // количество атрибутов во входном файле интервалов со всеми пробами
  // (с находками МСА и без таковых). В этом файле на одно атрибутивное
  // поле больше("находки"), но оно расположено в самом конце и не влияет
  // на порядок индексов предшедствующх атрибутов
  private val numberAttributesAllMSD = 268

  // колчество атрибутов во входном файле интервалов только для непустых проб
  private val numberAttributesOnlyMSD = 267

  private val inputFile: String by parameters
  private val outputFile: String by parameters
  private val ageIndex: String by parameters
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
  private var calculationsTask: CalculationsTask = { _ -> }

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
    // или if (keys.size != numberAttributesOnlyMSD && keys.size != numberAttributesAllMSD)
    if (keys.size !in numberAttributesOnlyMSD..numberAttributesAllMSD)
      throw IOException("Неверный формат входного файла")

    when (taskName) {
      "подсветить точки с указанным возрастом и с находками" -> {
        // если входной файл - интервалы по всем точкам наблюдения
        if (keys.size == numberAttributesAllMSD) {
          // при чтении файла, в коллекцию упрощенных проб добавлять атрибуты
          val addAttributes: AddAttributes = { simpleProbeMap, currentProbeList ->
            simpleProbeMap[keys[16]] = currentProbeList[16] // стратиграфия
            simpleProbeMap[keys.last()] = currentProbeList[keys.lastIndex] // находки
          }
          probes.fillSimpleProbes(addAttributes)
          // выделять точки со стратиграфическим индексом, указанным во
          // входных параметрах, при условии, что для них есть находки МСА
          calculationsTask = { layersForCurrentWell ->
            layersForCurrentWell.map {
              // если у текущего пласта стратиграфический возраст не совпадает с искомым
              // стратиграфическим индексом и для этого пласта есть находки, тогда для
              // атрибута "находки" установить значение "0.0"
              val layerAge = it[keys[16]] ?: "" // стратиграфия
              if (!layerAge.contains(ageIndex) && it[keys.last()] == "1.0") it[keys.last()] = "0.0"
            }
          }
        }
      }
      "общая сохранность" -> {
        val addAttributes: AddAttributes = { simpleProbeMap, currentProbeList ->
          simpleProbeMap[keys[24]] = currentProbeList[24] // пиропы
          simpleProbeMap[keys[25]] = currentProbeList[25] // пикроильмениты

          simpleProbeMap[keys[32]] = currentProbeList[32] // пироп/износ_механический/0
          simpleProbeMap[keys[33]] = currentProbeList[33] // пироп/износ_механический/I
          simpleProbeMap[keys[34]] = currentProbeList[34] // пироп/износ_механический/II
          simpleProbeMap[keys[35]] = currentProbeList[35] // пироп/износ_механический/III
          simpleProbeMap[keys[36]] = currentProbeList[36] // пироп/износ_механический/IV
          simpleProbeMap[keys[37]] = currentProbeList[37] // пироп/осколки
          simpleProbeMap[keys[38]] = currentProbeList[38] // пироп/гипергенные
          simpleProbeMap[keys[39]] = currentProbeList[39] // пироп/трещиноватости
          simpleProbeMap[keys[40]] = currentProbeList[40] // пироп/включения

          simpleProbeMap[keys[126]] = currentProbeList[126] // пикроильменит/износ_механический/0
          simpleProbeMap[keys[127]] = currentProbeList[127] // пикроильменит/износ_механический/I
          simpleProbeMap[keys[128]] = currentProbeList[128] // пикроильменит/износ_механический/II
          simpleProbeMap[keys[129]] = currentProbeList[129] // пикроильменит/износ_механический/III
          simpleProbeMap[keys[130]] = currentProbeList[130] // пикроильменит/износ_механический/IV
          simpleProbeMap[keys[131]] = currentProbeList[131] // пикроильменит/осколки
          simpleProbeMap[keys[132]] = currentProbeList[132] // пикроильменит/гипергенные
          simpleProbeMap[keys[133]] = currentProbeList[133] // пикроильменит/вторичные
        }
        probes.fillSimpleProbes(addAttributes)
      }
    }

    var i = 0
    keys.forEach {
      println("$i $it")
      i++
    }

    println("------------------")

    simpleProbes[0].forEach {
      println(it)
    }

    return simpleProbes.stream()
            .map { it[keys[1]] }
            .collect(Collectors.toSet()) // вернуть набор уникальных id скважин
  }

  @Throws(GeoTaskException::class)
  override fun perform(any: Any?) {
    try {
      val idWell = any as String
      val layersForCurrentWell: List<MutableMap<String, String>> = simpleProbes.filter { it[keys[1]] == idWell }
      calculationsTask.invoke(layersForCurrentWell)
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
      addAttributes.invoke(simpleProbeMap, currentProbeList)
      simpleProbes.add(simpleProbeMap)
    }
  }
}
