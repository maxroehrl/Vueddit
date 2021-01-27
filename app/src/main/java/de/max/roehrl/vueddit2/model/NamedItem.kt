package de.max.roehrl.vueddit2.model

open class NamedItem(val id: String) {
    companion object Loading: NamedItem("loading")

    override fun equals(other: Any?): Boolean {
        return other is NamedItem && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}