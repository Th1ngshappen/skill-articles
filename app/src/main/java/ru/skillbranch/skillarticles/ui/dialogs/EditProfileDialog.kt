package ru.skillbranch.skillarticles.ui.dialogs

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import ru.skillbranch.skillarticles.R

class EditProfileDialog : DialogFragment() {

    companion object {
        const val EDIT_PROFILE_KEY = "EDIT_PROFILE_KEY"
        const val APPLIED_NAME = "APPLIED_NAME"
        const val APPLIED_ABOUT = "APPLIED_ABOUT"
    }

    private val args: EditProfileDialogArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val view = layoutInflater.inflate(R.layout.fragment_edit_profile_dialog, null) as ViewGroup
        val etName = (view.findViewById(R.id.et_name) as EditText).apply { setText(args.name) }
        val etAbout = (view.findViewById(R.id.et_about) as EditText).apply { setText(args.about) }

        return AlertDialog.Builder(requireContext())
            .setView(view)
            .setTitle("Edit profile")
            .setPositiveButton("Apply") { _, _ ->

                val appliedName = etName.text.trim().toString()
                val appliedAbout = etAbout.text.trim().toString()

                // if nothing changes do nothing
                if (appliedName == args.name && appliedAbout == args.about) return@setPositiveButton

                setFragmentResult(
                    EDIT_PROFILE_KEY,
                    bundleOf(APPLIED_NAME to appliedName, APPLIED_ABOUT to appliedAbout)
                )
            }
            .setNegativeButton("Cancel") { _, _ -> }
            .create()
    }

}