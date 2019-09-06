package model.utils

import application.Mineralogy.logger
import model.constants.CommonConstants
import model.constants.IsihogyClientConstants.nameOfCodeAttribute
import model.constants.ProbesWithMSDConstants
import model.exception.ExcelException
import model.constants.ProbesWithMSDConstants.nameOfList
import model.constants.ProbesWithoutMSDConstants
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Collectors

object ExcelUtils {

  /** Возвращает список excel-файлов в каталоге (подкаталогах) */
  @Throws(SecurityException::class, IOException::class)
  fun listOfExcelFiles(directory: String): MutableList<File> {
    val path = Paths.get(directory)
    return Files.walk(path)
            .filter { e -> isExcelFile(e) }
            .filter { e -> isFitFile(e) }
            .map { it.toFile() }
            .collect(Collectors.toList())
  }

  private fun isExcelFile(file: Path): Boolean {
    val fileName: String = file.fileName.toString()
    return (fileName.endsWith(".xls") || fileName.endsWith(".XLS"))
            || (fileName.endsWith(".xlsx") || (fileName.endsWith(".XLSX")))
  }

  private fun isFitFile(file: Path): Boolean {
    return Files.isRegularFile(file) && Files.isReadable(file)
  }

  /**
   * Получить объект листа excel из файла с минералогическими пробами,
   * загруженного с web-ресурса ИСИХОГИ.
   * [Sheet] - объект листа excel
   */
  @Throws(ExcelException::class)
  fun getSheetOfWebResource(excelFile: File): Sheet {
    try {
      val workbook = getWorkBook(excelFile)
      if (workbook.numberOfNames == 1
              && workbook.getSheetName(0) == nameOfList) {
        return workbook.getSheetAt(0)
      } else {
        logger.info("Excel sheet ${"Page 1"} not found")
        throw ExcelException("Лист ${"Page 1"} не найден")
      }
    } catch (e: IOException) {
      logger.info("Excel sheet object was not create")
      throw ExcelException("Объект excel-листа не был создан")
    }
  }

  /**
   * Получить объект листа excel из файла, загруженного из клиента ИСИХОГИ
   * [Throws] ExcelException
   */
  @Throws(ExcelException::class)
  fun getSheetOfIsihogyClient(excelFile: File, nameOfSheet: String): Sheet {
    try {
      val workbook = getWorkBook(excelFile)
      val sheet = workbook.getSheet(nameOfSheet)
      if (sheet != null) {
        return sheet
      } else {
        logger.info("Excel sheet: $nameOfSheet not found")
        throw ExcelException("Лист excel-файла: $nameOfSheet не найден")
      }
    } catch (e: IOException) {
      logger.info("Excel sheet object was not create")
      throw ExcelException("Объект excel-листа не был создан")
    }
  }

  @Throws(IOException::class)
  private fun getWorkBook(excelFile: File): Workbook {
    FileInputStream(excelFile).use { input ->
      // try с ресурсами
      val extension = getExtensionOfFile(excelFile.name)
      return if (extension == "xls") HSSFWorkbook(input) else XSSFWorkbook(input)
    }
  }

  private fun getExtensionOfFile(fileName: String): String {
    val extensionOfFile = fileName.substring(
            fileName.lastIndexOf(".") + 1)
    return extensionOfFile.toLowerCase()
  }

  /**
   * На основе excel-листа [sheet] из файла, загруженного из клиента
   * ИСИХОГИ, возвращает список строк листа. Каждая строка представлена
   * отдельным отображением, где ключ - название столбца,
   * значение - содержимое ячейки.
   */
  fun getTableOfIsihogyClient(sheet: Sheet):
          MutableList<MutableMap<String, String>> {
    val table: MutableList<MutableMap<String, String>> = ArrayList()
    val title: MutableMap<Int, String> = HashMap()
    // если таблица содержит строку заголовка, и хотя бы одну строку с данными
    if (sheet.lastRowNum > 1) {
      val firstRow = sheet.getRow(0) // заголовок
      firstRow.forEach { title[it.columnIndex] = it.toString() }
      // считать все строки листа кроме заголовка
      (1..sheet.lastRowNum).forEach { row ->
        val currentRow: Row = sheet.getRow(row)
        val line = getCurrentLineOfSheet(currentRow, title)
        if (line.isNotEmpty()) table.add(line)
      }
    }
    return table
  }

  /**
   * На основе excel-листа [sheet] из файла, загруженного с помощью
   * клиента ИСИХОГИ, возвращает заголовок листа (список атрибутов)
   */
  fun getTitleTableOfIsihogyClient(sheet: Sheet): List<String> {
    val title = ArrayList<String>()
    if (sheet.lastRowNum > 0) {
      sheet.getRow(0).forEach { title.add(it.toString()) }
    }
    return title
  }

