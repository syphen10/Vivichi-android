package com.vivichi.app.showcase

import android.content.Intent
import com.vivichi.app.ui.VivichiViewModel

/** No-op in release builds. The real screenshot profile lives only in the debug source set. */
object ShowcaseLoader {
    @Suppress("UNUSED_PARAMETER")
    fun maybeLoad(intent: Intent?, viewModel: VivichiViewModel) = Unit
}
