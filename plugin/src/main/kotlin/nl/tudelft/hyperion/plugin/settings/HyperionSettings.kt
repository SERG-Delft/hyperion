package nl.tudelft.hyperion.plugin.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Attribute
import nl.tudelft.hyperion.plugin.graphs.HistogramInterval
import nl.tudelft.hyperion.plugin.graphs.HistogramSettings
import nl.tudelft.hyperion.plugin.graphs.HistogramSettingsConverter
import nl.tudelft.hyperion.plugin.graphs.ProjectScope

/**
 * Class that holds persistent states for Hyperion's project scope settings.
 */
@State(
    name = "HyperionProjectSettings",
    storages = [Storage("/hyperion.xml")]
)
class HyperionSettings(private val relatedProject: Project) : PersistentStateComponent<HyperionSettings.State> {
    private var currentState = State().apply {
        // Set default values.
        intervals = listOf(3600, 86400, 2592000)
        address = "hyperion.internal.yourcompany.com"
        project = relatedProject.name
        visualization = HistogramSettings(
            HistogramInterval.HalfHour,
            12,
            ProjectScope
        )
    }

    /**
     * State that holds Persistent values related to the current project.
     */
    class State {
        lateinit var intervals: List<Int>
        lateinit var address: String
        lateinit var project: String

        @Attribute(converter = HistogramSettingsConverter::class)
        lateinit var visualization: HistogramSettings
    }

    companion object {
        /**
         * Obtains an instance of HyperionSettings related to the specified Project.
         * This always returns an instance.
         */
        fun getInstance(project: Project): HyperionSettings {
            return project.getService(HyperionSettings::class.java)
        }
    }

    /**
     * Sets the intervals for the State.
     *
     * [State.intervals]
     */
    fun setIntervals(intervals: List<Int>) {
        currentState.intervals = intervals
    }

    /**
     * Sets the address of the state.
     * [State.address]
     */
    fun setAddress(address: String) {
        currentState.address = address
    }

    /**
     * Sets the project of the state.
     * [State.project]
     */
    fun setProject(project: String) {
        currentState.project = project
    }

    /**
     * @return a component state. All properties, public and annotated fields are serialized. Only values, which differ
     * from the default (i.e., the value of newly instantiated class) are serialized. `null` value indicates
     * that the returned state won't be stored, as a result previously stored state will be used.
     * @see com.intellij.util.xmlb.XmlSerializer
     */
    override fun getState(): State {
        return currentState
    }

    /**
     * This method is called when new component state is loaded. The method can and will be called several times, if
     * config files were externally changed while IDE was running.
     *
     * State object should be used directly, defensive copying is not required.
     *
     * @param state loaded component state
     * see [com.intellij.util.xmlb.XmlSerializerUtil.copyBean]
     */
    override fun loadState(state: State) {
        currentState = state
    }
}
