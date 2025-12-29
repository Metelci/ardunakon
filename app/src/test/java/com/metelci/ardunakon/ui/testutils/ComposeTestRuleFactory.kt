package com.metelci.ardunakon.ui.testutils

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.robolectric.Shadows.shadowOf

typealias RegisteredComposeRule =
    AndroidComposeTestRule<ActivityScenarioRule<ComponentActivity>, ComponentActivity>

fun createRegisteredComposeRule(): RegisteredComposeRule {
    registerComponentActivity()
    return createAndroidComposeRule<ComponentActivity>()
}

private fun registerComponentActivity() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val shadowPackageManager = shadowOf(context.packageManager)
    val component = ComponentName(context, ComponentActivity::class.java)
    val activityInfo = ActivityInfo().apply {
        name = component.className
        packageName = component.packageName
        exported = true
    }

    val methods = shadowPackageManager.javaClass.methods
    methods.firstOrNull {
        it.name == "addOrUpdateActivity" && it.parameterTypes.size == 2
    }?.let { method ->
        method.invoke(shadowPackageManager, component, activityInfo)
        return
    }
    methods.firstOrNull {
        it.name == "addActivityIfNotPresent" &&
            it.parameterTypes.size == 1 &&
            it.parameterTypes[0] == ComponentName::class.java
    }?.let { method ->
        method.invoke(shadowPackageManager, component)
        return
    }
    methods.firstOrNull {
        it.name == "addActivity" && it.parameterTypes.size == 1 && it.parameterTypes[0] == ActivityInfo::class.java
    }?.let { method ->
        method.invoke(shadowPackageManager, activityInfo)
        return
    }
    methods.firstOrNull {
        it.name == "addResolveInfoForIntent" && it.parameterTypes.size == 2
    }?.let { method ->
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(component)
        val resolveInfo = ResolveInfo().apply { this.activityInfo = activityInfo }
        method.invoke(shadowPackageManager, intent, resolveInfo)
        return
    }

    error("Unable to register ComponentActivity with Robolectric package manager.")
}
