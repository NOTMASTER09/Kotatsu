package org.koitharu.kotatsu.remotelist.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.base.ui.list.ListSelectionController
import org.koitharu.kotatsu.list.ui.MangaListFragment
import org.koitharu.kotatsu.list.ui.filter.FilterBottomSheet
import org.koitharu.kotatsu.main.ui.owners.AppBarOwner
import org.koitharu.kotatsu.parsers.model.MangaSource
import org.koitharu.kotatsu.search.ui.SearchActivity
import org.koitharu.kotatsu.settings.SettingsActivity
import org.koitharu.kotatsu.utils.ext.addMenuProvider
import org.koitharu.kotatsu.utils.ext.assistedViewModels
import org.koitharu.kotatsu.utils.ext.serializableArgument
import org.koitharu.kotatsu.utils.ext.withArgs

@AndroidEntryPoint
class RemoteListFragment : MangaListFragment() {

	@Inject
	lateinit var viewModelFactory: RemoteListViewModel.Factory

	public override val viewModel by assistedViewModels {
		viewModelFactory.create(source)
	}

	private val source by serializableArgument<MangaSource>(ARG_SOURCE)

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		addMenuProvider(RemoteListMenuProvider())
	}

	override fun onScrolledToEnd() {
		viewModel.loadNextPage()
	}

	override fun onCreateActionMode(controller: ListSelectionController, mode: ActionMode, menu: Menu): Boolean {
		mode.menuInflater.inflate(R.menu.mode_remote, menu)
		return super.onCreateActionMode(controller, mode, menu)
	}

	override fun onFilterClick(view: View?) {
		FilterBottomSheet.show(childFragmentManager)
	}

	override fun onEmptyActionClick() {
		viewModel.resetFilter()
	}

	private inner class RemoteListMenuProvider :
		MenuProvider,
		SearchView.OnQueryTextListener,
		MenuItem.OnActionExpandListener {

		override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
			menuInflater.inflate(R.menu.opt_list_remote, menu)
			val searchMenuItem = menu.findItem(R.id.action_search)
			searchMenuItem.setOnActionExpandListener(this)
			val searchView = searchMenuItem.actionView as SearchView
			searchView.setOnQueryTextListener(this)
			searchView.setIconifiedByDefault(false)
			searchView.queryHint = searchMenuItem.title
		}

		override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
			R.id.action_source_settings -> {
				startActivity(SettingsActivity.newSourceSettingsIntent(requireContext(), source))
				true
			}
			R.id.action_filter -> {
				onFilterClick(null)
				true
			}
			else -> false
		}

		override fun onQueryTextSubmit(query: String?): Boolean {
			if (query.isNullOrEmpty()) {
				return false
			}
			val intent = SearchActivity.newIntent(
				context = this@RemoteListFragment.context ?: return false,
				source = source,
				query = query,
			)
			startActivity(intent)
			return true
		}

		override fun onQueryTextChange(newText: String?): Boolean = false

		override fun onMenuItemActionExpand(item: MenuItem): Boolean {
			(activity as? AppBarOwner)?.appBar?.setExpanded(false, true)
			return true
		}

		override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
			val searchView = (item.actionView as? SearchView) ?: return false
			searchView.setQuery("", false)
			return true
		}
	}

	companion object {

		private const val ARG_SOURCE = "provider"

		fun newInstance(provider: MangaSource) = RemoteListFragment().withArgs(1) {
			putSerializable(ARG_SOURCE, provider)
		}
	}
}
