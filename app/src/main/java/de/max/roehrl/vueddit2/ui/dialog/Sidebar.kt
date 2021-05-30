package de.max.roehrl.vueddit2.ui.dialog

import android.content.Context
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.service.Markdown
import de.max.roehrl.vueddit2.service.Reddit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Sidebar {
    fun show(context: Context, subredditName: String, scope: CoroutineScope) {
        val dialog = MaterialAlertDialogBuilder(context)
            .setView(R.layout.sidebar)
            .show()
        val tv = dialog.findViewById<TextView>(R.id.sidebar)
        scope.launch(Dispatchers.IO) {
            val text = Reddit.getInstance(context).getSidebar(subredditName)
            scope.launch(Dispatchers.Main) {
                Markdown.getInstance(context).setMarkdown(tv!!, text)
            }
        }
    }
}