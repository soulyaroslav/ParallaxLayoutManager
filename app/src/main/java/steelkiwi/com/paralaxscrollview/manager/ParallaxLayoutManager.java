package steelkiwi.com.paralaxscrollview.manager;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Toast;

/**
 * Created by yaroslav on 6/1/17.
 */

public class ParallaxLayoutManager extends RecyclerView.LayoutManager {

    private static final float VIEW_HEIGHT_PERCENT = .8f;
    private SparseArray<View> viewCache = new SparseArray<>();
    private int position = 1;

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        );
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        detachAndScrapAttachedViews(recycler);
        fill(recycler);
    }

    private void fill(RecyclerView.Recycler recycler) {
        View anchorView = getChildAt(1) /*getAnchorView()*/;
        viewCache.clear();

        for (int i = 0; i < getChildCount(); i++) {
            View view = getChildAt(i);
            int position = getPosition(view);
            viewCache.put(position, view);
        }

        for (int i = 0; i < viewCache.size(); i++) {
            detachView(viewCache.valueAt(i));
        }

        fillUp(anchorView, recycler);
        fillDown(anchorView, recycler);

        for (int i = 0; i < viewCache.size(); i++) {
            recycler.recycleView(viewCache.valueAt(i));
        }
    }

    private void fillUp(View anchorView, RecyclerView.Recycler recycler) {
        int anchorPos = 0;
        int anchorTop = 0;
        if (anchorView != null) {
            anchorPos = getPosition(anchorView);
            anchorTop = getDecoratedTop(anchorView);
        }

        boolean fillUp = true;
        int position = anchorPos - 1;
        int viewBottom = anchorTop;
        int viewHeight = (int) (getHeight() * VIEW_HEIGHT_PERCENT);
        final int widthSpec = View.MeasureSpec.makeMeasureSpec(getWidth(), View.MeasureSpec.EXACTLY);
        final int heightSpec = View.MeasureSpec.makeMeasureSpec(viewHeight, View.MeasureSpec.EXACTLY);
        while (fillUp && position >= 0) {
            View view = viewCache.get(position);
            if (view == null) {
                // get new view from layout
                view = recycler.getViewForPosition(position);
                view.setScaleX(1f);
                view.setScaleY(1f);
                addView(view, 0);
                measureChildWithDecorationsAndMargin(view, widthSpec, heightSpec);
                int decoratedMeasuredWidth = getDecoratedMeasuredWidth(view);
                layoutDecorated(view, 0, viewBottom - viewHeight, decoratedMeasuredWidth, viewBottom);
            } else {
                // if view in catch simply attach it
                attachView(view);
                viewCache.remove(position);
            }
            viewBottom = getDecoratedTop(view);
            fillUp = (viewBottom > 0);
            position--;
        }
    }

    private void fillDown(View anchorView, RecyclerView.Recycler recycler) {
        int anchorPos = 0;
        int anchorTop = 0;
        if (anchorView != null) {
            anchorPos = getPosition(anchorView);
            anchorTop = getDecoratedTop(anchorView);
        }

        int position = anchorPos;
        boolean fillDown = true;
        int height = getHeight();
        int viewTop = anchorTop;
        int itemCount = getItemCount();
        int viewHeight = (int) (getHeight() * VIEW_HEIGHT_PERCENT);
        final int widthSpec = View.MeasureSpec.makeMeasureSpec(getWidth(), View.MeasureSpec.EXACTLY);
        final int heightSpec = View.MeasureSpec.makeMeasureSpec(viewHeight, View.MeasureSpec.EXACTLY);

        while (fillDown && position < itemCount) {
            View view = viewCache.get(position);
            if (view == null) {
                view = recycler.getViewForPosition(position);
                view.setScaleX(1f);
                view.setScaleY(1f);
                addView(view);
                measureChildWithDecorationsAndMargin(view, widthSpec, heightSpec);
                int decoratedMeasuredWidth = getDecoratedMeasuredWidth(view);
                layoutDecorated(view, 0, viewTop, decoratedMeasuredWidth, viewTop + viewHeight);
            } else {
                attachView(view);
                viewCache.remove(position);
            }
            viewTop = getDecoratedBottom(view);
            if (position > 0) {
                viewTop -= viewHeight * .75f;
            } else {
                viewTop -= viewHeight * .2f;
            }
            fillDown = viewTop <= height;
            position++;
        }
    }

    private void measureChildWithDecorationsAndMargin(View child, int widthSpec, int heightSpec) {
        Rect decorationRect = new Rect();
        calculateItemDecorationsForChild(child, decorationRect);
        RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();
        widthSpec = updateSpecWithExtra(widthSpec, layoutParams.leftMargin + decorationRect.left,
                layoutParams.rightMargin + decorationRect.right);
        heightSpec = updateSpecWithExtra(heightSpec, layoutParams.topMargin + decorationRect.top,
                layoutParams.bottomMargin + decorationRect.bottom);
        child.measure(widthSpec, heightSpec);
    }

    private int updateSpecWithExtra(int spec, int startInset, int endInset) {
        if (startInset == 0 && endInset == 0) {
            return spec;
        }
        final int mode = View.MeasureSpec.getMode(spec);
        if (mode == View.MeasureSpec.AT_MOST || mode == View.MeasureSpec.EXACTLY) {
            return View.MeasureSpec.makeMeasureSpec(
                    View.MeasureSpec.getSize(spec) - startInset - endInset, mode);
        }
        return spec;
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        return scroll(dy, recycler);
    }

    private int scroll(int dy, RecyclerView.Recycler recycler) {
        int delta = scrollVerticallyInternal(dy) / 2;
        offsetChildrenVertical(-delta);
        translateCurrentView(dy);
        fill(recycler);
        return delta;
    }

    private void translateCurrentView(int dy) {
        int delta = scrollVerticallyInternal(dy);
        View view = getChildAt(position);
        if (dy < 0) { // content going to down
            if (isBottomIntersect(view)) {
                if (position > 0) {
                    position--;
                }
            }
        } else if (dy > 0) { // content going to top
            if (isTopIntersect(view)) {
                if (position < getChildCount() - 1) {
                    position++;
                }
            }
        }
        if (position > 0 && position < getChildCount() - 1) {
            view.setTranslationY(-delta + view.getTranslationY());
        }
        if (position == 0) {
            position = 1;
        }
    }

    private int scrollVerticallyInternal(int dy) {
        int childCount = getChildCount();
        int itemCount = getItemCount();
        if (childCount == 0) {
            return 0;
        }

        final View topView = getChildAt(0);
        final View bottomView = getChildAt(childCount - 1);
        // case when all view layout on screen
        int viewSpan = getDecoratedBottom(bottomView) - getDecoratedTop(topView);
        if (viewSpan <= getHeight()) {
            return 0;
        }

        int delta = 0;

        if (dy < 0) { // content going to down
            View firstView = getChildAt(0);
            int firstViewAdapterPosition = getPosition(firstView);
            if (firstViewAdapterPosition > 0) {
                delta = dy;
            } else {
                int viewTop = getDecoratedTop(firstView);
                delta = Math.max(viewTop, dy);
            }
        } else if (dy > 0) { // content going to top
            View lastView = getChildAt(childCount - 1);
            int lastViewAdapterPosition = getPosition(lastView);
            if (lastViewAdapterPosition < itemCount - 1) {
                delta = dy;
            } else {
                int viewBottom = getDecoratedBottom(lastView);
                int parentBottom = getHeight();
                delta = Math.min(viewBottom - parentBottom, dy);
            }
        }
        return delta;
    }

    // check is current view inside top rectangle
    private boolean isTopIntersect(View currentView) {
        return getTopBounds().contains((int) currentView.getX(), (int) currentView.getY());
    }

    // check is current view inside bottom rectangle
    private boolean isBottomIntersect(View currentView) {
        return getBottomBounds().contains((int) currentView.getX(), (int) currentView.getY());
    }

    // get invisible rectangle in the top of the screen
    private Rect getTopBounds() {
        return new Rect(0, -getHeight(), getWidth(), 40);
    }

    // get invisible rectangle in the bottom of the screen
    private Rect getBottomBounds() {
        return new Rect(0, getHeight() - (int)(getHeight() * .4f), getWidth(), getHeight() + getHeight());
    }

    public class ResetAnimimation extends Animation {
        int targetHeight;
        int originalHeight;
        int extraHeight;
        View mView;

        protected ResetAnimimation(View view, int targetHeight) {
            this.mView = view;
            this.targetHeight = targetHeight;
            originalHeight = view.getHeight();
            extraHeight = this.targetHeight - originalHeight;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {

            int newHeight;
            newHeight = (int) (targetHeight - extraHeight * (1 - interpolatedTime));
            mView.getLayoutParams().height = newHeight;
            mView.requestLayout();
        }
    }
}
