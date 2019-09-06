package model.file

import TestUtils.outputFolderProbesWithMSD
import org.junit.jupiter.api.Assertions.*
import io.mockk.*
import model.constants.ProbesWithMSDConstants.indexAndNameOfColumns
import model.task.thread.ThreadTask
import org.junit.jupiter.api.*
import java.io.IOException
import java.io.File.separator as s
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class MicromineTextFileTest {

  private val mockTask = mockk<ThreadTask>()
  private val wellsFile: MicromineTextFile
  private val pathToFile: Path
  private val requiredKeysIntervalWell = ArrayList(indexAndNameOfColumns.values)
  private val wells: MutableList<MutableMap<String, String>> = ArrayList()
  private var sizeFileWithTitle = 0L

  init {
    every { mockTask.printConsole(any()) } just Runs
    pathToFile = Paths.get(outputFolderProbesWithMSD + s + "MicromineTextFileTest.txt")
    Files.deleteIfExists(pathToFile)
    wellsFile = MicromineTextFile(pathToFile)
    val well: MutableMap<String, String> = HashMap()
    for ((index, i) in requiredKeysIntervalWell.withIndex()) {
      well.put(i, "p$index")
    }
    wells.add(well)
  }

  @Test
  @Order(1)
  fun `write title`() {
    wellsFile.writeTitle(requiredKeysIntervalWell)
    sizeFileWithTitle = pathToFile.toFile().length()
    assertTrue(Files.exists(pathToFile))
  }

  @Test
  @Order(2)
  fun `write content`() {
    wellsFile.writeContent(wells)
    assertNotEquals(sizeFileWithTitle, pathToFile.toFile().length())
  }

  @Test
  @Order(3)
  fun `write incorrect content`() {
    wells[0].remove("Объем")
    var e: Exception
    e = assertThrows(IOException::class.java) { wellsFile.writeContent(wells) }
    assertEquals("Количество атрибутов в заголовке файла не совпадает " +
            "с количеством записываемых атрибутивных полей", e.message)

    wells.clear()
    e = assertThrows(IOException::class.java) { wellsFile.writeContent(wells) }
    assertEquals("Нет данных для записи в файл", e.message)

    requiredKeysIntervalWell.clear()
    e = assertThrows(IOException::class.java) {
      wellsFile.writeTitle(requiredKeysIntervalWell)
    }
    assertEquals("Список атрибутов для записи в файл пуст", e.message)
  }
}