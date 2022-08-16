package de.max.roehrl.vueddit2.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.max.roehrl.vueddit2.R
import de.max.roehrl.vueddit2.service.Markdown
import de.max.roehrl.vueddit2.service.Reddit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SidebarFragment : BottomSheetDialogFragment(R.layout.sidebar) {
    companion object {
        private const val TAG = "Sidebar"
        private const val TEXT = "SidebarText"

        fun show(context: Context, subredditName: String, scope: CoroutineScope) {
            scope.launch(Dispatchers.IO) {
                val text = Reddit.getInstance(context).getSidebar(subredditName)
                scope.launch(Dispatchers.Main) {
                    SidebarFragment().apply {
                        arguments = Bundle().apply {
                            putString(TEXT, text)
                        }
                        show((context as AppCompatActivity).supportFragmentManager, TAG)
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tv = view.findViewById<TextView>(R.id.sidebar)
        Markdown.getInstance(requireContext())
            .setMarkdown(tv!!, requireArguments().getString(TEXT)!!)
    }
}
