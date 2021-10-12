import ch.micheljung.d2rapi.dto.JsGeneratorPlugin
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldNotBe
import org.gradle.testfixtures.ProjectBuilder

class PluginTest : WordSpec({

  "Using the Plugin ID" should {
    "Apply the Plugin" {
      val project = ProjectBuilder.builder().build()
      project.pluginManager.apply("ch.micheljung.d2rapi.js-generator")

      project.plugins.getPlugin(JsGeneratorPlugin::class.java) shouldNotBe null
    }
  }
})