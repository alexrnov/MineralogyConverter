package model.utils

import java.io.File

/**
 * Вычислить абсолютную отметку центра интервала (пробы или
 * стратиграфии). По умолчанию, значение абсолютной отметки
 * центра записывается в атрибут abs устья скважины Z (т.е. атрибут
 * переписывается). Если нужно сохранить abs устья скважины,
 * необходимо явно указать название атрибута abs центра интервала
 * (второй входной параметр)
 * [intervalWells] - коллекция с пробами или стратиграфическими интервалами
 * [nameOfZAttribute] - название атрибута, в который записывается
 * значение абсолютной отметки центра интервала
 */
fun averageZByInterval(intervalWells: List<MutableMap<String, String>>,
                       nameOfZAttribute:String = "Z") {
  var from: Double
  var to: Double
  var z: Double
  for (probe in intervalWells) {
    from = java.lang.Double.valueOf(probe["От"])
    to = java.lang.Double.valueOf(probe["До"])
    z = java.lang.Double.valueOf(probe["Z"])
    from = z - from
    to = z - to
    var averageZ = (from + to) / 2
    averageZ = Math.round(averageZ * 100.00) / 100.0
    probe[nameOfZAttribute] = averageZ.toString()
  }
}

fun addPointsToIntervals(intervals: List<MutableMap<String, String>>):
    List<MutableMap<String, String>> {
  var start: Double
  var end: Double
  var range: Double
  val backCollection = ArrayList<MutableMap<String, String>>()
  intervals.forEach { interval ->
    start = interval["От"]?.toDouble() ?: 1000.0
    end = interval["До"]?.toDouble() ?: 1100.0
    range = Math.round((end - start) * 100.0) / 100.0
    val step = Math.round((range / (Math.ceil(range))) * 100.0) / 100.0

    val list = ArrayList<Double>()
    var i = start
    while (i <= step * Math.ceil(range) + start) {
      list.add(i)
      i = Math.round((i + step) * 100.0) / 100.0
    }
    list[list.lastIndex] = end
    //println("start = $start, end = $end")
    list.forEach {
      //print("$it ")
      val col = interval.toMutableMap()
      col["generateZ"] = it.toString()
      backCollection.add(col)
    }
    //println()

    //println("--------------------------------------")

  }
  return backCollection
}

/*
* Обработка пустых значений z в Промышленном-5 и Нижне-Накынском-4
* добавлено в код, потому что редактирование входного файла
* приводит к изменению количества ячеек, необходимо разобраться
 */
fun checkIndustrial5(file: File, table: MutableList<MutableMap<String, String>>) {
  if (file.name == "Промышленный-5.xls" || file.name == "Нижне-Накынский-4.xls") {
    table.forEach {
      when {
        it["ID"] == "842911" -> it["Z"] = "245,06"
        it["ID"] == "1756514" -> it["Z"] = "247,4"
        it["ID"] == "1756464" -> it["Z"] = "248,2"
        it["ID"] == "1756564" -> it["Z"] = "250,5"
        it["ID"] == "1756414" -> it["Z"] = "251"
        it["ID"] == "1756864" -> it["Z"] = "249,7"
        it["ID"] == "847800" -> it["Z"] = "245,06"
        it["ID"] == "847801" -> it["Z"] = "245,06"
        it["ID"] == "1756664" -> it["Z"] = "253,77"
        it["ID"] == "1756764" -> it["Z"] = "252,29"
        it["ID"] == "2050002" -> it["Z"] = "256,7"

        // необходимо добавить скважины по Нижне-Накынскому-4
      }
    }
  }
}