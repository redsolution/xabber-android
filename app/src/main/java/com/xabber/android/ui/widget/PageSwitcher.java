/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 * 
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 * 
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.ui.widget;

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.Adapter;
import android.widget.ListView;
import android.widget.Scroller;

import com.xabber.android.data.LogManager;
import com.xabber.android.ui.adapter.SaveStateAdapter;
import com.xabber.androiddev.R;

/**
 * Widget to switch between chat pages.
 * 
 * Warning: This class is to be replaced.
 * 
 * @author alexander.ivanov
 * 
 */
public class PageSwitcher extends ViewGroup implements AnimationListener {

	public static final boolean LOG = false;

	/**
	 * Delay before hide pages.
	 */
	private static final long PAGES_HIDDER_DELAY = 1000;

	/**
	 * Distance a touch can wander before we think the user is scrolling.
	 */
	private final int touchSlop;

	private final DataSetObserver dataSetObserver = new DataSetObserver() {
		@Override
		public void onChanged() {
			dataChanged = true;
			requestLayout();
		}

		@Override
		public void onInvalidated() {
			dataChanged = true;
			requestLayout();
		}
	};

	private boolean dataChanged;

	private SaveStateAdapter adapter;

	private int widthMeasureSpec;

	private int heightMeasureSpec;

	/**
	 * User is currently dragging this view.
	 */
	private boolean isBeingDragged;

	/**
	 * Drag was interrupted.
	 */
	private boolean dragWasCanceled;

	/**
	 * Position of down touch.
	 */
	private float touchX;

	/**
	 * Initial scroll position.
	 */
	private int initialScrollX;

	/**
	 * {@link Scroller#isFinished()} is <code>false</code> when being flung.
	 */
	private final Scroller scroller;

	/**
	 * The listener that receives notifications when an item is selected,
	 * unselected or removed.
	 */
	private OnSelectListener onSelectListener;

	/**
	 * Selected position in adapter. Can be incorrect while {@link #dataChanged}
	 * is <code>true</code> .
	 */
	private int selectedPosition;

	private View selectedView;

	/**
	 * Previous selected object (before data changes).
	 */
	private Object previousSelectedObject;

	/**
	 * Visible but not selected position.
	 */
	private int visiblePosition;

	/**
	 * Visible but not selected view.
	 */
	private View visibleView;

	/**
	 * Previous visible object (before data changes).
	 */
	private Object previousVisibleObject;

	/**
	 * Animation used to hide pages.
	 */
	private final Animation pagesHideAnimation;

	/**
	 * Runnable called to hide pages.
	 */
	private final Runnable pagesHideRunnable = new Runnable() {
		@Override
		public void run() {
			if (LOG)
				LogManager.i(this, "hide pages");
			handler.removeCallbacks(this);
			if (selectedView != null)
				selectedView.findViewById(R.id.chat_page).startAnimation(
						pagesHideAnimation);
			if (visibleView != null) {
				visibleView.findViewById(R.id.chat_page).setVisibility(
						View.GONE);
				visibleView.findViewById(R.id.chat_page).clearAnimation();
			}
		}
	};

	/**
	 * Whether pages are shown.
	 */
	private boolean pagesShown;

	private final Handler handler;

	public PageSwitcher(Context context, AttributeSet attrs) {
		super(context, attrs);
		touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
		dataChanged = true;
		adapter = null;
		widthMeasureSpec = 0;
		heightMeasureSpec = 0;
		isBeingDragged = false;
		dragWasCanceled = false;
		touchX = 0;
		initialScrollX = 0;
		scroller = new Scroller(getContext());
		onSelectListener = null;

		selectedPosition = 0;
		selectedView = null;
		previousSelectedObject = null;
		visiblePosition = 0;
		visibleView = null;
		previousVisibleObject = null;

		handler = new Handler();
		pagesHideAnimation = AnimationUtils.loadAnimation(context,
				R.anim.chat_page_out);
		pagesHideAnimation.setAnimationListener(this);
		pagesShown = false;
	}

	/**
	 * Sets adapter. Request update and set initial selected position.
	 * 
	 * @param adapter
	 */
	public void setAdapter(SaveStateAdapter adapter) {
		if (adapter == null)
			throw new IllegalStateException();
		if (this.adapter != null)
			this.adapter.unregisterDataSetObserver(dataSetObserver);
		this.adapter = adapter;
		this.adapter.registerDataSetObserver(dataSetObserver);
		// dataChanged will be set in setSelection().
		setSelection(0);
	}

	/**
	 * @return Assigned adapter.
	 */
	public Adapter getAdapter() {
		return adapter;
	}

