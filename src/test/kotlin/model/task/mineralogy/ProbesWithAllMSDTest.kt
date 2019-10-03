package model.task.mineralogy

import TestUtils.initToolkit
import TestUtils.inputFolderProbesWithMSD
import TestUtils.inputFolderProbesWithoutMSD
import TestUtils.outputFolderAllProbes
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import model.constants.CommonConstants
import model.constants.IsihogyClientConstants
import model.exception.GeoTaskException
import model.file.MicromineTextFile
import model.task.thread.ManyFilesThreadTask
import model.task.thread.ThreadTask
import model.utils.ExcelUtils.listOfExcelFiles
import org.junit.jupiter.api.Test
import java.io.File.separator as s
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import java.io.BufferedReader
import java.io.File
import java.net.URLDecoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

internal class ProbesWithAllMSDTest {
  private val nameOfExcelFileWithoutMSD =
          "input/excel files probes without MSD/Нижне-накынский-2.xls"
  private val excelFileWithoutMSD: File

  private val nameOfExcelFileWithMSD =
          "input/excel files probes with MSD/Нижне-накынский-2.xls"
  private val excelFileWithMSD: File

  private val mockTask = mockk<ThreadTask>()
  private val volume = "10 л"
  private val parameters = mapOf("inputFolderWithoutMSD" to inputFolderProbesWithoutMSD,
          "inputFolderWithMSD" to inputFolderProbesWithMSD,
          "outputFolder" to outputFolderAllProbes,
          "probeVolume" to volume, "useReferenceVolume" to true,
          "useAmendment" to true, "createDotFile" to true,
          "typeOfSelectionAge" to "Все пробы")

  init {
    initToolkit()
    every { mockTask.printConsole(any()) } just Runs
    val resourceWithoutMSD = ClassLoader.getSystemResource(nameOfExcelFileWithoutMSD)
    val configPathWithoutMSD = URLDecoder.decode(resourceWithoutMSD.file, "UTF-8")
    excelFileWithoutMSD = File(configPathWithoutMSD)

    val resourceWithMSD = ClassLoader.getSystemResource(nameOfExcelFileWithMSD)
    val configPathWithMSD = URLDecoder.decode(resourceWithMSD.file, "UTF-8")
    excelFileWithMSD = File(configPathWithMSD)
  }

  @DisplayName("excel file without MSD")
  @Test
  fun perform1() {
    val task = ProbesWithAllMSD(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFileWithoutMSD)
    assertEquals(322, task.getTopWells.size)
    assertEquals(5191, task.getIntervalWells.size)
    assertEquals(5191, task.getDotWells.size)

    val expectedTopWell = mapOf("Объект" to "Нижне-Накынский-2",
            "Линия" to "518", "Зона" to "M20", "X" to "500647.0",
            "Y" to "7210527.0", "Участок" to "Ханнинский", "Точка" to "427",
            "Z" to "237.8", "ID" to "177668", "Тип ТН" to "Скв. поиск.",
            "Глубина ТН" to "84.0", "IDW" to "300")
    assertEquals(expectedTopWell, task.getTopWells[300])
    val actualIntervalWell = task.getIntervalWells[2934]
    assertEquals(56, actualIntervalWell.size)
    assertEquals("202", actualIntervalWell["IDW"])
    assertEquals("63.9", actualIntervalWell["От"])
    assertEquals(true, actualIntervalWell.contains("X"))

    assertEquals("3 л", actualIntervalWell["Объем"])
    assertEquals("5", actualIntervalWell["минеральная ассоциация шлиха/сидерит"])
    assertEquals("0", actualIntervalWell["количество минералов/сфалерит"])
    assertEquals("73", actualIntervalWell["минеральная ассоциация шлиха/пирит"])
    assertEquals("5", actualIntervalWell["минеральная ассоциация шлиха/сидерит"])
    assertEquals("0", actualIntervalWell["минеральная ассоциация шлиха/гроссуляр"])
    assertEquals("2", actualIntervalWell["минеральная ассоциация шлиха/дистен"])
    assertEquals("0", actualIntervalWell["минеральная ассоциация шлиха/барит"])

    assertEquals("166.67", actualIntervalWell["количество минералов/альмандин"])
    assertEquals("166.67", actualIntervalWell["количество минералов/ставролит"])
    assertEquals("0", actualIntervalWell["количество минералов/сфалерит"])
  }

