package model.file

import model.constants.CommonConstants.nameOfAttributeDepth
import model.constants.IsihogyClientConstants.nameOfAttributeX
import model.constants.IsihogyClientConstants.nameOfAttributeY
import java.io.BufferedWriter
import java.io.IOException
import java.io.PrintWriter
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*

/**
 * Тестовый файл Micromine, который затем может быть преобразован в
 * файл *.DAT с помощью функции импорта в Micromine
 * @throws IOException
 */
class MicromineTextFile @Throws(IOException::class)

constructor(val file: Path) {
  private lateinit var title: List<String>

  init {
    Files.deleteIfExists(file)
    Files.createFile(file)
  }

  /**
   * Записывает заголовок со списком названий атрибутов в файл.
   * [title] - список, который содержит перечень названий атрибутов
   * [file] - существующий файл, в который записывается заголов
   * @throws IOException
   */
  @Throws(IOException::class)
  fun writeTitle(title: List<String>) {
    if (title.isEmpty()) {
      throw IOException("Список атрибутов для записи в файл пуст")
    }

    this.title = title
    val bw: BufferedWriter = Files.newBufferedWriter(file,
              Charset.forName("Windows-1251"),
              StandardOpenOption.WRITE,
              StandardOpenOption.APPEND)
    PrintWriter(bw).use { out -> // try с ресурсами
      val namesOfAttributes = title.iterator()
      while (namesOfAttributes.hasNext()) {
        var name = namesOfAttributes.next()
        when (name) {
          "X", nameOfAttributeX -> name = "east"
          "Y", nameOfAttributeY -> name = "north"
          nameOfAttributeDepth -> name = "depth"
          else -> {
            if (name.contains(" ")) name = name.replace(" ", "_")
            if (name.contains(";")) name = name.replace(";", "_")
          }
        }
        out.print(name)
        if (namesOfAttributes.hasNext()) out.print(";")
      }
      out.println()
    }
    bw.close()
  }

  /**
   * Записывает таблицу в существующий файл, в который уже добавлен
   * заголовок с названиями атрибутов.
   * [rows] - список, который содержит отображения, где каждое отображение
   * представляет собой текущую строку таблицы, а каждый элемент отображения
   * представлен парой {название атрибута = значение атрибута}.
   * [file] - существующий файл с добавленным заголовком.
   * @throws IOException
   */
  @Throws(IOException::class)
  fun writeContent(rows: List<Map<String, String>>) {
    checkData(rows)

    val bw: BufferedWriter = Files.newBufferedWriter(file,
              Charset.forName("Windows-1251"),
              StandardOpenOption.WRITE,
              StandardOpenOption.APPEND)

    PrintWriter(bw).use { out -> // try с ресурсами
      rows.forEach { row ->
        val namesOfAttributes = title.iterator()
        while (namesOfAttributes.hasNext()) {
          val currentName = namesOfAttributes.next()
          var value = row[currentName]
          value = value!!.replace(";", ", ")
          value = value.replace("\n", "_")
          out.print(value)
          if (namesOfAttributes.hasNext()) out.print(";")
        }
        out.println()
      }
    }
    bw.close()
  }

  @Throws(IOException::class)
  private fun checkData(rows: List<Map<String, String>>) {
    if (rows.isEmpty()) throw IOException("Нет данных для записи в файл")
    val title2 = ArrayList(title)
    val keysRow: List<String> = ArrayList(rows[0].keys)
    // отсортировать коллекции, чтобы порядок следования элементов
    // не влиял на сравнение коллекций
    Collections.sort(title2)
    Collections.sort(keysRow)
    if (keysRow != title2) {
      throw IOException("Количество атрибутов в заголовке файла не совпадает" +
              " с количеством записываемых атрибутивных полей")
    }
  }

  /**
   * Записывает таблицу в существующий файл, в который уже добавлен
   * заголовок с названиями атрибутов. При этом, у таблицы могут
   * отсутствовать некоторые элементы из заголовка. В таком случае,
   * для атрибутов, для которых отсутствуют значения, записываются нули.
   * Это может потребоваться, например, если к таблице проб с МСА,
   * добавляется таблица без МСА, - для этих проб в атрибутах находок
   * МСА, а также некоторых других атрибутах, будут записываться
   * нулевые значения.
   * [rows] - список, который содержит отображения, где каждое отображение
   * представляет собой текущую строку таблицы, а каждый элемент отображения
   * представлен парой {название атрибута = значение атрибута}.
   * [file] - существующий файл с добавленным заголовком.
   * @throws IOException
   */
  @Throws(IOException::class)
  fun writeContentWithoutSomeKeys(rows: List<Map<String, String>>) {
    val bw: BufferedWriter = Files.newBufferedWriter(file,
            Charset.forName("Windows-1251"),
            StandardOpenOption.WRITE,
            StandardOpenOption.APPEND)

    PrintWriter(bw).use { out -> // try с ресурсами
      rows.forEach { row ->
        val namesOfAttributes = title.iterator()
        while (namesOfAttributes.hasNext()) {
          val currentName = namesOfAttributes.next()
          var value: String?
          if (row.containsKey(currentName)) {
            value = row[currentName]
            value = value!!.replace(";", ", ")
            value = value.replace("\n", "_")
          } else {
            value = "0"
          }
          out.print(value)
          if (namesOfAttributes.hasNext()) out.print(";")
        }
        out.println()
      }
    }
    bw.close()
  }
}