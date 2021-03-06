/*
 * NewsCatchr  Copyright (C) 2016  Jan-Lukas Else
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package jlelse.newscatchr.extensions

import android.content.Context
import android.view.LayoutInflater
import android.widget.AutoCompleteTextView
import com.afollestad.materialdialogs.MaterialDialog
import com.mcxiaoke.koi.async.asyncSafe
import com.mcxiaoke.koi.async.mainThreadSafe
import com.mcxiaoke.koi.ext.find
import jlelse.newscatchr.backend.Article
import jlelse.newscatchr.backend.Feed
import jlelse.newscatchr.backend.apis.AutoCompleteAdapter
import jlelse.newscatchr.backend.apis.Feedly
import jlelse.newscatchr.ui.fragments.BaseFragment
import jlelse.newscatchr.ui.fragments.FeedListFragment
import jlelse.newscatchr.ui.views.ProgressDialog
import jlelse.readit.R

fun Array<out Article?>.removeEmptyArticles(): Array<Article> {
	return mutableListOf<Article>().apply {
		this@removeEmptyArticles.forEach { if (it.notNullOrEmpty()) add(it!!) }
	}.toTypedArray()
}

fun Array<out Feed?>.removeEmptyFeeds(): Array<Feed> {
	return mutableListOf<Feed>().apply {
		this@removeEmptyFeeds.forEach { if (it.notNullOrEmpty()) add(it!!) }
	}.toTypedArray()
}

fun Array<Feed>.onlySaved(): Array<Feed> {
	return toMutableList().filter(Feed::saved).toTypedArray()
}

fun Feed?.notNullOrEmpty(): Boolean {
	return this?.url().notNullOrBlank()
}

fun Article?.notNullOrEmpty(): Boolean {
	return this?.process()?.url.notNullOrBlank()
}

fun searchForFeeds(context: Context, fragmentNavigation: BaseFragment.FragmentNavigation, query: String? = null) {
	val progressDialog = ProgressDialog(context)
	val load = { finalQuery: String ->
		progressDialog.show()
		context.asyncSafe {
			var foundFeeds: Array<Feed>? = null
			var foundRelated: Array<String>? = null
			Feedly().feedSearch(finalQuery.toString(), 100, null, null) { feeds, related ->
				foundFeeds = feeds
				foundRelated = related
			}
			mainThreadSafe {
				progressDialog.dismiss()
				if (foundFeeds.notNullAndEmpty()) {
					fragmentNavigation.pushFragment(FeedListFragment().addObject(foundFeeds, "feeds").addObject(foundRelated, "tags"), "${R.string.search_results_for.resStr()} $finalQuery")
				} else context.nothingFound()
			}
		}
	}
	if (query.isNullOrBlank()) {
		val requestView = LayoutInflater.from(context).inflate(R.layout.searchdialog, null)
		val textView = requestView.find<AutoCompleteTextView>(R.id.autocompletetextview)
		textView.setAdapter(AutoCompleteAdapter(context))
		MaterialDialog.Builder(context)
				.title(android.R.string.search_go)
				.customView(requestView, true)
				.onPositive { materialDialog, dialogAction ->
					load(textView.text.toString())
				}
				.negativeText(android.R.string.cancel)
				.positiveText(android.R.string.search_go)
				.show()
	} else {
		load(query!!)
	}
}