  @DisplayName("excel file with MSD")
  @Test
  fun perform2() {
    val task = ProbesWithAllMSD(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFileWithMSD)
    assertEquals(150, task.getTopWells.size)
    assertEquals(617, task.getIntervalWells.size)
    assertEquals(617, task.getDotWells.size)

    val expectedTopWell = mapOf("Объект" to "Нижне-Накынский-2",
            "Линия" to "464", "Глубина ТН" to "77.5", "Зона" to "M20",
            "X" to "497753.0", "Y" to "7205163.0", "Участок" to "Озерный",
            "Z" to "225.9", "Точка" to "399", "ID" to "177591",
            "Тип ТН" to "Скв. поиск.", "IDW" to "148")
    assertEquals(expectedTopWell, task.getTopWells[148])
    var actualIntervalWell = task.getIntervalWells[616]
    val actualDotWell = task.getDotWells[616]
    assertEquals(268, actualIntervalWell.size)
    assertEquals("149", actualIntervalWell["IDW"])
    assertEquals("84.5", actualIntervalWell["От"])
    assertEquals(true, actualIntervalWell.contains("X"))
    assertEquals("497924.0", actualIntervalWell["X"])
    assertEquals("7205293.0", actualIntervalWell["Y"])
    assertEquals("497924.0", actualDotWell["X"])
    // количество кристаллов с учетом пересчета на эталонный объем пробы
    assertEquals("21.05", actualIntervalWell["Все МСА"])
    assertEquals("0", actualIntervalWell["Хромшпинелиды"])
    assertEquals("14.04", actualIntervalWell["пироп/по классам крупности/-0.5"])
    assertEquals("8.77", actualIntervalWell["пироп/класс износа I/-0.5"])
    assertEquals("4.39", actualIntervalWell["пироп/гипергенные/-0.5"])
    // минеральная ассоциация не пересчитывается - поскольку она выражена в процентах
    // ильменит - первый атрибут в разделе "минералогическая ассоциация шлиха"
    assertEquals("1", actualIntervalWell["минеральная ассоциация шлиха/ильменит"])
    assertEquals("0", actualIntervalWell["количество минералов/сфалерит"])

    actualIntervalWell = task.getIntervalWells[3]
    // раздел "количество минералов" пересчитывается
    // альмандин - первый атрибут в разделе "количество минералов"
    assertEquals("83.33", actualIntervalWell["количество минералов/альмандин"])
  }

  @DisplayName("both types of files")
  @Test
  fun perform3() {
    val task = ProbesWithAllMSD(parameters)
    task.setThreadingTask(mockTask)

    task.perform(excelFileWithoutMSD)
    assertEquals(322, task.getTopWells.size)
    assertEquals(5191, task.getIntervalWells.size)
    assertEquals(5191, task.getDotWells.size)

    task.perform(excelFileWithMSD)
    assertEquals(150, task.getTopWells.size)
    assertEquals(617, task.getIntervalWells.size)
    assertEquals(617, task.getDotWells.size)
  }

  @DisplayName("perform all excel files")
  @Test
  fun perform4() {
    val pathToFile = Paths.get(outputFolderAllProbes + s + "intervalWells.txt")
    Files.deleteIfExists(pathToFile)
    val files = listOfExcelFiles(inputFolderProbesWithoutMSD)
    files.addAll(listOfExcelFiles(inputFolderProbesWithMSD))
    val task = ProbesWithAllMSD(parameters)
    task.setThreadingTask(mockTask)
    files.forEach {
      try {
        task.perform(it)
      } catch (e: GeoTaskException) { println("GeoTaskException") }
    }
    assertEquals(46946084, pathToFile.toFile().length())
  }

