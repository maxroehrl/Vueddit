package de.max.roehrl.vueddit2.ui.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.service.Markdown
import de.max.roehrl.vueddit2.service.Reddit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class Sidebar(
    context: Context,
    private val subredditName: String,
    private val scope: CoroutineScope,
) : Dialog(context) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.sidebar)
        val tv = findViewById<TextView>(R.id.sidebar)
        scope.launch(Dispatchers.IO) {
            val text = Reddit.getSidebar(subredditName)
            scope.launch(Dispatchers.Main) {
                Markdown.getInstance(context).setMarkdown(tv, text)
            }
        }
    }
}