package model.task.mineralogy

class TypeOfCalculationsTasks(private val taskName: String, private val keys: List<String>) {

  private var calculationsTask: CalculationsTask = { }
  private var addAttributes: AddAttributes = { _, _ -> }

  fun getAlgorithm(): Pair<AddAttributes, CalculationsTask> {
    when {
      taskName == "выделить точки по находкам" -> justAdditionalPoints() // дополнительные точки будут выделены по возрасту (находки 0.0 или 1.0)
      taskName.contains("выделить точки по находкам и возрасту") -> highlightByAge() // точки будут выделены по возрасту при условии, что по ним имеются находки
      taskName == "вычислить общую сохранность" -> commonSafety() // у дополнительных точек будет дополнительный параметр - общая сохранность
      else -> {}
    }
    return Pair(addAttributes, calculationsTask)
  }

  private fun justAdditionalPoints() {
    if (keys.size != numberAttributesAllProbes) return
    addAttributes = { simpleProbeMap, currentProbeList ->
      simpleProbeMap[keys[16]] = currentProbeList[16] // стратиграфия
      simpleProbeMap[keys.last()] = currentProbeList[keys.lastIndex] // находки
    }
  }

  private fun highlightByAge() {
    val ageIndex = taskName.split(";;").run { this.takeIf { it.size > 1 }?.let { this[1].trim() } ?: "" }
    // входной файл должен быть файлом с  интерваломи по всем точкам наблюдений
    if (keys.size != numberAttributesAllProbes || ageIndex.isEmpty()) return
    // при чтении файла, в коллекцию упрощенных проб добавлять атрибуты
    addAttributes = { simpleProbeMap, currentProbeList ->
      simpleProbeMap[keys[16]] = currentProbeList[16] // стратиграфия
      simpleProbeMap[keys.last()] = currentProbeList[keys.lastIndex] // находки
    }
    // выделять точки со стратиграфическим индексом, указанным во
    // входных параметрах, при условии, что для них есть находки МСА
    calculationsTask = { layersForCurrentWell ->
      for (layer in layersForCurrentWell) {
        // если у текущего пласта стратиграфический возраст не совпадает с искомым
        // стратиграфическим индексом и для этого пласта есть находки, тогда для
        // атрибута "находки" установить значение "0.0"
        val layerAge = layer[keys[16]] ?: "" // стратиграфия
        if (!layerAge.contains(ageIndex) && layer[keys.last()] == "1.0") layer[keys.last()] = "0.0"
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

      simpleProbeMap["общая сохранность"] = "-1.0"
    }

    calculationsTask = { layersForCurrentWell ->
      for (layer in layersForCurrentWell) {
        val amountPyrope: Double = layer[keys[24]]?.toDouble() ?: 0.0
        var wear0: Double = layer[keys[32]]?.toDouble() ?: 0.0
        var wear1: Double = layer[keys[33]]?.toDouble() ?: 0.0
        var wear2: Double = layer[keys[34]]?.toDouble() ?: 0.0
        var wear3: Double = layer[keys[35]]?.toDouble() ?: 0.0
        var wear4: Double = layer[keys[36]]?.toDouble() ?: 0.0
        var debris: Double = layer[keys[37]]?.toDouble() ?: 0.0 // осколки
        var hypergene: Double = layer[keys[38]]?.toDouble() ?: 0.0 // гипергенные
        val brokenCondition: Double = layer[keys[39]]?.toDouble() ?: 0.0 // трещиноватость
        val inclusion: Double = layer[keys[40]]?.toDouble() ?: 0.0 // включения
        var safePyrope = 0.0
        if (amountPyrope > 0.0) {
          safePyrope = (wear0 * 6 + wear1 * 5 + wear2 * 4 + wear3 * 3 + wear4 * 2 +
          debris * 1 + hypergene * 1 + brokenCondition * 1 + inclusion * 1) / amountPyrope
        }
        /*
        println("amountPyrope = $amountPyrope")
        println("wear0 = $wear0, wear1 = $wear1, wear2 = $wear2, wear3 = $wear3," +
                "wear4 = $wear4, debris = $debris, hypergene = $hypergene, " +
                "brokenCondition = $brokenCondition, inclusion = $inclusion")
        println("safePyrope = $safePyrope")
        println("-")
        */

        val amountPicroilmenite: Double = layer[keys[25]]?.toDouble() ?: 0.0
        wear0 = layer[keys[126]]?.toDouble() ?: 0.0
        wear1 = layer[keys[127]]?.toDouble() ?: 0.0
        wear2 = layer[keys[128]]?.toDouble() ?: 0.0
        wear3 = layer[keys[129]]?.toDouble() ?: 0.0
        wear4 = layer[keys[130]]?.toDouble() ?: 0.0
        debris = layer[keys[131]]?.toDouble() ?: 0.0
        hypergene = layer[keys[132]]?.toDouble() ?: 0.0
        val secondary = layer[keys[133]]?.toDouble() ?: 0.0 // вторичные
        var safePicroilmenite = 0.0
        if (amountPicroilmenite > 0.0) {
          safePicroilmenite = (wear0 * 6 + wear1 * 5 + wear2 * 4 + wear3 * 3 + wear4 * 2 +
                  debris * 1 + hypergene * 1 + secondary * 1) / amountPicroilmenite
        }

        /*
        println("amountPicroilmenite = $amountPicroilmenite")
        println("wear0 = $wear0, wear1 = $wear1, wear2 = $wear2, wear3 = $wear3," +
                "wear4 = $wear4, debris = $debris, hypergene = $hypergene, " +
                "secondary = $secondary")
        println("safePicroilmenite = $safePicroilmenite")
        */

        val allSafe = if (safePyrope > 0.0 && safePicroilmenite > 0.0) ((safePyrope + safePicroilmenite) / 2)
        else (safePyrope + safePicroilmenite)

        layer["общая сохранность"] = (Math.round(allSafe * 100.0) / 100.0).toString()
        /*
        println("-")
        println("all safe = ${layer["общая сохранность"]}")
        println("------------------------------")
        */
        /*
        if (layer["ID"] == "177649") {
          println(layer)
        }
        */
        /*
        if (safePyrope == 0.0 && safePicroilmenite == 0.0) {
          println(layer["ID"])
        }
        */
      }
    }
  }

}