package de.max.roehrl.vueddit2.ui.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.max.roehrl.vueddit2.R

class BottomSheetFragment : BottomSheetDialogFragment() {
    companion object {
        private const val TAG = "BottomSheetFragment"
        private const val ITEMS = "Items"
        private const val SELECTED_ITEM = "Item"

        fun showSheet(context: Context, items: Map<String, () -> Unit>) {
            BottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putCharSequenceArray(ITEMS, items.keys.toTypedArray())
                }
                (context as AppCompatActivity).supportFragmentManager.apply {
                    setFragmentResultListener(TAG, context) { _, bundle ->
                        items[bundle.getCharSequence(SELECTED_ITEM)]?.invoke()
                        clearFragmentResultListener(TAG)
                    }
                    show(beginTransaction(), TAG)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet, container, false).apply {
            findViewById<ListView>(R.id.list_view).apply {
                val items = requireArguments().getCharSequenceArray(ITEMS)!!
                adapter = object : ArrayAdapter<CharSequence>(
                    requireContext(),
                    R.layout.bottom_sheet_item,
                    items
                ) {
                    override fun getView(
                        position: Int,
                        convertView: View?,
                        parent: ViewGroup
                    ): View {
                        return super.getView(position, convertView, parent).apply {
                            setOnClickListener {
                                setFragmentResult(TAG, bundleOf(SELECTED_ITEM to items[position]))
                                dismiss()
                            }
                        }
                    }
                }
            }
        }
    }
}