	/**
	 * Register a callback to be invoked when an item in this View has been
	 * selected, unselected or removed.
	 * 
	 * @param listener
	 *            The callback that will run
	 */
	public void setOnSelectListener(OnSelectListener listener) {
		onSelectListener = listener;
	}

	public OnSelectListener getOnSelectListener() {
		return onSelectListener;
	}

	/**
	 * @return Selected position in adapter.
	 */
	public int getSelectedItemPosition() {
		if (adapter == null)
			throw new IllegalStateException();
		return selectedPosition;
	}

	/**
	 * @return Selected item in adapter or <code>null</code> if there is no
	 *         elements.
	 */
	public Object getSelectedItem() {
		if (adapter == null)
			throw new IllegalStateException();
		if (selectedPosition < 0 || selectedPosition >= adapter.getCount())
			return null;
		else
			return adapter.getItem(selectedPosition);
	}

	/**
	 * @return Visible item from adapter or <code>null</code> if there is no
	 *         visible elements (selected element still can exists).
	 */
	public Object getVisibleItem() {
		if (adapter == null)
			throw new IllegalStateException();
		if (visiblePosition < 0 || visiblePosition >= adapter.getCount())
			return null;
		else
			return adapter.getItem(visiblePosition);
	}

	/**
	 * @return Selected view.
	 */
	public View getSelectedView() {
		if (adapter == null)
			throw new IllegalStateException();
		return selectedView;
	}

	/**
	 * @return Visible view.
	 */
	public View getVisibleView() {
		if (adapter == null)
			throw new IllegalStateException();
		return visibleView;
	}

	/**
	 * @return number of items or zero if adapter is <code>null</code> .
	 */
	private int getCount() {
		return adapter == null ? 0 : adapter.getCount();
	}

	/**
	 * Returns correct position.
	 * 
	 * @param position
	 * @return value between <code>0</code> and <code>count - 1</code>.
	 */
	private int correntPosition(int position) {
		final int count = getCount();
		if (position >= count)
			return 0;
		if (position < 0)
			return count - 1;
		return position;
	}

	/**
	 * Gets view.
	 * 
	 * @param position
	 *            in adapter.
	 * @param x
	 *            position in layout.
	 * @param convertView
	 *            previous view.
	 * @param update
	 *            whether we need force update underlying view.
	 * @param layout
	 *            whether we need to update layout (remeasure).
	 * @return
	 */
	private View getView(int position, int x, View convertView, boolean update,
			boolean layout) {
		final View view;
		if (convertView == null) {
			if (LOG)
				LogManager.i(this, "new view");
			view = adapter.getView(position, null, this);
		} else if (update) {
			if (LOG)
				LogManager.i(this, "update view");
			view = adapter.getView(position, convertView, this);
		} else {
			view = convertView;
		}
		if (view != convertView) {
			if (LOG)
				LogManager.i(this, "init view");
			LayoutParams layoutParams = view.getLayoutParams();
			if (layoutParams == null)
				layoutParams = new LayoutParams(LayoutParams.FILL_PARENT,
						LayoutParams.FILL_PARENT);
			addViewInLayout(view, 0, layoutParams, true);
		}
		if (update || layout || view.getLeft() != x) {
			if (LOG)
				LogManager.i(this, "layout view");
			// We must measure ListView after update to show items.
			measureChild(view, widthMeasureSpec, heightMeasureSpec);
			view.layout(x, 0, x + view.getMeasuredWidth(),
					view.getMeasuredHeight());
		}
		return view;
	}

