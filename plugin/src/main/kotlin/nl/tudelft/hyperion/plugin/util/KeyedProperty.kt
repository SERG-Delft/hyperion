package nl.tudelft.hyperion.plugin.util

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import kotlin.reflect.KProperty

/**
 * Helper property delegate that attaches to a [UserDataHolder] and allows
 * for direct access to user data stored in that object.
 *
 * Example:
 * ```kotlin
 * val key = Key.create<Int>("some.key")
 * var Editor.myData by KeyedProperty(key)
 *
 * println("Data is ${editor.myData}")
 * editor.myData = 10
 * ```
 */
class KeyedProperty<T>(private val key: Key<T>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        val holder = thisRef as UserDataHolder
        return holder.getUserData(key)
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val holder = thisRef as UserDataHolder
        holder.putUserData(key, value)
    }
}
