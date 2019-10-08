package model.task.structure

import TestUtils.initToolkit
import TestUtils.inputFolderIsihogyClient
import TestUtils.outputFilePointsRoofOfBaseLayers
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import model.task.thread.ThreadTask
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.net.URLDecoder

internal class RoofOfBaseLayersToPointsTest {
  private val folder = "input/excel files from isihogy client"
  private var path = "$folder/Промышленный_4/Промышленный_4_участок2_part1.xls"
  private var validPath = URLDecoder.decode(ClassLoader.getSystemResource(path).file, "UTF-8")
  private val excelFile = File(validPath)
  private val mockTask = mockk<ThreadTask>()
  private var parameters: MutableMap<String, Any>? = null

  init {
    initToolkit()
    every { mockTask.printConsole(any()) } just Runs
  }

  @BeforeEach
  fun setUp() {
    // если инициализировать коллекцию при объявлении,
    // от теста к тесту она будет модифицироваться, что приведет
    // к ошибке, поэтому она инициализируется здесь
    parameters = mutableMapOf("inputFolder" to inputFolderIsihogyClient,
            "outputFile" to outputFilePointsRoofOfBaseLayers,
            "ageIndexes" to "O1ol; мБD2-3vm", "useAmendment" to true)
  }

  @AfterEach
  fun tearDown() { parameters = null }

  @Test
  fun perform1() {
    val task = RoofOfBaseLayersToPoints(parameters!!)
    task.setThreadingTask(mockTask)
    task.perform(excelFile)
    val table = task.getStratigraphicTable
  }
}