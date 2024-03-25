/*
 * Copyright (c) 2024 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.element.android.features.roomdirectory.impl.root

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.element.android.features.roomdirectory.impl.root.model.RoomDescriptionUiModel
import io.element.android.features.roomdirectory.impl.root.model.toUiModel
import io.element.android.libraries.architecture.Presenter
import io.element.android.libraries.designsystem.theme.components.SearchBarResultState
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.roomdirectory.RoomDirectoryList
import io.element.android.libraries.matrix.api.roomdirectory.RoomDirectoryService
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

class RoomDirectoryPresenter @Inject constructor(
    private val client: MatrixClient,
    private val roomDirectoryService: RoomDirectoryService,
) : Presenter<RoomDirectoryState> {

    private val roomDirectoryList = roomDirectoryService.createRoomDirectoryList()

    @Composable
    override fun present(): RoomDirectoryState {

        var searchQuery by rememberSaveable {
            mutableStateOf("")
        }
        val allRooms by roomDirectoryList.collectItemsAsState()
        val hasMoreToLoad by produceState(initialValue = true, allRooms) {
            value = roomDirectoryList.hasMoreToLoad()
        }
        val coroutineScope = rememberCoroutineScope()
        LaunchedEffect(searchQuery) {
            roomDirectoryList.filter(searchQuery, 20)
        }
        fun handleEvents(event: RoomDirectoryEvents) {
            when (event) {
                is RoomDirectoryEvents.JoinRoom -> {
                    //coroutineScope.joinRoom(event.roomId)
                }
                RoomDirectoryEvents.LoadMore -> {
                    coroutineScope.launch {
                        roomDirectoryList.loadMore()
                    }
                }
                is RoomDirectoryEvents.Search -> {
                    searchQuery = event.query
                }
                is RoomDirectoryEvents.SearchActiveChange -> {

                }
            }
        }

        return RoomDirectoryState(
            query = searchQuery,
            roomDescriptions = allRooms,
            displayLoadMoreIndicator = hasMoreToLoad,
            eventSink = ::handleEvents
        )
    }

    @Composable
    private fun searchResults(
        filteredRooms: ImmutableList<RoomDescriptionUiModel>,
        hasMoreToLoad: Boolean,
        isSearchActive: Boolean,
    ): SearchBarResultState<ImmutableList<RoomDescriptionUiModel>> {
        return if (!isSearchActive) {
            SearchBarResultState.Initial()
        } else {
            if (filteredRooms.isEmpty() && !hasMoreToLoad) {
                SearchBarResultState.NoResultsFound()
            } else {
                SearchBarResultState.Results(filteredRooms)
            }
        }
    }

    @Composable
    private fun RoomDirectoryList.collectItemsAsState() = remember {
        items.map { list ->
            list
                .map { roomDescription -> roomDescription.toUiModel() }
                .toImmutableList()
        }.flowOn(Dispatchers.Default)
    }.collectAsState(persistentListOf())
}
