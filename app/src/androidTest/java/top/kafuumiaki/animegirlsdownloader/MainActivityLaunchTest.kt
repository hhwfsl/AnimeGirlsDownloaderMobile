package top.kafuumiaki.animegirlsdownloader

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityLaunchTest {
    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun activityStartsWithoutCrashing() {
        activityRule.scenario.onActivity { activity ->
            assertFalse(activity.isFinishing)
            assertFalse(activity.isDestroyed)
        }
    }
}
