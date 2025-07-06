package `in`.srikanthk.devlabs.karatedebugger.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import `in`.srikanthk.devlabs.karatedebugger.service.KarateExecutionService
import java.util.concurrent.CompletableFuture

open class RunTestAction : AnAction() {
    override fun actionPerformed(action: AnActionEvent) {
        val project = action.project ?: return

        val file = action.getData(CommonDataKeys.VIRTUAL_FILE);

        if (file?.extension != "feature") {
            return
        }

        val executionService = project.getService(KarateExecutionService::class.java);
        CompletableFuture.supplyAsync { executionService.executeSuite(file.path) }
    }
}