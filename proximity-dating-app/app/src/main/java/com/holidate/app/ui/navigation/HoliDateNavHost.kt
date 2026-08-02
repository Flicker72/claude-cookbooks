package com.holidate.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.holidate.app.R
import com.holidate.app.ui.screens.ChatScreen
import com.holidate.app.ui.screens.DiscoverScreen
import com.holidate.app.ui.screens.MatchesScreen
import com.holidate.app.ui.screens.OnboardingScreen
import com.holidate.app.ui.screens.ProfileScreen
import com.holidate.app.viewmodel.ChatViewModel
import com.holidate.app.viewmodel.ChatViewModelFactory
import com.holidate.app.viewmodel.MainViewModel

private sealed class Tab(val route: String, val labelRes: Int, val icon: ImageVector) {
    data object Discover : Tab("discover", R.string.tab_discover, Icons.Filled.Explore)
    data object Matches : Tab("matches", R.string.tab_matches, Icons.Filled.Favorite)
    data object Profile : Tab("profile", R.string.tab_profile, Icons.Filled.Person)
}

private val tabs = listOf(Tab.Discover, Tab.Matches, Tab.Profile)

@Composable
fun HoliDateNavHost() {
    val vm: MainViewModel = viewModel()
    val self by vm.selfProfile.collectAsStateWithLifecycle()

    if (self == null) {
        OnboardingScreen(onSave = { name, age, bio, interests ->
            vm.saveProfile(name, age, bio, interests, photo = null)
            // Turn the mesh on as soon as there is a profile to advertise.
            vm.setMeshEnabled(true)
        })
    } else {
        MainShell(vm)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(vm: MainViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val onChatScreen = currentRoute?.startsWith("chat/") == true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                navigationIcon = {
                    if (onChatScreen) {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (!onChatScreen) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(Tab.Discover.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Tab.Discover.route,
            modifier = Modifier.padding(padding),
        ) {
            mainGraph(vm, nav)
        }
    }
}

private fun NavGraphBuilder.mainGraph(vm: MainViewModel, nav: NavHostController) {
    composable(Tab.Discover.route) {
        val profiles by vm.discoverable.collectAsStateWithLifecycle()
        DiscoverScreen(
            profiles = profiles,
            onLike = { vm.swipe(it, liked = true) },
            onPass = { vm.swipe(it, liked = false) },
        )
    }
    composable(Tab.Matches.route) {
        val matches by vm.matches.collectAsStateWithLifecycle()
        MatchesScreen(matches = matches, onOpenChat = { nav.navigate("chat/$it") })
    }
    composable(Tab.Profile.route) {
        val self by vm.selfProfile.collectAsStateWithLifecycle()
        val running by vm.meshRunning.collectAsStateWithLifecycle()
        val peers by vm.nearbyPeerCount.collectAsStateWithLifecycle()
        ProfileScreen(
            self = self,
            meshRunning = running,
            nearbyPeerCount = peers,
            onToggleMesh = { vm.setMeshEnabled(it) },
        )
    }
    composable("chat/{peerId}") { entry ->
        val peerId = entry.arguments?.getString("peerId").orEmpty()
        val chatVm: ChatViewModel = viewModel(factory = ChatViewModelFactory(peerId))
        val messages by chatVm.messages.collectAsStateWithLifecycle()
        ChatScreen(messages = messages, onSend = { chatVm.send(it) })
    }
}
