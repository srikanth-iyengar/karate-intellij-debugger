package `in`.srikanthk.devlabs.kchopdebugger.service

import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import java.lang.reflect.Field

class PatchKarateRunner {
    companion object {
        val customScanResult: ScanResult = ClassGraph()
            .acceptPaths("/")
            .overrideClassLoaders(
                PatchKarateRunner::class.java.classLoader,
                Thread.currentThread().contextClassLoader
            )
            .scan(1)

    }

    fun patch() {
        try {
            val clazz = Class.forName(
                "com.intuit.karate.resource.ResourceUtils"
            )

            clazz.modifiers;
        } catch (e: ClassNotFoundException) {

        }
        try {
            val clazz = Class.forName(
                "com.intuit.karate.resource.ResourceUtils",
                false,
                PatchKarateRunner::class.java.classLoader
            )

            val field: Field = clazz.getDeclaredField("SCAN_RESULT")
            field.isAccessible = true

            // Remove final (Java 12+ doesn't allow changing 'modifiers' directly, so we rely on accessible + setting)
            val theUnsafe = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
            theUnsafe.isAccessible = true
            val unsafe = theUnsafe.get(null) as sun.misc.Unsafe
            println("Thread: ${Thread.currentThread()} ${Thread.currentThread().name}")

            val staticFieldBase = unsafe.staticFieldBase(field)
            val staticFieldOffset = unsafe.staticFieldOffset(field)

            unsafe.putObject(staticFieldBase, staticFieldOffset, customScanResult)

            println("✅ Patched SCAN_RESULT in ResourceUtils")
        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ Failed to patch SCAN_RESULT: ${e.message}")
        }
    }
}
