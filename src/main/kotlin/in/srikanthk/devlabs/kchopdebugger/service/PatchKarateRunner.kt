package `in`.srikanthk.devlabs.kchopdebugger.service

import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import java.lang.reflect.Field
import java.lang.reflect.Modifier

// Hack for plugin to work in win environment
class PatchKarateRunner {
    companion object {
        // 1. Create your custom ScanResult
        val customScanResult: ScanResult = ClassGraph().acceptPaths("/").overrideClassLoaders(
            this::class.java.classLoader, Thread.currentThread().contextClassLoader
        ).scan(1)
    }


    fun emptyFun() {
        try {
            // 2. Access the ResourceUtils class
            val clazz = Class.forName("com.intuit.karate.resource.ResourceUtils", false, this.javaClass.classLoader)

            // 3. Get the SCAN_RESULT field
            val field: Field = clazz.getDeclaredField("SCAN_RESULT")
            field.isAccessible = true

            // 4. Remove final modifier using reflection hack
            val modifiersField = Field::class.java.getDeclaredField("modifiers")
            modifiersField.isAccessible = true
            modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())

            // 5. Set your custom ScanResult
            field.set(null, customScanResult)

            println("✅ Patched SCAN_RESULT in ResourceUtils")
        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ Failed to patch SCAN_RESULT: ${e.message}")
        }

    }
}
