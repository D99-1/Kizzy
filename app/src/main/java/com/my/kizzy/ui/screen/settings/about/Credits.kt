package com.my.kizzy.ui.screen.settings.about

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.my.kizzy.R
import com.my.kizzy.data.remote.Contributor
import com.my.kizzy.domain.model.Resource
import com.my.kizzy.ui.components.BackButton
import com.my.kizzy.ui.components.CreditItem
import com.my.kizzy.ui.components.Subtitle
import com.skydoves.landscapist.glide.GlideImage
import kotlin.math.floor

data class Credit(val title: String = "", val license: String = "", val url: String = "")

const val GPL_V3 = "GNU General Public License v3.0"
const val APACHE_V2 = "Apache License, Version 2.0"
const val MIT = "MIT License"

const val app_home_page="https://kizzy.vercel.app"
const val readYou = "https://github.com/Ashinch/ReadYou"
const val seal = "https://github.com/JunkFood02/Seal"
const val materialColor = "https://github.com/material-foundation/material-color-utilities"
const val nintendoRepo = "https://github.com/ninstar/Rich-Presence-U"
const val XboxRpc = "https://github.com/MrCoolAndroid/Xbox-Rich-Presence-Discord"

val creditsList = listOf(
    Credit("Read You", GPL_V3, readYou),
    Credit("Seal", GPL_V3, seal),
    Credit("material-color-utilities", APACHE_V2, materialColor),
    Credit("Rich-Presence-U", GPL_V3, nintendoRepo),
    Credit("Xbox-Rich-Presence-Discord", MIT, XboxRpc)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Credits(viewModel: CreditsScreenViewModel,onBackPressed: () -> Unit) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        rememberTopAppBarState(),
        canScroll = { true })

    val uriHandler = LocalUriHandler.current
    fun openUrl(url: String) {
        uriHandler.openUri(url)
    }
    val contributors = viewModel.contributors.collectAsState()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.credits),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                },
                navigationIcon = { BackButton{ onBackPressed() } },
                scrollBehavior = scrollBehavior
            )
        }
    ){ paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)){
            item {
                Subtitle(text = stringResource(id = R.string.design_credits))
                }
            items(creditsList){item: Credit ->
                CreditItem(title = item.title,
                description = item.license) {
                    openUrl(item.url)
                }
            }
            item {
                Subtitle(text = stringResource(id = R.string.contributors))
            }
            item {
                when (contributors.value) {
                    is Resource.Success -> {
                        // dirty way to enable LazyVerticalGird,
                        // TODO update this part once compose team release out a better
                        //  implementation of nested scrolling
                        val itemsPerRowAccordingToScreenWidth = floor((LocalConfiguration.current.screenWidthDp.dp / 90).value)
                        val totalHeightForLazyGrid =
                            contributors.value.data?.size?.div(itemsPerRowAccordingToScreenWidth)
                                ?.times((96))?.dp
                        if (totalHeightForLazyGrid != null) {
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(90.dp),
                                modifier = Modifier.height(totalHeightForLazyGrid)
                            ) {
                                items(contributors.value.data!!) {
                                    ContributorItem(contributor = it)
                                }
                            }
                        }
                    }
                    is Resource.Error -> {
                        Text(
                            text = contributors.value.message + "",
                            modifier = Modifier
                                .padding(16.dp, 20.dp),
                        )
                    }
                    is Resource.Loading -> {
                       CircularProgressIndicator(
                           modifier = Modifier
                               .padding(16.dp, 20.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ContributorItem(contributor: Contributor) {
    val uriHandler = LocalUriHandler.current
    val absoluteElevation = LocalAbsoluteTonalElevation.current + 2.dp
    Box(
        modifier = Modifier
            .padding(5.dp)
            .aspectRatio(ratio = 1f)
            .clip(RoundedCornerShape(25.dp))
            .background(MaterialTheme.colorScheme.surfaceColorAtElevation(absoluteElevation))
            .clickable {
                uriHandler.openUri(contributor.url)
            }
    ) {
        GlideImage(
            imageModel = contributor.avatar,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 5.dp)
                .size(60.dp)
                .clip(RoundedCornerShape(26.dp)),
            previewPlaceholder = R.drawable.error_avatar
        )
        Text(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp),
            text = contributor.name,
            maxLines = 1,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview
@Composable
fun CreditScreen() {

}