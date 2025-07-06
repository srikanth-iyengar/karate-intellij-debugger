package `in`.srikanthk.devlabs.kchopdebugger.listener

import com.intellij.openapi.project.Project
import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XBreakpointListener
import `in`.srikanthk.devlabs.kchopdebugger.service.KarateExecutionService

class KarateBreakpointListener(private val project: Project) : XBreakpointListener<XBreakpoint<*>> {

    override fun breakpointAdded(breakpoint: XBreakpoint<*>) {
        breakpoint.sourcePosition?.let { sourcePosition ->
            val executionService = project.getService(KarateExecutionService::class.java)
            executionService.addBreakpoint(sourcePosition.file.path, sourcePosition.line + 1)
        }
        super.breakpointAdded(breakpoint)
    }

    override fun breakpointRemoved(breakpoint: XBreakpoint<*>) {
        breakpoint.sourcePosition?.let { sourcePosition ->
            val executionService = project.getService(KarateExecutionService::class.java)
            executionService.removeBreakpoint(sourcePosition.file.path, sourcePosition.line + 1)
        }
        super.breakpointRemoved(breakpoint)
    }
}