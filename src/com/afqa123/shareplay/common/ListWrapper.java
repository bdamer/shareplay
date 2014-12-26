package com.afqa123.shareplay.common;

import android.database.Cursor;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

public class ListWrapper {

	private ListView listView;
	private String filter;
	private int index;
	private int pos;
	private boolean stale;
	private Cursor cursor;
	
	public ListWrapper(final View view, final OnItemClickListener clickListener, final OnItemLongClickListener longClickListener) {
		listView = (ListView)view;
		if (clickListener != null)
			listView.setOnItemClickListener(clickListener);
		if (longClickListener != null) 
			listView.setOnItemLongClickListener(longClickListener);
		stale = true;
	}
	
	public void clearFilters() {
		stale = true;
		listView.clearTextFilter();
	}

	public void setFilter(final String aFilter) {
		filter = aFilter;
	}
	
	public String getFilter() {
		String result = null;
		CharSequence tf = listView.getTextFilter();
		if (tf != null) {
			result = tf.toString();
		}
		return result;
	}
	
	public void setPosition(int index, int pos) {
		this.index = index;
		this.pos = pos;
	}
	
	public void restoreState() {
		listView.setFilterText(filter);
		listView.setSelectionFromTop(index, pos);
	}
	
	public void focus() {
		listView.requestFocus();
	}
	
	public ListView getView() {
		return listView;
	}
	
	public void setStale(boolean stale) {
		this.stale = stale;
	}
	
	public boolean isStale() {
		return stale;
	}
	
	public void setCursor(final Cursor c) {
		// close existing cursor
		releaseCursor();
		cursor = c;
	}
	
	public Cursor getCursor() {
		return cursor;
	}
	
	public void releaseCursor() {
		if (cursor != null && !cursor.isClosed()) {
			cursor.close();
		}		
		stale = true;
	}
}