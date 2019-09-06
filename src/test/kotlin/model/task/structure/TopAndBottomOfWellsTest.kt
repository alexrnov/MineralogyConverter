package model.task.structure

import TestUtils.initToolkit
import TestUtils.inputFolderIsihogyClient
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import model.constants.IsihogyClientConstants.nameOfAttributeBottomWell
import model.constants.IsihogyClientConstants.nameOfAttributeID
import model.constants.IsihogyClientConstants.nameOfAttributeX
import model.constants.IsihogyClientConstants.nameOfAttributeY
import model.constants.IsihogyClientConstants.nameOfAttributeZ
import model.task.thread.ThreadTask
import org.junit.jupiter.api.Test
import java.io.File.separator as s
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.net.URLDecoder

internal class TopAndBottomOfWellsTest {
  private val mockTask = mockk<ThreadTask>()
  private val folder = "input/excel files from isihogy client"
  private val parameters = mutableMapOf(
          "inputFolder" to inputFolderIsihogyClient,
          "outputFile" to "." + s + "src" + s + "test" + s + "resources" + s + "output" +
                  s + "isihogy client" + s + "topAndBottomOfWells.txt",
          "exportAllFields" to true, "useAmendment" to false)
  private val excelFile: File

  init {
    var path = "$folder/Промышленный_4/Промышленный_4_участок2_part1.xls"
    path = URLDecoder.decode(ClassLoader.getSystemResource(path).file, "UTF-8")
    excelFile = File(path)
    initToolkit()
    every { mockTask.printConsole(any()) } just Runs
  }

  @Test
  fun `export all fields = false`() {
    val task = TopAndBottomOfWells(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    val table = task.getObservationsPointsTable

    assertEquals(80, table.size)
    assertEquals("183701", table[table.size - 1][nameOfAttributeID])
    assertEquals("520966.0", table[table.size - 1][nameOfAttributeX])
    assertEquals("7221303.0", table[table.size - 1][nameOfAttributeY])
    assertEquals("239.97", table[table.size - 1][nameOfAttributeZ])
    assertEquals("Выполнено итоговое документирование",
            table[table.size - 1]["Код Состояния документирования"])
    assertEquals("2000", table[table.size - 2][nameOfAttributeZ])
    assertEquals("140.0", table[0][nameOfAttributeBottomWell])
    assertEquals("72.97", table[table.size - 1][nameOfAttributeBottomWell])
  }

  @Test
  fun `export all fields = true`() {
    parameters["exportAllFields"] = false
    val task = TopAndBottomOfWells(parameters)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    assertEquals(80, task.getObservationsPointsTable.size)
    task.getObservationsPointsTable.forEach { assertEquals(6, it.size) }
  }
}