package model.task.mineralogy

import model.exception.GeoTaskException
import model.file.MicromineTextFile
import model.task.GeoTaskOneFile
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

typealias AddAttribute = (MutableMap<String, String>, List<String>) -> Unit

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
  private lateinit var inputFilePath: Path
  private lateinit var outputFilePath: Path

  // коллекция с пробами, которые были считаны из файла.
  // Для проб остаются только необходимые атрибуты
  private val simpleProbes = ArrayList<MutableMap<String, String>>()

  // коллекция содержит точки с данными по кровле верхней пробы
  // и подошве нижней пробы. Эти данные записываются в
  // выходной текстовый файл
  private val dotWells: MutableList<MutableMap<String, String>> = ArrayList()

  // названия необходимых атрибутов во входном/выходном файле
  private var keys: List<String> = ArrayList()

  private var age = true
  private var strat = "J1dh"

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
        //или: if (line != null) probes.add(line)
        //else noEnd = false
      }
    }
    if (probes.size < 2) throw IOException("Неверный формат входного файла")
    keys = probes[0].split(";")
    // или if (keys.size != numberAttributesOnlyMSD && keys.size != numberAttributesAllMSD)
    if (keys.size !in numberAttributesOnlyMSD..numberAttributesAllMSD)
      throw IOException("Неверный формат входного файла")

    val addAgeAttribute: AddAttribute =
      if (age) { map, currentProbe -> map[keys[16]] = currentProbe[16]}
      else { _, _ -> } // не добавлять атрибут


    val addFindAttribute: AddAttribute = if (keys.size == numberAttributesAllMSD)
    // при чтении файла, в коллекцию с упрощенными пробами добавлять
    // атрибут "находки"
      { map, currentProbe -> map[keys.last()] = currentProbe[keys.lastIndex] }
    else { _, _ -> } // не добавлять атрибут

    probes.fillSimpleProbes(addAgeAttribute, addFindAttribute)

    return simpleProbes.stream()
            .map { it[keys[1]] }
            .collect(Collectors.toSet()) // вернуть набор уникальных id скважин
  }

  @Throws(GeoTaskException::class)
  override fun perform(any: Any?) {
    try {
      val idWell = any as String
      val layersForCurrentWell = simpleProbes.filter { it[keys[1]] == idWell }
      if (age && keys.contains("находки")) {
        layersForCurrentWell.forEach {
          it[keys.last()] = if (it[keys[16]] == strat && it[keys.last()] == "1.0") "1.0" else "0.0"
        }
      }
      val list = addPointsToIntervals(layersForCurrentWell)
      calculateAbsZForAdditionalPoints(list)
      println("-------------")
      dotWells.addAll(list)
    } catch(e: Exception) {
      throw GeoTaskException(e.message?.let{e.message} ?: "perform error")
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
    task.printConsole("Входной файл: ${inputFilePath.toAbsolutePath()}")
    task.printConsole("Выходной файл: ${outputFilePath.toAbsolutePath()}")
    task.printConsole("")
  }

  override fun printReport() {
    task.printConsole("")
    task.printConsole("В выходной файл записано точек: ${dotWells.size}")
  }

  /**
   * Функци расширения для списка проб, которая читает эти пробы, и
   * заполняет на основе прочитанных данных коллекцию с упрощенными
   * пробами (т.е. имеющими меньшее количество атрибутов)
   */
  @Throws(IOException::class)
  private inline fun List<String>.fillSimpleProbes(addAge: AddAttribute,
                                                   addFind: AddAttribute) {
    for (i in 1 until this.size) {
      val currentProbe = this[i].split(";")
      if (currentProbe.size != keys.size)
        throw IOException("Неверный формат входного файла")
      val map = HashMap<String, String>()
      map[keys[1]] = currentProbe[1] // ID
      map[keys[7]] = currentProbe[7] // east
      map[keys[8]] = currentProbe[8] // north
      map[keys[9]] = currentProbe[9] // z
      map[keys[11]] = currentProbe[11] // from
      map[keys[12]] = currentProbe[12] // to
      map[keys[23]] = currentProbe[23] // all MSD
      addAge.invoke(map, currentProbe) // стратиграфия
      addFind.invoke(map, currentProbe) // находки (0 или 1)
      simpleProbes.add(map)
    }
  }
}
