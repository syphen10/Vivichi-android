package com.vivichi.app.showcase

import android.content.Intent
import com.vivichi.app.ui.VivichiViewModel

/** No-op in release builds. The real screenshot profile lives only in the debug source set. */
object ShowcaseLoader {
    /** Never true in release: ads always show for non-Premium users. */
    val active: Boolean get() = false

    @Suppress("UNUSED_PARAMETER")
    fun maybeLoad(intent: Intent?, viewModel: VivichiViewModel) = Unit
}
