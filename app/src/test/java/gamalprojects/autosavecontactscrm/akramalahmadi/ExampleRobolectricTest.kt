package gamalprojects.autosavecontactscrm.akramalahmadi

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(gamalprojects.autosavecontactscrm.akramalahmadi.R.string.app_name)
    assertEquals("AutoSave Contacts CRM", appName)
  }
}
