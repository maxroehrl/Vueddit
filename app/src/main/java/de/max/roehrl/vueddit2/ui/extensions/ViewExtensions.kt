package de.max.roehrl.vueddit2.ui.extensions

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import coil.load
import de.max.roehrl.vueddit2.R


fun ImageView.loadWithPlaceholders(data: Any?) {
    load(data) {
        crossfade(true)
        placeholder(getInsetDrawable(R.drawable.ic_image, context))
        error(getInsetDrawable(R.drawable.ic_broken_image, context))
    }
}

fun getInsetDrawable(@DrawableRes res: Int, context: Context, inset: Int = 40): Drawable {
    return InsetDrawable(ContextCompat.getDrawable(context, res), inset)
}