  @Test
  fun changeValue() {
    val resourceWithMSD = ClassLoader.getSystemResource("input/excel files probes with MSD/Промышленный-5.xls")
    val configPathWithMSD = URLDecoder.decode(resourceWithMSD.file, "UTF-8")
    val excelFileWithMSD = File(configPathWithMSD)
    val task = ProbesWithAllMSD(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFileWithMSD)
    val table = task.getDotWells
  }
  /**
   * Сравнение данных одной скважины из файла с МСА и файла без МСА:
   * проба с интервалом От:101; До:110, одинаковым типом, и одинаковой
   * минеральной ассоциацией присутствует в обох файлах - что странно.
   */
  @Disabled
  @DisplayName("compare well with MSD and well without MSD, two wells" +
          " with same id")
  @Test
  fun perform5() {
    val task = ProbesWithAllMSD(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFileWithMSD)
    println("Проба с находками МСА: ")
    task.getIntervalWells.forEach {
      if (it["Линия"] == "512" && it["Точка"] == "423") {
        println("${it["Линия"]}/${it["Точка"]}; От:${it["От"]}; До:${it["До"]}; " +
                "Все МСА:${it["Все МСА"]}; Тип: ${it["Тип пробы"]}; " +
                "Пирит: ${it["минеральная ассоциация шлиха/пирит"]}; " +
                "Сидерит: ${it["минеральная ассоциация шлиха/сидерит"]}; " +
                "Ильменит: ${it["минеральная ассоциация шлиха/ильменит"]};")
      }
    }
    task.getTopWells.filter {it["ID"] == "177591"}.forEach {
      println(it)
    }
    println("Проба без МСА: ")
    task.perform(excelFileWithoutMSD)
    task.getIntervalWells.forEach {
      if (it["Линия"] == "512" && it["Точка"] == "423") {
        println("${it["Линия"]}/${it["Точка"]}; От:${it["От"]}; До:${it["До"]}; " +
                "Все МСА:${it["Все МСА"]}; Тип: ${it["Тип пробы"]}; " +
                "Пирит: ${it["минеральная ассоциация шлиха/пирит"]}; " +
                "Сидерит: ${it["минеральная ассоциация шлиха/сидерит"]}; " +
                "Ильменит: ${it["минеральная ассоциация шлиха/ильменит"]};")
      }
    }
    task.getTopWells.filter {it["ID"] == "177591"}.forEach {
      println(it)
    }
  }

  @DisplayName("check sequence intervals")
  @Test
  fun perform6() {
    val task = ProbesWithAllMSD(parameters)
    task.setThreadingTask(mockTask)
    val resourceWithMSD = ClassLoader.getSystemResource(
            "input/excel files probes with MSD/Южно-Накынский.xls")
    val configPathWithMSD = URLDecoder.decode(resourceWithMSD.file, "UTF-8")
    val excelFileWithMSD = File(configPathWithMSD)
    task.perform(excelFileWithMSD)
    val table = task.getTopWells
    assertEquals("0.5", table[0]["Глубина ТН"])
    assertEquals("0.5", table[1]["Глубина ТН"])
    assertEquals("0.6", table[6]["Глубина ТН"])
    assertEquals("0.5", table[7]["Глубина ТН"])
  }

  @Test
  fun perform7() {
    /*
    val task = ProbesWithAllMSD(parameters)
    task.setThreadingTask(mockTask)
    val resourceWithMSD = ClassLoader.getSystemResource(
            "input/excel files probes with MSD/Нижне-Накынский-4.xls")
    val configPathWithMSD = URLDecoder.decode(resourceWithMSD.file, "UTF-8")
    val excelFileWithMSD = File(configPathWithMSD)
    task.perform(excelFileWithMSD)
    task.getIntervalWells.filter {it["ID"] == "977735"}.forEach {
      println("От = " + it["От"] + ", До = " + it["До"] + ", Все МСА = " + it["Все МСА"])
    }
    */


    val task = ProbesWithAllMSD(parameters)
    task.setThreadingTask(mockTask)
    val inputFiles2: MutableList<File> = ArrayList()

    /*
    val nameOfExcelFileWithoutMSD =
            "input/excel files probes without MSD/Нижне-накынский-4.xls"
    val nameOfExcelFileWithMSD =
            "input/excel files probes with MSD/Нижне-накынский-4.xls"
    */
    val nameOfExcelFileWithoutMSD =
            "input/excel files All MSD/Нижне-накынский-4_withoutMSD.xls"
    val nameOfExcelFileWithMSD =
            "input/excel files All MSD/Нижне-накынский-4_withMSD.xls"


    val resourceWithoutMSD = ClassLoader.getSystemResource(nameOfExcelFileWithoutMSD)
    val configPathWithoutMSD = URLDecoder.decode(resourceWithoutMSD.file, "UTF-8")
    val excelFileWithoutMSD = File(configPathWithoutMSD)

    val resourceWithMSD = ClassLoader.getSystemResource(nameOfExcelFileWithMSD)
    val configPathWithMSD = URLDecoder.decode(resourceWithMSD.file, "UTF-8")
    val excelFileWithMSD = File(configPathWithMSD)

    inputFiles2.add(excelFileWithoutMSD)
    inputFiles2.add(excelFileWithMSD)

    for (excelFile in inputFiles2) {
      println(excelFile.name)
      task.perform(excelFile)
      println(task.getIntervalWells.size)
    }

  }
}