	/**
	 * Updates scrolling, creates views if necessary .
	 * 
	 * @param layout
	 *            whether we need to update layout (remeasure).
	 */
	private void update(boolean layout) {
		// Process data change.
		final int count = getCount();
		if (dataChanged) {
			if (previousSelectedObject != null)
				for (int position = 0; position < count; position++)
					if (adapter.getItem(position)
							.equals(previousSelectedObject)) {
						selectedPosition = position;
						if (LOG)
							LogManager.i(this, "Found selected position: "
									+ selectedPosition);
						break;
					}
			selectedPosition = correntPosition(selectedPosition);
		}

		// Process scrolling.
		final int width = getWidth();
		int scrollX = getScrollX();
		if (width != 0) {
			while (scrollX >= width) {
				scrollX -= width;
				initialScrollX -= width;
				selectedPosition = correntPosition(selectedPosition + 1);
				if (LOG)
					LogManager.i(this, "scrollX >= width: " + selectedPosition);
			}
			while (scrollX <= -width) {
				scrollX += width;
				initialScrollX += width;
				selectedPosition = correntPosition(selectedPosition - 1);
				if (LOG)
					LogManager
							.i(this, "scrollX <= -width: " + selectedPosition);
			}
		}

		// Process low count.
		if (count < 2) {
			if (LOG)
				LogManager.i(this, "count < 2");
			dragWasCanceled = true;
			isBeingDragged = false;
			if (!scroller.isFinished())
				scroller.abortAnimation();
			if (scrollX != 0)
				scrollX = 0;
		}

		// Store focus.
		final View focus;
		if (selectedView != null)
			focus = selectedView.findFocus();
		else
			focus = null;

		// Process selected view.
		if (count == 0) {
			if (LOG)
				LogManager.i(this, "count == 0");
			selectedPosition = -1;
			if (selectedView != null) {
				if (onSelectListener != null)
					onSelectListener.onUnselect();
				adapter.saveState(selectedView);
				removeViewInLayout(selectedView);
				selectedView = null;
				// We must invalidate to update view.
				invalidate();
			}
		} else {
			if (LOG)
				LogManager.i(this, "count > 0");

			// Exchange visible and selected views and previous objects.
			final Object selectedObject = adapter.getItem(selectedPosition);
			final boolean exchange = previousSelectedObject != null
					&& previousVisibleObject != null
					&& !previousSelectedObject.equals(selectedObject)
					&& previousVisibleObject.equals(selectedObject);
			if (exchange) {
				Object tempObject = previousSelectedObject;
				previousSelectedObject = previousVisibleObject;
				previousVisibleObject = tempObject;
				View view = selectedView;
				selectedView = visibleView;
				visibleView = view;
			}

			// Update view.
			final boolean update = dataChanged
					|| previousSelectedObject == null
					|| !previousSelectedObject.equals(selectedObject);
			selectedView = getView(selectedPosition, 0, selectedView, update,
					layout);
			previousSelectedObject = selectedObject;
			if (update || exchange)
				if (onSelectListener != null)
					onSelectListener.onSelect();

			// Enable focusable.
			if (selectedView instanceof ViewGroup)
				((ViewGroup) selectedView)
						.setDescendantFocusability(ViewGroup.FOCUS_AFTER_DESCENDANTS);
			else
				selectedView.setFocusable(true);
		}

		// Process visible (not selected) view.
		if (count < 2) {
			if (LOG)
				LogManager.i(this, "count < 2 || scrollX == 0");
			visiblePosition = -1;
			if (visibleView != null) {
				adapter.saveState(visibleView);
				removeViewInLayout(visibleView);
				visibleView = null;
			}
		} else {
			// Calculate position.
			final int visibleX;
			if (scrollX > 0) {
				if (LOG)
					LogManager.i(this, "scrollX > 0");
				visiblePosition = correntPosition(selectedPosition + 1);
				visibleX = width;
			} else {
				if (LOG)
					LogManager.i(this, "scrollX < 0");
				visiblePosition = correntPosition(selectedPosition - 1);
				visibleX = -width;
			}

			// Update view.
			final Object visibleObject = adapter.getItem(visiblePosition);
			final boolean update = dataChanged || previousVisibleObject == null
					|| !previousVisibleObject.equals(visibleObject);
			visibleView = getView(visiblePosition, visibleX, visibleView,
					update, layout);
			previousVisibleObject = visibleObject;

			// Disable focusable.
			if (visibleView instanceof ViewGroup)
				((ViewGroup) visibleView)
						.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
			else
				visibleView.setFocusable(false);
		}

		// Restore focus by ID.
		if (selectedView != null) {
			View target;
			if (focus == null || focus.getId() == View.NO_ID)
				target = null;
			else
				target = selectedView.findViewById(focus.getId());
			if (target == null)
				target = selectedView.findViewById(R.id.chat_input);
			target.requestFocus();
		}

		if (scrollX == 0) {
			if (LOG)
				LogManager.i(this, "Scroll X == 0");
			hidePages();
		} else {
			if (LOG)
				LogManager.i(this, "Scroll X != 0");
			showPages();
		}

		super.scrollTo(scrollX, 0);

		dataChanged = false;
	}

	/**
	 * Show pages.
	 */
	private void showPages() {
		if (pagesShown)
			return;
		pagesShown = true;
		handler.removeCallbacks(pagesHideRunnable);
		if (selectedView != null) {
			selectedView.findViewById(R.id.chat_page).clearAnimation();
			selectedView.findViewById(R.id.chat_page).setVisibility(
					View.VISIBLE);
		}
		if (visibleView != null) {
			visibleView.findViewById(R.id.chat_page).clearAnimation();
			visibleView.findViewById(R.id.chat_page)
					.setVisibility(View.VISIBLE);
		}
	}

	/**
	 * Requests pages to be hiden in future.
	 */
	private void hidePages() {
		if (!pagesShown)
			return;
		pagesShown = false;
		handler.postDelayed(pagesHideRunnable, PAGES_HIDDER_DELAY);
	}