  /**
   * На основе excel-листа [sheet] из файла, загруженного с помощью
   * клиента ИСИХОГИ, возвращает ассоциативный массив. В этом массиве,
   * ключ - код, а значение - расшифрованное значение. В массиве также
   * есть элемент у которого ключ - IsihogyClientConstants.nameOfCodeAttribute
   * и значение - название кода, которое должно использоваться при
   * расшифровке.
   */
  fun getCodesOfIsihogyClient(sheet: Sheet): MutableMap<String, String> {
    val codes: MutableMap<String, String> = HashMap()
    val firstRow = sheet.getRow(0)
    // если таблица содержит строку заголовка, и хотя бы одну строку с данными,
    // а также если таблица содержит две колонки: для ключей и расшифровок
    if (sheet.lastRowNum > 1 && firstRow.lastCellNum == 2.toShort()) {
      //добавить название кода, которое должно использоваться при расшифровке
      codes[nameOfCodeAttribute] = firstRow.getCell(0).toString()
      (1..sheet.lastRowNum).forEach { row ->
        val r: Row = sheet.getRow(row)
        codes[r.getCell(0).toString()] = r.getCell(1).toString()
      }
    }
    return codes
  }

  // получить отображение с данными текущей строки (row) листа
  // excel-файла, загруженного с web-ресурса. Заголовок (title)
  // - это ассоциативный массив, где в качестве ключа хранятся
  // индексы ячеек, а в качестве значений - их имена.
  private fun getCurrentLineOfSheet(row: Row, title: Map<Int, String>):
          MutableMap<String, String> {
    var emptyString = true
    val map = HashMap<String, String>()
    title.forEach { indexColumn, nameColumn ->
      val cell: Cell? = row.getCell(indexColumn)
      var valueCell = CommonConstants.noData
      if (cell != null && cell.toString().isNotEmpty()
              && cell.toString() != " ") {
        valueCell = cell.toString()
        emptyString = false
      }
      map[nameColumn] = valueCell
    }
    //если пустая строка (все значения = "Нет данных"), вернуть пустой массив
    if (emptyString) map.clear()
    return map
  }

  /**
   * На основе excel-листа из файла с минералогическими пробами,
   * загруженного с web-ресурса ИСИХОГИ "МСА по всем объектам",
   * возвращает список строк листа. Каждая строка листа храниться
   * в отдельном отображении, где ключ - название столбца,
   * значение - содержимое ячейки
   */
  @Throws(ExcelException::class)
  fun getTableOfProbesWithMSD(sheet: Sheet):
          MutableList<MutableMap<String, String>> {
    if (!verifyTableOfProbesWithMSD(sheet)) {
      logger.info("Excel page has the wrong format")
      throw ExcelException("лист excel имеет неверный формат")
    }
    val table: MutableList<MutableMap<String, String>> = ArrayList()
    // считать все строки листа кроме заголовка
    (ProbesWithMSDConstants.firstRowData..sheet.lastRowNum).forEach { row ->
      val currentRow: Row = sheet.getRow(row)
      table.add(getCurrentLineOfSheet(currentRow,
              ProbesWithMSDConstants.indexAndNameOfColumns))
    }
    return table
  }

  // минимальная проверка формата листа excel-файла, загруженного с web-ресурса
  private fun verifyTableOfProbesWithMSD(sheet: Sheet): Boolean = when {
  // проверка на минимальное количество строк
    sheet.lastRowNum < ProbesWithMSDConstants.firstRowData -> false
  // проверка на минимальное количество столбцов
    sheet.getRow(ProbesWithMSDConstants.firstRowData).lastCellNum !=
            ProbesWithMSDConstants.numberOfColumn -> false
    else -> true
  }

  /**
   * На основе excel-листа из файла с минералогическими пробами,
   * загруженного с web-ресурса ИСИХОГИ "Пробы без МСА",
   * возвращает список строк листа. Каждая строка листа храниться
   * в отдельном отображении, где ключ - название столбца,
   * значение - содержимое ячейки
   */
  @Throws(ExcelException::class)
  fun getTableOfProbesWithoutMSD(sheet: Sheet):
          MutableList<MutableMap<String, String>> {
    if (!verifyTableOfProbesWithoutMSD(sheet)) {
      logger.info("Excel page has the wrong format")
      throw ExcelException("лист excel имеет неверный формат")
    }
    val table: MutableList<MutableMap<String, String>> = ArrayList()
    // считать все строки листа кроме заголовка
    (ProbesWithoutMSDConstants.firstRowData..sheet.lastRowNum).forEach { row ->
      val currentRow: Row = sheet.getRow(row)
      table.add(getCurrentLineOfSheet(currentRow,
              ProbesWithoutMSDConstants.indexAndNameOfColumns))
    }
    return table
  }

  // минимальная проверка формата листа excel-файла,
  // загруженного с web-ресурса "Пробы без МСА"
  private fun verifyTableOfProbesWithoutMSD(sheet: Sheet): Boolean = when {
  // проверка на минимальное количество строк
    sheet.lastRowNum < ProbesWithoutMSDConstants.firstRowData -> false
  // проверка на минимальное количество столбцов
    sheet.getRow(ProbesWithoutMSDConstants.firstRowData).lastCellNum !=
            ProbesWithoutMSDConstants.numberOfColumn -> false
    else -> true
  }

  fun isTableOfProbesWithoutMSD(sheet: Sheet): Boolean {
    val firstRow: Row = sheet.getRow(0)
    val firstCell: String = firstRow.getCell(0).toString()
    return firstCell.contains("По Объекту: ")
  }

  fun getNameOfObject(sheet: Sheet): String {
    val firstRow: Row = sheet.getRow(0)
    val firstCell: String = firstRow.getCell(0).toString()
    return firstCell.split("По Объекту: ")[1]
  }
}