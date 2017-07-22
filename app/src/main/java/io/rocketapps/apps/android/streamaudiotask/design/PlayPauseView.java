package io.rocketapps.apps.android.streamaudiotask.design;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.LinearLayout;

import android.view.GestureDetector.SimpleOnGestureListener;

/**
 * Created by macbook on 6/25/17.
 */

public class PlayPauseView extends View   implements View.OnClickListener, View.OnTouchListener {

    private static final int DEFAULT_CIRCLE_COLOR = Color.RED;
    private float START_ANGLE_POINT = 90;

    private int circleColor = DEFAULT_CIRCLE_COLOR;
    private Paint paint;
    private Paint paintFetching;
    private Paint trianglePaint;
    private Path trianglePath;
    private Rect rectangle;
    private Paint paintRect;

    private int Direction_SOUTH = 1;
    private int Direction_NORTH = 2;
    private int Direction_EAST = 3;
    private int Direction_WEST = 4;
    private int _radius;
    private int _cx, _cy;
    private Point _trianglePoint;
    private BUTTON_STATE mState = BUTTON_STATE.STATE_NORMAL;

    private int _preferedWidth = 0;
    private int _preferedWidthDP = 56;

    private int _preferedPadding = 0;
    private RotateAnimation rotateAnim;
    private Paint paintArch;
    private RectF rectArch;
    private int angleArch;
    private float oldXvalue = 0;
    private float oldYvalue = 0;
    private boolean isFetchingAnimRunning = false;


    private GestureDetector gestureDetector;
    private OnClickListener mListener;
    private MediaPlayer mMediaPlayer;
    private View mParentView;


    public enum BUTTON_STATE {
        STATE_PLAYING,
        STATE_PAUSE,
        STATE_FETCHING,
        STATE_NORMAL,
        STATE_COMPLETED
    }


    public PlayPauseView(Context context) {
        super(context);
        init(context, null);
    }

    public PlayPauseView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PlayPauseView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }


    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);



