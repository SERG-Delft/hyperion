package nl.tudelft.hyperion.plugin.doc

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.markup.GutterIconRenderer
import nl.tudelft.hyperion.plugin.visualization.GUTTER_ICON
import javax.swing.Icon

class MetricGutterIconRenderer : GutterIconRenderer() {
    companion object {
        const val CLICK_GROUP_ID = "nl.tudelft.hyperion.plugin.visualization.actions.GraphActionGroup"
    }

    override fun getIcon(): Icon = GUTTER_ICON

    override fun hashCode(): Int {
        logger<MetricGutterIconRenderer>().warn("${this.javaClass.name}.hashCode() should not be used")
        return 1
    }

    @SuppressWarnings("EqualsAlwaysReturnsTrueOrFalse")
    override fun equals(other: Any?): Boolean {
        logger<MetricGutterIconRenderer>().warn("${this.javaClass.name}.equals() should not be used")
        return false
    }

    override fun getPopupMenuActions(): ActionGroup? {
        val actionManager = ActionManager.getInstance()
        return actionManager.getAction(CLICK_GROUP_ID) as ActionGroup
    }

    override fun isNavigateAction(): Boolean = true
}
