package model.utils

import model.constants.IsihogyClientConstants.lithostratigraphicSheetName
import model.constants.IsihogyClientConstants.nameOfAttributeFrom
import model.constants.IsihogyClientConstants.nameOfAttributeID
import model.constants.IsihogyClientConstants.nameOfAttributeLCodeAge
import model.constants.IsihogyClientConstants.nameOfAttributeTo
import model.constants.IsihogyClientConstants.stratigraphicCodesSheetName
import model.utils.ExcelUtils.getCodesOfIsihogyClient
import model.utils.ExcelUtils.getSheetOfIsihogyClient
import model.utils.ExcelUtils.getTableOfIsihogyClient
import model.utils.IsihogyClientUtils.decodingField
import model.utils.IsihogyClientUtils.deleteDecimalPart
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.net.URLDecoder

internal class UnionAgeLayersTest {
  private var tableForUnionLayers: List<MutableMap<String, String>> = ArrayList()
  private var tableWithUnionTable: List<MutableMap<String, String>> = ArrayList()
  private var indexAge = "J1sn"

  init {
    val folder = "input/excel files from isihogy client"
    var path = "$folder/Промышленный_4/Промышленный_4_участок2_part1.xls"
    path = URLDecoder.decode(ClassLoader.getSystemResource(path).file, "UTF-8")
    val file = File(path)

    var sheet = getSheetOfIsihogyClient(file, lithostratigraphicSheetName)
    tableForUnionLayers = getTableOfIsihogyClient(sheet)
    sheet = getSheetOfIsihogyClient(file, stratigraphicCodesSheetName)
    val stratigraphicCodes = getCodesOfIsihogyClient(sheet)
    decodingField(tableForUnionLayers, stratigraphicCodes)
    deleteDecimalPart(nameOfAttributeID, tableForUnionLayers)
  }

  @Test
  fun `union layers without simplify packets`() {
    val unionAgeLayers = UnionAgeLayers(tableForUnionLayers)
    tableWithUnionTable = unionAgeLayers.getTableWithUnionLayers()
    assertEquals(tableForUnionLayers.size, 1404)
    assertEquals(tableWithUnionTable.size, 720)
    assertEquals(tableWithUnionTable[0].size, 4)

    assertEquals(tableWithUnionTable[6][nameOfAttributeID], "178051")
    assertEquals(tableWithUnionTable[6][nameOfAttributeFrom], "72.1")
    assertEquals(tableWithUnionTable[6][nameOfAttributeTo], "88.3")
    assertEquals(tableWithUnionTable[6][nameOfAttributeLCodeAge], "J1uk")

    assertEquals(tableWithUnionTable[29][nameOfAttributeID], "168053")
    assertEquals(tableWithUnionTable[29][nameOfAttributeFrom], "135.0")
    assertEquals(tableWithUnionTable[29][nameOfAttributeTo], "146.0")
    assertEquals(tableWithUnionTable[29][nameOfAttributeLCodeAge], "O1ol")
  }

  @Test
  fun `union layers with simplify packets`() {
    val unionAgeLayers = UnionAgeLayers(tableForUnionLayers)
    unionAgeLayers.simplifyPacketsForIndex(indexAge)
    tableWithUnionTable = unionAgeLayers.getTableWithUnionLayers()
    assertEquals(tableForUnionLayers.size, 1404)
    assertEquals(tableWithUnionTable.size, 503)
    assertEquals(tableWithUnionTable[1][nameOfAttributeID], "178051")
    assertEquals(tableWithUnionTable[1][nameOfAttributeFrom], "5.2")
    assertEquals(tableWithUnionTable[1][nameOfAttributeTo], "65.3")
    assertEquals(tableWithUnionTable[1][nameOfAttributeLCodeAge], "J1sn")
  }
}