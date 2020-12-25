package de.max.roehrl.vueddit2.model

open class NamedItem(val id: String) {
    companion object Loading: NamedItem("loading")
}