import ch.micheljung.d2rapi.dto.DtoGeneratorPlugin
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldNotBe
import org.gradle.testfixtures.ProjectBuilder

class PluginTest : WordSpec({

  "Using the Plugin ID" should {
    "Apply the Plugin" {
      val project = ProjectBuilder.builder().build()
      project.pluginManager.apply("ch.micheljung.d2rapi.dto-generator")

      project.plugins.getPlugin(DtoGeneratorPlugin::class.java) shouldNotBe null
    }
  }
})