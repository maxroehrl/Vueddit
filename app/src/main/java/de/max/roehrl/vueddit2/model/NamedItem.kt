package de.max.roehrl.vueddit2.model

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
open class NamedItem(val id: String) : Parcelable {
    companion object {
        val Loading = NamedItem("loading")
    }

    private constructor(parcel: Parcel) : this(id = parcel.readString()!!)

    override fun equals(other: Any?): Boolean {
        return other is NamedItem && id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}