//        int w = getWidth();
//        int h = getHeight();
//
//        int pl = getPaddingLeft();
//        int pr = getPaddingRight();
//        int pt = getPaddingTop();
//        int pb = getPaddingBottom();
//
//        int usableWidth = w - (pl + pr);
//        int usableHeight = h - (pt + pb);
        int usableWidth = _preferedWidth;
        int usableHeight = _preferedWidth;

        _radius = Math.min(usableWidth, usableHeight) / 2;
        _cx =  (usableWidth / 2);
        _cy =  (usableHeight / 2);



        canvas.drawCircle(_cx, _cy, _radius, paint);

        if(getState() == BUTTON_STATE.STATE_NORMAL || getState() == BUTTON_STATE.STATE_PAUSE || getState() == BUTTON_STATE.STATE_COMPLETED){

            int triangleWidth = ( usableWidth / 2 ) - _preferedPadding ;

            _trianglePoint.x = _cx - (triangleWidth / 2) + _preferedPadding;
            _trianglePoint.y = _cy - (triangleWidth / 2);
            trianglePath = getEquilateralTriangle(_trianglePoint, triangleWidth, Direction_WEST);

            canvas.drawPath(trianglePath, trianglePaint);

            if(getState() == BUTTON_STATE.STATE_PAUSE){

                rectArch.left = _preferedPadding / 2;
                rectArch.top = _preferedPadding / 2;
                rectArch.right =  _preferedWidth - _preferedPadding / 2 ;
                rectArch.bottom =  _preferedWidth - _preferedPadding / 2 ;

                canvas.drawArc(rectArch, START_ANGLE_POINT, angleArch, false, paintArch);
            }
        }else if (getState() == BUTTON_STATE.STATE_PLAYING){

            int width = ( usableWidth / 3 )  - _preferedPadding;

            int x = _cx - width ;
            int y = _cy - width ;


            rectangle.top = y;
            rectangle.left = x + (_preferedPadding / 4);
            rectangle.right = _cx - (_preferedPadding / 2);
            rectangle.bottom = _cy +  width;
            canvas.drawRect(rectangle, paintRect);


            //second
            rectangle.top = y;
            rectangle.left = _cx + (_preferedPadding / 2) ;
            rectangle.right = _cx + (_cx - x) - (_preferedPadding / 4) ;
            rectangle.bottom = _cy +  width;
            canvas.drawRect(rectangle, paintRect);


            rectArch.left = _preferedPadding / 2;
            rectArch.top = _preferedPadding / 2;
            rectArch.right =  _preferedWidth - _preferedPadding / 2 ;
            rectArch.bottom =  _preferedWidth - _preferedPadding / 2 ;


            canvas.drawArc(rectArch, START_ANGLE_POINT, angleArch, false, paintArch);



        }else if(getState() == BUTTON_STATE.STATE_FETCHING){


//            _radius = Math.min(usableWidth - (_preferedPadding), usableHeight  - (_preferedPadding)) / 2;
//            canvas.drawCircle(_cx, _cy , _radius, paintFetching);

            rectArch.left = _preferedPadding / 2;
            rectArch.top = _preferedPadding / 2;
            rectArch.right =  _preferedWidth - _preferedPadding / 2 ;
            rectArch.bottom =  _preferedWidth - _preferedPadding / 2 ;


            canvas.drawArc(rectArch, START_ANGLE_POINT, angleArch, false, paintArch);


        }




    }

    public void setCircleColor(int circleColor)
    {
        this.circleColor = circleColor;
        invalidate();
    }

    public int getCircleColor()
    {
        return circleColor;
    }


    private class SingleTapConfirm extends SimpleOnGestureListener {

        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            return true;
        }
    }


    private void init(Context context, AttributeSet attrs)
    {



        gestureDetector = new GestureDetector(context, new SingleTapConfirm());
        setOnClickListener(this);
        setOnTouchListener(this);

//        mParentView = (View) getRootView().getParent();
//        mParentView = ((ViewGroup)mParentView).getChildAt(0);

        _preferedWidth = (int) dipToPixels(context, _preferedWidthDP);
        _preferedPadding = (int) dipToPixels(context, 4);
        setLayoutParams(new LinearLayout.LayoutParams(_preferedWidthDP, _preferedWidthDP));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElevation(10F);
        }
        setPadding(_preferedPadding, _preferedPadding, _preferedPadding, _preferedPadding);


        paint = new Paint();
        paint.setAntiAlias(true);
        paint.setColor(circleColor);

        trianglePaint = new Paint();
        trianglePaint.setColor(Color.WHITE);
        trianglePaint.setStyle(Paint.Style.FILL);
         _trianglePoint = new Point();



        // create the Paint and set its color
        paintRect = new Paint();
        paintRect.setColor(Color.WHITE);

        rectangle = new Rect(0, 0, 0 , 0);


        paintFetching = new Paint();
        paintFetching.setStyle(Paint.Style.STROKE);
        paintFetching.setColor(Color.WHITE);
        paintFetching.setStrokeWidth(dipToPixels(context, 2));


        paintFetching.setAlpha(255);
        // paint.setXfermode(xfermode);
        paintFetching.setAntiAlias(true);

        rotateAnim = new RotateAnimation(0, 360,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);

        rotateAnim.setDuration(1000);
        rotateAnim.setRepeatCount(Animation.INFINITE);
        rotateAnim.setRepeatMode(Animation.RESTART);
        rotateAnim.setInterpolator(new LinearInterpolator());



        paintArch = new Paint();
        paintArch.setAntiAlias(true);
        paintArch.setStyle(Paint.Style.STROKE);
        paintArch.setStrokeWidth(dipToPixels(context, 2));
        //Circle color
        paintArch.setColor(Color.WHITE);

        //size 200x200 example
        rectArch = new RectF(0, 0, 0, 0);

        //Initial Angle (optional, it can be zero)
        angleArch = 120;

    }

    private Path getEquilateralTriangle(Point p1, int width, int direction) {
        Point p2 = null, p3 = null;

        if (direction == Direction_NORTH) {
            p2 = new Point(p1.x + width, p1.y);
            p3 = new Point(p1.x + (width / 2), p1.y - width);
        } else if (direction == Direction_SOUTH) {
            p2 = new Point(p1.x + width, p1.y);
            p3 = new Point(p1.x + (width / 2), p1.y + width);
        } else if (direction == Direction_EAST) {
            p2 = new Point(p1.x, p1.y + width);
            p3 = new Point(p1.x - width, p1.y + (width / 2));
        } else if (direction == Direction_WEST) {
            p2 = new Point(p1.x, p1.y + width);
            p3 = new Point(p1.x + width, p1.y + (width / 2));
        }

        Path path = new Path();
        path.moveTo(p1.x, p1.y);
        path.lineTo(p2.x, p2.y);
        path.lineTo(p3.x, p3.y);

        return path;
    }

    public BUTTON_STATE getState(){
        return mState;
    }
    public void setState(BUTTON_STATE state){
        mState = state;
        if(state == BUTTON_STATE.STATE_FETCHING){
//            setState(BUTTON_STATE.STATE_FETCHING);

        }else if (state == BUTTON_STATE.STATE_PLAYING){
            isFetchingAnimRunning = false;
            startPlayingAnim();
        }else if (state == BUTTON_STATE.STATE_COMPLETED){
            isFetchingAnimRunning = false;
            angleArch  = 120;
            START_ANGLE_POINT = 90;
        }else {
            isFetchingAnimRunning = false;
        }
        invalidate();
    }

    public static float dipToPixels(Context context, float dipValue) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dipValue, metrics);
    }




    private void startFetchingAnim(){
        if(!isFetchingAnimRunning){
            return;
        }
        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {
                if(angleArch >= 350)
                    angleArch = 0;
                angleArch += 10;
                if(START_ANGLE_POINT >= 350)
                    START_ANGLE_POINT = 0;
                START_ANGLE_POINT += 10;
                invalidate();
                startFetchingAnim();
            }
        }, 50);

    }

    public void startFetching() {
        setState( PlayPauseView.BUTTON_STATE.STATE_FETCHING);
        if(!isFetchingAnimRunning){
            isFetchingAnimRunning = !isFetchingAnimRunning;
            startFetchingAnim();
        }
    }

    public void startPlayingAnim(){
        if(getState() != BUTTON_STATE.STATE_PLAYING || mMediaPlayer == null){
            return;
        }
        Handler h = new Handler();
        h.postDelayed(new Runnable() {
            @Override
            public void run() {

                angleArch = (mMediaPlayer.getCurrentPosition() * 360 ) / mMediaPlayer.getDuration();
                Log.d("Duration A", String.valueOf(mMediaPlayer.getDuration()));
                Log.d("Duration C", String.valueOf(mMediaPlayer.getCurrentPosition()));
                Log.d("Duration", String.valueOf(angleArch));

                START_ANGLE_POINT = 0;
                invalidate();
                startPlayingAnim();
            }
        }, 1000);

    }



    //listeners

    @Override
    public void onClick(View v) {





    }


    public void setClickedListener(OnClickListener listener){

        mListener  = listener;

    }

    public void setMediaPlayer(MediaPlayer mp){
        mMediaPlayer = mp;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {


        if (gestureDetector.onTouchEvent(event)) {

            if(mListener != null){
                mListener.onClick(view);
            }

            return true;
        } else {

//            view.setClickable(false);

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:

                    oldXvalue = view.getX() - event.getRawX();
                    oldYvalue = view.getY() - event.getRawY();
                    break;

                case MotionEvent.ACTION_MOVE:

//                Log.d("PARENT " , String.valueOf(getRootView().getX()));
//                Log.d("PARENT " , String.valueOf(getRootView().getY()));
//                if ((event.getRawY() + oldYvalue) < parentView.getY() || (event.getRawY() + oldYvalue) > (parentView.getY() + parentView.getHeight() - view.getHeight() - dipToPixels(view.getContext(), 24))) {
                    if ((event.getRawY() + oldYvalue) < getRootView().getY() || (event.getRawY() + oldYvalue) > (getRootView().getY() + getRootView().getHeight() - view.getHeight() - dipToPixels(view.getContext(), 24))) {
                        return false;
                    }

                    view.animate()
                            .x(event.getRawX() + oldXvalue)
                            .y(event.getRawY() + oldYvalue)
                            .setDuration(0)
                            .start();
                    break;
                default:
                    break;
//                return false;
            }
            return false;
        }
    }

}
