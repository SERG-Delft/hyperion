@file:JvmName("CodeList")

package nl.tudelft.hyperion.plugin.visualization

import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel

class CodeList {
    lateinit var codeList: JList<String>
    lateinit var root: JPanel
    lateinit var label: JLabel

    val content
        get() = root

    fun createUIComponent() {
        println("fooo")
    }
}