package nl.tudelft.hyperion.plugin.util

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IconLoader

/**
 * Class that handles notifications for the Hyperion plugin.
 * These notifications are project-bound and show in the event log.
 */
class HyperionNotifier {
    companion object {
        private val NOTIFICATION_GROUP = NotificationGroup(
            "com.github.serg-delft.hyperion",
            NotificationDisplayType.BALLOON,
            true,
            "nl.tudelft.hyperion.plugin.visualization.actions.GraphActionGroup",
            IconLoader.getIcon("icons/tool_window_icon.png"),
            "Hyperion"
        )

        /**
         * Sends an error notification to the given project with given content.
         * @param project Project to send the notification in.
         * @param content The content of the notification.
         */
        fun error(project: Project, content: String) {
            notify(project, content, NotificationType.ERROR)
        }
        // Other types of notifications can go here but for now we only need error.

        /**
         * Creates an event log notification in the given project with given content and type.
         * @param project Project to send the notification in.
         * @param content The content of the notification.
         * @param type The type of notification to send.
         */
        private fun notify(project: Project, content: String, type: NotificationType) {
            NOTIFICATION_GROUP.createNotification(content, type).notify(project)
        }
    }
}