	/**
	 * Save state of views. Must be called on activity pause.
	 */
	public void saveState() {
		if (visibleView != null)
			adapter.saveState(visibleView);
		if (selectedView != null)
			adapter.saveState(selectedView);
	}

	/**
	 * Selects an item. Immediately scroll to this position.
	 * 
	 * @param position
	 */
	public void setSelection(int position) {
		if (adapter == null)
			throw new IllegalStateException();
		dataChanged = true;
		isBeingDragged = false;
		dragWasCanceled = true;
		if (!scroller.isFinished())
			scroller.abortAnimation();
		if (getScrollX() != 0 || getScrollY() != 0)
			super.scrollTo(0, 0);
		previousSelectedObject = null;
		selectedPosition = position;
		if (LOG)
			LogManager.i(this, "setSelection: " + selectedPosition);
		update(false);
		if (selectedView == null)
			return;
		ListView listView = (ListView) selectedView
				.findViewById(android.R.id.list);
		listView.setAdapter(listView.getAdapter());
	}

	/**
	 * Stop any movements.
	 */
	public void stopMovement() {
		isBeingDragged = false;
		dragWasCanceled = true;
		if (!scroller.isFinished())
			scroller.abortAnimation();
		if (getScrollX() != 0 || getScrollY() != 0)
			super.scrollTo(0, 0);
		update(false);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
		this.widthMeasureSpec = widthMeasureSpec;
		this.heightMeasureSpec = heightMeasureSpec;
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right,
			int bottom) {
		if (LOG)
			LogManager.i(this, "onLayout");
		update(true);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event) {
		if (getCount() <= 1) {
			isBeingDragged = false;
			dragWasCanceled = true;
			return false;
		}

		final int action = event.getAction();
		if (action == MotionEvent.ACTION_DOWN)
			dragWasCanceled = false;
		if (dragWasCanceled)
			return false;

		final float x = event.getX();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			touchX = x;
			initialScrollX = getScrollX();
			if (!scroller.isFinished()) {
				scroller.abortAnimation();
				isBeingDragged = true;
			} else {
				isBeingDragged = false;
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (Math.abs(x - touchX) > touchSlop)
				isBeingDragged = true;
			// requestDisallowInterceptTouchEvent(true);
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			break;
		}
		return isBeingDragged;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (getCount() <= 1)
			dragWasCanceled = true;
		if (dragWasCanceled)
			return false;

		final int action = event.getAction();
		final float x = event.getX();
		switch (action) {
		case MotionEvent.ACTION_DOWN:
			break;
		case MotionEvent.ACTION_MOVE:
			if (Math.abs(x - touchX) > touchSlop)
				isBeingDragged = true;
			if (isBeingDragged) {
				if (LOG)
					LogManager.i(this, "onTouchEvent - MOVE");
				scrollTo((int) (touchX - x) + initialScrollX, 0);
			}
			break;
		case MotionEvent.ACTION_CANCEL:
		case MotionEvent.ACTION_UP:
			if (isBeingDragged) {
				final int scroll = getScrollX();
				final int width = getWidth();
				final int center = width / 2;
				final int target;
				if (scroll > center) {
					target = width;
				} else if (scroll < -center) {
					target = -width;
				} else {
					target = 0;
				}
				scroller.startScroll(scroll, 0, target - scroll, 0);
				invalidate();
			}
			isBeingDragged = false;
			break;
		}
		return true;
	}

	@Override
	public void computeScroll() {
		if (scroller.computeScrollOffset()) {
			if (scroller.getCurrX() == scroller.getFinalX()
					&& scroller.getCurrY() == scroller.getFinalY())
				scroller.abortAnimation();
			if (LOG)
				LogManager.i(this, "computeScroll");
			scrollTo(scroller.getCurrX(), scroller.getCurrY());
			postInvalidate();
		}
	}

	@Override
	public void scrollTo(int x, int y) {
		if (LOG)
			LogManager.i(this, "scrollTo: " + x + "," + y);
		super.scrollTo(x, y);
		update(false);
	}

	@Override
	public void onAnimationStart(Animation animation) {
	}

	@Override
	public void onAnimationEnd(Animation animation) {
		if (selectedView != null)
			selectedView.findViewById(R.id.chat_page).setVisibility(View.GONE);
	}

	@Override
	public void onAnimationRepeat(Animation animation) {
	}

	public interface OnSelectListener {

		/**
		 * Callback method to be invoked when an item has been selected.
		 */
		void onSelect();

		/**
		 * Callback method to be invoked when an item has been unselected.
		 */
		void onUnselect();

	}
}
