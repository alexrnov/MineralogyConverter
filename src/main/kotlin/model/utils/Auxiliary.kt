package model.utils

import model.constants.CommonConstants.nameOfAttributeGenerateZ
import model.constants.IsihogyClientConstants.nameOfAttributeID
import java.io.File
import java.lang.Double.valueOf
import java.util.*

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
    from = valueOf(probe["От"])
    to = valueOf(probe["До"])
    z = valueOf(probe["Z"])
    from = z - from
    to = z - to
    var averageZ = (from + to) / 2
    averageZ = Math.round(averageZ * 100.00) / 100.0
    probe[nameOfZAttribute] = averageZ.toString()
  }
}

/** Для дополнительных точек вычисляются абсолютные отметки */
fun calculateAbsZForAdditionalPoints(intervalWells: List<MutableMap<String, String>>,
        nameOfZAttribute: String = "Z") {
  var generateZ: Double
  var absZOfWell: Double
  for (probe in intervalWells) {
    generateZ = probe[nameOfAttributeGenerateZ]?.toDouble() ?: 2000.0
    absZOfWell = probe["Z"]?.toDouble() ?: 1000.0
    var newZ = absZOfWell - generateZ
    newZ = (Math.round(newZ * 100.0) / 100.0)
    probe[nameOfZAttribute] = newZ.toString()
  }
}

/**
 * Метод добавляет дополнительные точки для интервала. Это может
 * понадобиться при моделировании в Micromine - чтобы блочная модель
 * получалась более точной. Точки добавляются через каждый метр.
 * Если интервал меньше метра, тогда точки не добавляются
 */
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
    list.forEach {
      val col = interval.toMutableMap()
      col[nameOfAttributeGenerateZ] = it.toString()
      backCollection.add(col)
    }
  }
  return backCollection
}

/**
 * Скорректировать точки таким образом, что если две точки имеют
 * одинаковое значение generateZ (когда один слой заканчивается и
 * начинается другой), то первая точка незначительно приподнимается
 * вверх, а вторая незначительно опускается вниз
 */
fun correctPointsOfIntervals(intervals: List<MutableMap<String, String>>) {
  fun correct(well: List<MutableMap<String, String>>) {
    well.forEach { println(it) }
    val correctWell = well.toMutableList()
    // получить группы с одинаковым значением атрибута generateZ
    val groupWithEqualZ = correctWell
            .groupBy { it[nameOfAttributeGenerateZ]?.toDouble() }
            .values.filter { it.size == 2 } // взять группы с двумя элементами
    // группы со скорректированными значениями generateZ
    val groupWithCorrectZ = groupWithEqualZ.toMutableList()
    var v: Double
    groupWithCorrectZ.forEach { // перебор групп
      // взять значение generateZ из первого элемента группы. Первый
      // и второй элементы группы равны, поэтому здесь и далее
      // используется только первый элемент
      v = it.first()[nameOfAttributeGenerateZ]?.toDouble() ?: 0.0
      // скорректировать значения: первую отметку незначительно
      // приподнять, вторую отметку незначительно опустить
      it[0][nameOfAttributeGenerateZ] = (Math.round((v - 0.01) * 100.0) / 100.0).toString()
      it[1][nameOfAttributeGenerateZ] = (Math.round((v + 0.01) * 100.0) / 100.0).toString()
    }
    // заменить элементы исходных групп на элементы из групп со
    // скорректированными значениями
    Collections.replaceAll(correctWell as List<Any>?, groupWithEqualZ,
            groupWithCorrectZ)
    println("-----------")
    correctWell.forEach { println(it) }
    println("_________________")
  }

  val ids = intervals.map {it[nameOfAttributeID]}.toSet() // уникальные ID
  ids.forEach { idWell ->
    // получить интервалы текущей скважины
    val layersForCurrentWell = intervals.filter { it[nameOfAttributeID] == idWell }
    correct(layersForCurrentWell)
  }
}

/*
* Обработка пустых значений z в объектах, которые находятся в работе.
* В этих объектах могут отсутствовать значения по высотной отметке z.
* Добавлено в код, потому что редактирование входного файла
* приводит к изменению количества ячеек. Используется как временное
* решение, пока исходные данные не будут исправлены в БД
 */
fun checkWorkingObjects(file: File, table: MutableList<MutableMap<String, String>>) {
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
        // возможно потребуется добавить скважины по Нижне-Накынскому-4
      }
    }
  }
}