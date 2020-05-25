package nl.tudelft.hyperion.plugin.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

@State(
        name = "HyperionProjectSettings",
        storages = [Storage("/hyperion.xml")]
)
class HyperionSettings : PersistentStateComponent<HyperionSettings.State> {
    private var currentState = State().apply { intervals = listOf(3600, 86400, 2592000) }

    class State {
        public lateinit var intervals: List<Int>
    }

    fun setIntervals(intervals: List<Int>) {
        currentState.intervals = intervals
    }
    /**
     * @return a component state. All properties, public and annotated fields are serialized. Only values, which differ
     * from the default (i.e., the value of newly instantiated class) are serialized. `null` value indicates
     * that the returned state won't be stored, as a result previously stored state will be used.
     * @see com.intellij.util.xmlb.XmlSerializer
     */
    override fun getState(): State? {
        return currentState
    }

    /**
     * This method is called when new component state is loaded. The method can and will be called several times, if
     * config files were externally changed while IDE was running.
     *
     *
     * State object should be used directly, defensive copying is not required.
     *
     * @param state loaded component state
     * @see com.intellij.util.xmlb.XmlSerializerUtil.copyBean
     */
    override fun loadState(state: State) {
        currentState = state
    }
}