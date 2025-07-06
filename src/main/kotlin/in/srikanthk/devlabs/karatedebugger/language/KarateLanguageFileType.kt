package `in`.srikanthk.devlabs.karatedebugger.language

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.NlsSafe
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

class KarateLanguageFileType : LanguageFileType {
    constructor() : super(LANG_INSTANCE) {
    }
    override fun getName(): @NonNls String {
        return "Karate"
    }

    override fun getDescription(): @NlsContexts.Label String {
        return "Karate file"
    }

    override fun getDefaultExtension(): @NlsSafe String {
        return "feature"
    }

    override fun getIcon(): Icon? {
        return AllIcons.FileTypes.Text
    }

    companion object {
        val LANG_INSTANCE = KarateLanguage();
        val INSTANCE = KarateLanguageFileType()
    }
}