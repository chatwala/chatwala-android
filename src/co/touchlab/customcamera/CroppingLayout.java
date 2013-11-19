package co.touchlab.customcamera;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsoluteLayout;

/**
 * Created with IntelliJ IDEA.
 * User: kgalligan
 * Date: 11/15/13
 * Time: 2:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class CroppingLayout extends ViewGroup
{
    public CroppingLayout(Context context)
    {
        super(context);
    }

    public CroppingLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public CroppingLayout(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
    }

    @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int count = getChildCount();

            int maxHeight = 0;
            int maxWidth = 0;

            // Find out how big everyone wants to be
            measureChildren(widthMeasureSpec, heightMeasureSpec);

            //Just measure the children, we don't want to use them to determine max
            // Find rightmost and bottom-most child
//            for (int i = 0; i < count; i++) {
//                View child = getChildAt(i);
//                if (child.getVisibility() != GONE) {
//                    int childRight;
//                    int childBottom;
//
//                    AbsoluteLayout.LayoutParams lp
//                            = (AbsoluteLayout.LayoutParams) child.getLayoutParams();
//
//                    childRight = lp.x + child.getMeasuredWidth();
//                    childBottom = lp.y + child.getMeasuredHeight();
//
//                    maxWidth = Math.max(maxWidth, childRight);
//                    maxHeight = Math.max(maxHeight, childBottom);
//                }
//            }

            // Account for padding too
//            maxWidth += mPaddingLeft + mPaddingRight;
//            maxHeight += mPaddingTop + mPaddingBottom;

            // Check against minimum height and width
            maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
            maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

            setMeasuredDimension(resolveSize(maxWidth, widthMeasureSpec),
                    resolveSize(maxHeight, heightMeasureSpec));
        }

        /**
         * Returns a set of layout parameters with a width of
         * {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT},
         * a height of {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT}
         * and with the coordinates (0, 0).
         */
        @Override
        protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
            return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0, 0);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t,
                int r, int b) {
            int count = getChildCount();

            for (int i = 0; i < count; i++) {
                View child = getChildAt(i);
                if (child.getVisibility() != GONE) {

                    AbsoluteLayout.LayoutParams lp =
                            (AbsoluteLayout.LayoutParams) child.getLayoutParams();

                    int viewWidth = getMeasuredWidth();
                    int viewHeight = getMeasuredHeight();

                    int childLeft = -((child.getMeasuredWidth()/2) - (viewWidth/2));
                    int childTop = -((child.getMeasuredHeight()/2) - (viewHeight/2));
                    child.layout(childLeft, childTop,
                            childLeft + child.getMeasuredWidth(),
                            childTop + child.getMeasuredHeight());

                }
            }
        }

        @Override
        public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
            return new AbsoluteLayout.LayoutParams(getContext(), attrs);
        }

        // Override to allow type-checking of LayoutParams.
        @Override
        protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
            return p instanceof AbsoluteLayout.LayoutParams;
        }

        @Override
        protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
            return new LayoutParams(p);
        }

        /**
         * Per-child layout information associated with AbsoluteLayout.
         * See
         * {@link android.R.styleable#AbsoluteLayout_Layout Absolute Layout Attributes}
         * for a list of all child view attributes that this class supports.
         */
        public static class LayoutParams extends ViewGroup.LayoutParams {
            /**
             * The horizontal, or X, location of the child within the view group.
             */
            public int x;
            /**
             * The vertical, or Y, location of the child within the view group.
             */
            public int y;

            /**
             * Creates a new set of layout parameters with the specified width,
             * height and location.
             *
             * @param width the width, either {@link #FILL_PARENT},
                      {@link #WRAP_CONTENT} or a fixed size in pixels
             * @param height the height, either {@link #FILL_PARENT},
                      {@link #WRAP_CONTENT} or a fixed size in pixels
             * @param x the X location of the child
             * @param y the Y location of the child
             */
            public LayoutParams(int width, int height, int x, int y) {
                super(width, height);
                this.x = x;
                this.y = y;
            }

            /**
             * Creates a new set of layout parameters. The values are extracted from
             * the supplied attributes set and context. The XML attributes mapped
             * to this set of layout parameters are:
             *
             * <ul>
             *   <li><code>layout_x</code>: the X location of the child</li>
             *   <li><code>layout_y</code>: the Y location of the child</li>
             *   <li>All the XML attributes from
             *   {@link android.view.ViewGroup.LayoutParams}</li>
             * </ul>
             *
             * @param c the application environment
             * @param attrs the set of attributes fom which to extract the layout
             *              parameters values
             */
            public LayoutParams(Context c, AttributeSet attrs) {
                super(c, attrs);
                /*TypedArray a = c.obtainStyledAttributes(attrs,
                        com.android.internal.R.styleable.AbsoluteLayout_Layout);
                x = a.getDimensionPixelOffset(
                        com.android.internal.R.styleable.AbsoluteLayout_Layout_layout_x, 0);
                y = a.getDimensionPixelOffset(
                        com.android.internal.R.styleable.AbsoluteLayout_Layout_layout_y, 0);
                a.recycle();*/
            }

            /**
             * {@inheritDoc}
             */
            public LayoutParams(ViewGroup.LayoutParams source) {
                super(source);
            }

            public String debug(String output) {
                return "asdf"; /*output + "Absolute.LayoutParams={width="
                        + sizeToString(width) + ", height=" + sizeToString(height)
                        + " x=" + x + " y=" + y + "}";*/
            }
        }
}
