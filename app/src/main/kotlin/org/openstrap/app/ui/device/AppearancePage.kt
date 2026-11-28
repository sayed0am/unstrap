package org.openstrap.app.ui.device

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.openstrap.app.R
import org.openstrap.app.ui.glow.HaloPageHeader
import org.openstrap.app.ui.glow.HaloPageList
import org.openstrap.app.ui.glow.PageGroup
import org.openstrap.app.ui.glow.PageVariant
import org.openstrap.app.ui.glow.SwitchRow

/** The Appearance page, opened from the You page: how the app and its widgets look. */
@Composable
fun AppearancePage(
    plainNumbers: Boolean,
    contentPadding: PaddingValues,
    onPlainNumbers: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    HaloPageList(contentPadding, variant = PageVariant.Settings) {
        item { HaloPageHeader(stringResource(R.string.you_row_appearance), onBack) }
        item {
            PageGroup {
                SwitchRow(
                    stringResource(R.string.you_row_plain_numbers), stringResource(R.string.you_row_plain_numbers_note),
                    plainNumbers, onPlainNumbers, divider = false,
                )
            }
        }
    }
}
