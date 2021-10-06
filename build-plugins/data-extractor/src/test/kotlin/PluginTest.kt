import ch.micheljung.d2rapi.extract.DataExtractorPlugin
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.WordSpec
import org.gradle.testfixtures.ProjectBuilder

class PluginTest : WordSpec({

  "Using the Plugin ID" should {
    "Apply the Plugin" {
      val project = ProjectBuilder.builder().build()
      project.pluginManager.apply("ch.micheljung.d2rapi.data-extractor")

      project.plugins.getPlugin(DataExtractorPlugin::class.java) shouldNotBe null
    }
  }
})