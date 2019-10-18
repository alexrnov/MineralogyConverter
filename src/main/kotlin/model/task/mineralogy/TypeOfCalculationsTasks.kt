package model.task.mineralogy

class TypeOfCalculationsTasks(private val taskName: String, private val keys: List<String>) {

  private var calculationsTask: CalculationsTask = { }
  private var addAttributes: AddAttributes = { _, _ -> }

  fun getAlgorithm(): Pair<AddAttributes, CalculationsTask> {
    when {
      // точки будут подсвечены по возрасту при условии, что по ним имеются находки
      taskName.contains("подсветить точки по возрасту") -> highlightByAge()
      taskName == "общая сохранность" -> commonSafety()
      else -> {}
    }
    return Pair(addAttributes, calculationsTask)
  }

  private fun highlightByAge() {
    val ageIndex = taskName.split(";;").run { this.takeIf { it.size > 1 }?.let { this[1].trim() } ?: "" }
    // если входной файл - интервалы по всем точкам наблюдения
    if (keys.size != numberAttributesAllProbes || ageIndex.isEmpty()) return
    // при чтении файла, в коллекцию упрощенных проб добавлять атрибуты
    addAttributes = { simpleProbeMap, currentProbeList ->
      simpleProbeMap[keys[16]] = currentProbeList[16] // стратиграфия
      simpleProbeMap[keys.last()] = currentProbeList[keys.lastIndex] // находки
    }
    // выделять точки со стратиграфическим индексом, указанным во
    // входных параметрах, при условии, что для них есть находки МСА
    calculationsTask = { layersForCurrentWell ->
      layersForCurrentWell.map {
        // если у текущего пласта стратиграфический возраст не совпадает с искомым
        // стратиграфическим индексом и для этого пласта есть находки, тогда для
        // атрибута "находки" установить значение "0.0"
        val layerAge = it[keys[16]] ?: "" // стратиграфия
        if (!layerAge.contains(ageIndex) && it[keys.last()] == "1.0") it[keys.last()] = "0.0"
      }
    }
  }

  private fun commonSafety() {
    addAttributes = { simpleProbeMap, currentProbeList ->
      simpleProbeMap[keys[24]] = currentProbeList[24] // пиропы
      simpleProbeMap[keys[25]] = currentProbeList[25] // пикроильмениты

      simpleProbeMap[keys[32]] = currentProbeList[32] // пироп/износ_механический/0
      simpleProbeMap[keys[33]] = currentProbeList[33] // пироп/износ_механический/I
      simpleProbeMap[keys[34]] = currentProbeList[34] // пироп/износ_механический/II
      simpleProbeMap[keys[35]] = currentProbeList[35] // пироп/износ_механический/III
      simpleProbeMap[keys[36]] = currentProbeList[36] // пироп/износ_механический/IV
      simpleProbeMap[keys[37]] = currentProbeList[37] // пироп/осколки
      simpleProbeMap[keys[38]] = currentProbeList[38] // пироп/гипергенные
      simpleProbeMap[keys[39]] = currentProbeList[39] // пироп/трещиноватости
      simpleProbeMap[keys[40]] = currentProbeList[40] // пироп/включения

      simpleProbeMap[keys[126]] = currentProbeList[126] // пикроильменит/износ_механический/0
      simpleProbeMap[keys[127]] = currentProbeList[127] // пикроильменит/износ_механический/I
      simpleProbeMap[keys[128]] = currentProbeList[128] // пикроильменит/износ_механический/II
      simpleProbeMap[keys[129]] = currentProbeList[129] // пикроильменит/износ_механический/III
      simpleProbeMap[keys[130]] = currentProbeList[130] // пикроильменит/износ_механический/IV
      simpleProbeMap[keys[131]] = currentProbeList[131] // пикроильменит/осколки
      simpleProbeMap[keys[132]] = currentProbeList[132] // пикроильменит/гипергенные
      simpleProbeMap[keys[133]] = currentProbeList[133] // пикроильменит/вторичные
    }
  }
}