package model.exception

class ExcelException(message: String): Exception(message) {
  companion object {
    private val serialVersionUID: Long = 2L
  }
}