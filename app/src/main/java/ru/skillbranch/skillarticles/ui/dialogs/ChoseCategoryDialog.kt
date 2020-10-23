package ru.skillbranch.skillarticles.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.skillbranch.skillarticles.R
import ru.skillbranch.skillarticles.extensions.dpToIntPx
import ru.skillbranch.skillarticles.extensions.setPaddingOptionally

// 10: 02:31:35, 11: 55:19
class ChoseCategoryDialog : DialogFragment() {

    companion object {
        const val CHOOSE_CATEGORY_KEY = "CHOOSE_CATEGORY_KEY"
        const val SELECTED_CATEGORIES = "SELECTED_CATEGORIES"
    }

    private val selectedCategories = mutableSetOf<String>()
    private val args: ChoseCategoryDialogArgs by navArgs()

    private val categoryAdapter = CategoryAdapter { categoryId, isChecked ->
        if (isChecked) selectedCategories.add(categoryId)
        else selectedCategories.remove(categoryId)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        selectedCategories.clear()
        selectedCategories.addAll(
            savedInstanceState?.getStringArray(SELECTED_CATEGORIES) ?: args.selectedCategories
        )

        val categoryItems = args.categories.toList().map {
            it.toItem(selectedCategories.contains(it.categoryId))
        }

        categoryAdapter.submitList(categoryItems)

        val rvCategories = RecyclerView(requireContext()).apply {
            id = R.id.categories_list
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
            setPaddingOptionally(top = requireContext().dpToIntPx(16))
        }

        return AlertDialog.Builder(requireContext())
            .setView(rvCategories)
            .setTitle("Choose category")
            .setPositiveButton("Apply") { _, _ ->
                setFragmentResult(
                    CHOOSE_CATEGORY_KEY,
                    bundleOf(SELECTED_CATEGORIES to selectedCategories.toList())
                )
            }
            .setNegativeButton("Reset") { _, _ ->
                setFragmentResult(
                    CHOOSE_CATEGORY_KEY,
                    bundleOf(SELECTED_CATEGORIES to emptyList<String>())
                )
            }
            .create()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArray(SELECTED_CATEGORIES, selectedCategories.toTypedArray())
    }
}