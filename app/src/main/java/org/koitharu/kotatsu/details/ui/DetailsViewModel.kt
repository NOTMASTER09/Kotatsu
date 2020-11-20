package org.koitharu.kotatsu.details.ui

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koitharu.kotatsu.base.domain.MangaDataRepository
import org.koitharu.kotatsu.base.ui.BaseViewModel
import org.koitharu.kotatsu.core.exceptions.MangaNotFoundException
import org.koitharu.kotatsu.core.model.FavouriteCategory
import org.koitharu.kotatsu.core.model.Manga
import org.koitharu.kotatsu.core.model.MangaHistory
import org.koitharu.kotatsu.favourites.domain.FavouritesRepository
import org.koitharu.kotatsu.favourites.domain.OnFavouritesChangeListener
import org.koitharu.kotatsu.history.domain.HistoryRepository
import org.koitharu.kotatsu.history.domain.OnHistoryChangeListener
import org.koitharu.kotatsu.local.domain.LocalMangaRepository
import org.koitharu.kotatsu.tracker.domain.TrackingRepository
import org.koitharu.kotatsu.utils.SingleLiveEvent
import org.koitharu.kotatsu.utils.ext.safe
import java.io.IOException

class DetailsViewModel(
	private val historyRepository: HistoryRepository,
	private val favouritesRepository: FavouritesRepository,
	private val localMangaRepository: LocalMangaRepository,
	private val trackingRepository: TrackingRepository,
	private val mangaDataRepository: MangaDataRepository
) : BaseViewModel(), OnHistoryChangeListener, OnFavouritesChangeListener {

	val mangaData = MutableLiveData<Manga>()
	val newChapters = MutableLiveData<Int>(0)
	val onMangaRemoved = SingleLiveEvent<Manga>()
	val history = MutableLiveData<MangaHistory?>()
	val favouriteCategories = MutableLiveData<List<FavouriteCategory>>()

	init {
		HistoryRepository.subscribe(this)
		FavouritesRepository.subscribe(this)
	}

	fun findMangaById(id: Long) {
		launchLoadingJob {
			val manga = mangaDataRepository.findMangaById(id)
				?: throw MangaNotFoundException("Cannot find manga by id")
			mangaData.value = manga
			loadDetails(manga, true)
		}
	}

	fun loadDetails(manga: Manga, force: Boolean = false) {
		if (!force && mangaData.value == manga) {
			return
		}
		loadHistory(manga)
		mangaData.value = manga
		loadFavourite(manga)
		launchLoadingJob {
			val data = withContext(Dispatchers.Default) {
				manga.source.repository.getDetails(manga)
			}
			mangaData.value = data
			newChapters.value = trackingRepository.getNewChaptersCount(manga.id)
		}
	}

	fun deleteLocal(manga: Manga) {
		launchLoadingJob {
			withContext(Dispatchers.Default) {
				val original = localMangaRepository.getRemoteManga(manga)
				localMangaRepository.delete(manga) || throw IOException("Unable to delete file")
				safe {
					historyRepository.deleteOrSwap(manga, original)
				}
			}
			onMangaRemoved.call(manga)
		}
	}

	private fun loadHistory(manga: Manga) {
		launchJob {
			history.value = historyRepository.getOne(manga)
		}
	}

	private fun loadFavourite(manga: Manga) {
		launchJob {
			favouriteCategories.value = favouritesRepository.getCategories(manga.id)
		}
	}

	override fun onHistoryChanged() {
		loadHistory(mangaData.value ?: return)
	}

	override fun onFavouritesChanged(mangaId: Long) {
		val manga = mangaData.value ?: return
		if (mangaId == manga.id) {
			loadFavourite(manga)
		}
	}

	override fun onCleared() {
		HistoryRepository.unsubscribe(this)
		FavouritesRepository.unsubscribe(this)
		super.onCleared()
	}
}