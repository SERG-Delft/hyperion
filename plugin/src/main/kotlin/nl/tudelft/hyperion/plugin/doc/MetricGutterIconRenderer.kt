package nl.tudelft.hyperion.plugin.doc

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.markup.GutterIconRenderer
import nl.tudelft.hyperion.plugin.visualization.GUTTER_ICON
import nl.tudelft.hyperion.plugin.visualization.actions.OpenLineGraphAction
import javax.swing.Icon

/**
 * Represents a gutter icon that displays next to the metric inlays that opens
 * an action group when clicked.
 *
 * @property logicalLine the corresponding logical line to this gutter icon.
 */
class MetricGutterIconRenderer(private val logicalLine: Int) : GutterIconRenderer() {
    companion object {
        const val CLICK_GROUP_ID = "nl.tudelft.hyperion.plugin.visualization.actions.GraphActionGroup"
    }

    override fun getIcon(): Icon = GUTTER_ICON

    override fun hashCode(): Int {
        logger<MetricGutterIconRenderer>().warn("${this.javaClass.name}.hashCode() should not be used")
        return 1
    }

    override fun equals(other: Any?): Boolean {
        logger<MetricGutterIconRenderer>().warn("${this.javaClass.name}.equals() should not be used")
        return this === other
    }

    override fun getPopupMenuActions(): ActionGroup? {
        val actionManager = ActionManager.getInstance()

        // Save the line number of this gutter icon when the popup menu is
        // opened, this is for when the current line number action is chosen,
        OpenLineGraphAction.cachedLogicalLine = logicalLine

        return actionManager.getAction(CLICK_GROUP_ID) as ActionGroup
    }

    override fun getTooltipText(): String? = "Click to view all actions"

    override fun isNavigateAction(): Boolean = true
}
