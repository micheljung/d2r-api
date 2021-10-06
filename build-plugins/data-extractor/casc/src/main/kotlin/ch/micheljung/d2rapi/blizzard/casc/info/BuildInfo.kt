package ch.micheljung.d2rapi.blizzard.casc.info

import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.*

/**
 * Top level CASC information file containing configuration information and
 * entry point references.
 */
class BuildInfo(
  private val fieldDescriptors: List<FieldDescriptor>,
  private val records: List<ArrayList<String>>
) {

  /**
   * Retrieves a specific field of a record.
   *
   * @param recordIndex Record index to lookup.
   * @param fieldIndex  Field index to retrieve of record.
   * @return Field value.
   * @throws IndexOutOfBoundsException When recordIndex or fieldIndex are out of
   * bounds.
   */
  fun getField(recordIndex: Int, fieldIndex: Int): String {
    return records[recordIndex][fieldIndex]
  }

  /**
   * Retrieves a specific field of a record.
   *
   * @param recordIndex Record index to lookup.
   * @param fieldName   Field name to retrieve of record.
   * @return Field value, or null if field does not exist.
   * @throws IndexOutOfBoundsException When recordIndex is out of bounds.
   */
  fun getField(recordIndex: Int, fieldName: String): String? {
    val fieldIndex = getFieldIndex(fieldName)
    return if (fieldIndex == -1) {
      null
    } else getField(recordIndex, fieldIndex)
  }

  /** The number of fields that make up each record.  */
  val fieldCount: Int
    get() = fieldDescriptors.size

  /**
   * Retrieve the field descriptor of a field index.
   *
   * @param fieldIndex Field index to retrieve descriptor from.
   * @return Field descriptor for field index.
   */
  fun getFieldDescriptor(fieldIndex: Int): FieldDescriptor {
    return fieldDescriptors[fieldIndex]
  }

  /**
   * Lookup the index of a named field. Returns the field index for the field name
   * if found, otherwise returns -1.
   *
   * @param name Name of the field to find.
   * @return Field index of field.
   */
  fun getFieldIndex(name: String): Int {
    var i = 0
    while (i < fieldDescriptors.size) {
      if (fieldDescriptors[i].name == name) {
        return i
      }
      i += 1
    }
    return -1
  }

  fun getBuildKey(): String {
    val fieldIndex = getFieldIndex("Build Key")
    check(fieldIndex != -1) { "Info missing field" }
    return getField(0, fieldIndex)
  }

  val recordCount: Int
    get() = records.size

  companion object {
    /** Name of the CASC build info file located in the install root (parent of the data folder).  */
    const val BUILD_INFO_FILE_NAME = ".build.info"

    /** Character encoding used by info files.  */
    private val FILE_ENCODING = StandardCharsets.UTF_8

    /** Field separator used by CASC info files.  */
    private const val FIELD_SEPARATOR_REGEX = "|"

    /** Helper method to separate a single line of info file into separate field strings.  */
    private fun separateFields(encodedLine: String): Array<String> {
      return encodedLine.split(FIELD_SEPARATOR_REGEX).toTypedArray()
    }

    fun read(infoFile: Path): BuildInfo {
      return Scanner(infoFile, FILE_ENCODING).use { lineScanner ->
        val fileDescriptors = separateFields(lineScanner.nextLine())
          .map { FieldDescriptor(it) }
          .toList()

        val records: MutableList<ArrayList<String>> = mutableListOf()
        while (lineScanner.hasNextLine()) {
          records.add(ArrayList(listOf(*separateFields(lineScanner.nextLine()))))
        }
        BuildInfo(fileDescriptors, records)
      }
    }
  }
}