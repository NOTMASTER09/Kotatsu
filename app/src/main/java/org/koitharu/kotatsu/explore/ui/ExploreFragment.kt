package org.koitharu.kotatsu.explore.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.graphics.Insets
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import coil.ImageLoader
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.domain.reverseAsync
import org.koitharu.kotatsu.base.ui.BaseFragment
import org.koitharu.kotatsu.base.ui.list.OnListItemClickListener
import org.koitharu.kotatsu.base.ui.util.RecyclerViewOwner
import org.koitharu.kotatsu.base.ui.util.ReversibleAction
import org.koitharu.kotatsu.bookmarks.ui.BookmarksActivity
import org.koitharu.kotatsu.databinding.FragmentExploreBinding
import org.koitharu.kotatsu.details.ui.DetailsActivity
import org.koitharu.kotatsu.explore.ui.adapter.ExploreAdapter
import org.koitharu.kotatsu.explore.ui.adapter.ExploreListEventListener
import org.koitharu.kotatsu.explore.ui.model.ExploreItem
import org.koitharu.kotatsu.favourites.ui.categories.FavouriteCategoriesActivity
import org.koitharu.kotatsu.history.ui.HistoryActivity
import org.koitharu.kotatsu.main.ui.owners.BottomNavOwner
import org.koitharu.kotatsu.parsers.model.Manga
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.search.ui.MangaListActivity
import org.koitharu.kotatsu.settings.SettingsActivity
import org.koitharu.kotatsu.suggestions.ui.SuggestionsActivity
import org.koitharu.kotatsu.utils.ext.getDisplayMessage
import javax.inject.Inject

@AndroidEntryPoint
class ExploreFragment :
	BaseFragment<FragmentExploreBinding>(),
	RecyclerViewOwner,
	ExploreListEventListener,
	OnListItemClickListener<ExploreItem.Source> {

	@Inject
	lateinit var coil: ImageLoader

	private val viewModel by viewModels<ExploreViewModel>()
	private var exploreAdapter: ExploreAdapter? = null
	private var paddingHorizontal = 0

	override val recyclerView: RecyclerView
		get() = binding.recyclerView

	override fun onInflateView(inflater: LayoutInflater, container: ViewGroup?): FragmentExploreBinding {
		return FragmentExploreBinding.inflate(inflater, container, false)
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		exploreAdapter = ExploreAdapter(coil, viewLifecycleOwner, this, this)
		with(binding.recyclerView) {
			adapter = exploreAdapter
			setHasFixedSize(true)
			val spacing = resources.getDimensionPixelOffset(R.dimen.list_spacing)
			paddingHorizontal = spacing
		}
		viewModel.content.observe(viewLifecycleOwner) {
			exploreAdapter?.items = it
		}
		viewModel.onError.observe(viewLifecycleOwner, ::onError)
		viewModel.onOpenManga.observe(viewLifecycleOwner, ::onOpenManga)
		viewModel.onActionDone.observe(viewLifecycleOwner, ::onActionDone)
	}

	override fun onDestroyView() {
		super.onDestroyView()
		exploreAdapter = null
	}

	override fun onWindowInsetsChanged(insets: Insets) {
		binding.recyclerView.updatePadding(
			bottom = insets.bottom,
		)
	}

	override fun onManageClick(view: View) {
		startActivity(SettingsActivity.newManageSourcesIntent(view.context))
	}

	override fun onClick(v: View) {
		val intent = when (v.id) {
			R.id.button_history -> HistoryActivity.newIntent(v.context)
			R.id.button_local -> MangaListActivity.newIntent(v.context, MangaSource.LOCAL)
			R.id.button_bookmarks -> BookmarksActivity.newIntent(v.context)
			R.id.button_suggestions -> SuggestionsActivity.newIntent(v.context)
			R.id.button_favourites -> FavouriteCategoriesActivity.newIntent(v.context)
			R.id.button_random -> {
				viewModel.openRandom()
				return
			}

			else -> return
		}
		startActivity(intent)
	}

	override fun onItemClick(item: ExploreItem.Source, view: View) {
		val intent = MangaListActivity.newIntent(view.context, item.source)
		startActivity(intent)
	}

	override fun onItemLongClick(item: ExploreItem.Source, view: View): Boolean {
		val menu = PopupMenu(view.context, view)
		menu.inflate(R.menu.popup_source)
		menu.setOnMenuItemClickListener(SourceMenuListener(item))
		menu.show()
		return true
	}

	override fun onRetryClick(error: Throwable) = Unit

	override fun onEmptyActionClick() = onManageClick(requireView())

	private fun onError(e: Throwable) {
		val snackbar = Snackbar.make(
			binding.recyclerView,
			e.getDisplayMessage(resources),
			Snackbar.LENGTH_SHORT,
		)
		snackbar.anchorView = (activity as? BottomNavOwner)?.bottomNav
		snackbar.show()
	}

	private fun onOpenManga(manga: Manga) {
		val intent = DetailsActivity.newIntent(context ?: return, manga)
		startActivity(intent)
	}

	private fun onActionDone(action: ReversibleAction) {
		val handle = action.handle
		val length = if (handle == null) Snackbar.LENGTH_SHORT else Snackbar.LENGTH_LONG
		val snackbar = Snackbar.make(binding.recyclerView, action.stringResId, length)
		if (handle != null) {
			snackbar.setAction(R.string.undo) { handle.reverseAsync() }
		}
		snackbar.anchorView = (activity as? BottomNavOwner)?.bottomNav
		snackbar.show()
	}

	private inner class SourceMenuListener(
		private val sourceItem: ExploreItem.Source,
	) : PopupMenu.OnMenuItemClickListener {

		override fun onMenuItemClick(item: MenuItem): Boolean {
			when (item.itemId) {
				R.id.action_settings -> {
					startActivity(SettingsActivity.newSourceSettingsIntent(requireContext(), sourceItem.source))
				}

				R.id.action_hide -> {
					viewModel.hideSource(sourceItem.source)
				}

				else -> return false
			}
			return true
		}
	}

	companion object {

		fun newInstance() = ExploreFragment()
	}
}
