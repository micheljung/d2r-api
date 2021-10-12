import ch.micheljung.d2rapi.dto.SchemaGenerator
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.gradle.api.logging.Logger

class SchemaGeneratorTest : StringSpec({
  val logger: Logger = mockk()
  val schemaGenerator = SchemaGenerator(logger)

  "1 should result in Int" {
    val current = SchemaGenerator.ColumnProperties("test")
    schemaGenerator.detectColumnProperties(current, "1") shouldBe current.copy(hasInt = true)
  }
  "2147483647 should result in Int" {
    val current = SchemaGenerator.ColumnProperties("test")
    schemaGenerator.detectColumnProperties(current, "2147483647") shouldBe current.copy(hasInt = true)
  }
  "2147483648 should result in Long" {
    val current = SchemaGenerator.ColumnProperties("test")
    schemaGenerator.detectColumnProperties(current, "2147483648") shouldBe current.copy(hasLong = true)
  }
  "9223372036854775807 should result in Long" {
    val current = SchemaGenerator.ColumnProperties("test")
    schemaGenerator.detectColumnProperties(current, "9223372036854775807") shouldBe current.copy(hasLong = true)
  }
  "9223372036854775808 should result in BigInteger" {
    val current = SchemaGenerator.ColumnProperties("test")
    schemaGenerator.detectColumnProperties(current, "9223372036854775808") shouldBe current.copy(hasBigInteger = true)
  }
  "1.0 should result in Float" {
    val current = SchemaGenerator.ColumnProperties("test")
    schemaGenerator.detectColumnProperties(current, "1.0") shouldBe current.copy(hasFloat = true)
  }
  "1.00 should result in Float" {
    val current = SchemaGenerator.ColumnProperties("test")
    schemaGenerator.detectColumnProperties(current, "1.00") shouldBe current.copy(hasFloat = true)
  }
  "1.0000001 should result in Float" {
    val current = SchemaGenerator.ColumnProperties("test")
    schemaGenerator.detectColumnProperties(current, "1.0000001") shouldBe current.copy(hasFloat = true)
  }
  "1.00000001 should result in Double" {
    val current = SchemaGenerator.ColumnProperties("test")
    schemaGenerator.detectColumnProperties(current, "1.00000001") shouldBe current.copy(hasDouble = true)
  }
  "1.000000000000001 should result in Double" {
    val current = SchemaGenerator.ColumnProperties("test")
    schemaGenerator.detectColumnProperties(current, "1.000000000000001") shouldBe current.copy(hasDouble = true)
  }
  "1.0000000000000001 should result in BigDecimal" {
    val current = SchemaGenerator.ColumnProperties("test")
    schemaGenerator.detectColumnProperties(current, "1.0000000000000001") shouldBe current.copy(hasBigDecimal = true)
  }
  "abc should result in String" {
    val current = SchemaGenerator.ColumnProperties("test")
    schemaGenerator.detectColumnProperties(current, "abc") shouldBe current.copy(hasString = true)
  }
  "test" {
    val current = SchemaGenerator.ColumnProperties("test", hasInt = true)
    schemaGenerator.detectColumnProperties(current, "abc") shouldBe current.copy(hasString = true)
  }

})
