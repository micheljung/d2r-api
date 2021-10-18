import ch.micheljung.d2rapi.extract.SpriteConverterPlugin
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.WordSpec
import org.gradle.testfixtures.ProjectBuilder

class PluginTest : WordSpec({

  "Using the Plugin ID" should {
    "Apply the Plugin" {
      val project = ProjectBuilder.builder().build()
      project.pluginManager.apply("ch.micheljung.d2rapi.sprite-converter")

      project.plugins.getPlugin(SpriteConverterPlugin::class.java) shouldNotBe null
    }
  }
})