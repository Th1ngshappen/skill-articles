package ru.skillbranch.skillarticles.viewmodels.base

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle

interface IViewModelState {
    /**
     * override this if need to save state in bundle
     */
    fun save(outState: SavedStateHandle) {
        // default empty implementation
    }

    /**
     * override this if need to restore state from bundle
     */
    fun restore(savedState: SavedStateHandle): IViewModelState {
        // default empty implementation
        return this
    }
}