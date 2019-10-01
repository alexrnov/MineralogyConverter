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
import java.util.stream.Collectors

/**
 * Задача "Границы опробования в точки". Для каждой скважины
 * находится кровля верхней пробы и подошва нижней пробы.
 * По этим данным в ArcMap строятся гриды, на основе которых
 * в Micromine создаются ограничивающие поверхности для
 * пустой блочной модели. Пустая блочная модель, в свою очередь,
 * используется для вычисления блочной модели с
 * минералогическими ореолами.
 */
class BoundsOfSamplingToPoints

  @Throws(IllegalArgumentException::class)
  constructor(parameters: Map<String, Any>): GeoTaskOneFile(parameters) {
    // количество атрибутов во входном файле
    private val numberAttributes = 268

    private val inputFile: String by parameters
    private val outputFile: String by parameters
    private lateinit var inputFilePath: Path
    private lateinit var outputFilePath: Path

    // коллекция с пробами, которые были считаны из файла.
    // Для проб остаются только необходимые атрибуты
    private val simpleProbes = ArrayList<Map<String, String>>()

    // коллекция содержит точки с данными по кровле верхней пробы
    // и подошве нижней пробы. Эти данные записываются в
    // выходной текстовый файл
    private val dotWells: MutableList<MutableMap<String, String>> = ArrayList()

    // названия необходимых атрибутов во входном/выходном файле
    private var keys: List<String> = ArrayList()

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
          noEnd = line?.let {probes.add(line)} ?: false
          //или: if (line != null) probes.add(line)
          //else noEnd = false
        }
      }

      if (probes.size < 2)
        throw IOException("Неверный формат входного файла")
      keys = probes[0].split(";")
      if (keys.size != numberAttributes)
        throw IOException("Неверный формат входного файла")
      for (i in 1 until probes.size) {
        val currentProbe = probes[i].split(";")
        if (keys.size != numberAttributes)
          throw IOException("Неверный формат входного файла")
        val map = HashMap<String, String>()
        map.put(keys[1], currentProbe[1]) // ID
        map.put(keys[7], currentProbe[7]) // east
        map.put(keys[8], currentProbe[8]) // north
        map.put(keys[9], currentProbe[9]) // z
        map.put(keys[11], currentProbe[11]) // from
        map.put(keys[12], currentProbe[12]) // to
        map.put(keys[23], currentProbe[23]) // all MSD
        simpleProbes.add(map)
      }

      return simpleProbes.stream()
              .map { it[keys[1]] }
              .collect(Collectors.toSet()) // вернуть набор уникальных id скважин
    }

    @Throws(GeoTaskException::class)
    override fun perform(any: Any?) {
      try {
        val idWell = any as String
        val layersForCurrentWell = simpleProbes.filter { it[keys[1]] == idWell }
        var min = layersForCurrentWell
                .map { it[keys[11]]?.toDouble() ?: 1000.0 }
                .min()!!
        var max = layersForCurrentWell
                .map { it[keys[12]]?.toDouble() ?: 1100.0 }
                .max()!!
        val z = layersForCurrentWell[0][keys[9]]?.toDouble()!!
        min = Math.round((z - min) * 100.0) / 100.0
        max = Math.round((z - max) * 100.0) / 100.0
        val map = HashMap<String, String>()
        map.put(keys[0], idWell)
        map.put(keys[7], layersForCurrentWell[0][keys[7]]!!) // east
        map.put(keys[8], layersForCurrentWell[0][keys[8]]!!) // north
        map.put("Z", min.toString()) // abs z
        map.put("D", max.toString())
        map.put(keys[23], layersForCurrentWell[0][keys[23]]!!) // all MSD
        dotWells.add(map)
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
  }
