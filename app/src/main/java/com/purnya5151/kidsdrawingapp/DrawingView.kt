package com.purnya5151.kidsdrawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

class DrawingView(context : Context, attrs: AttributeSet) : View(context , attrs) {

    private var mDrawPath : CoustomPath? =null
    private var mCanvasBitmap : Bitmap? =null
    private var mDrawPaint : Paint? =null
    private var mCanvasPaint : Paint? =null
    private var mBrushSize : Float = 0.toFloat()
    private var color = Color.BLACK
    private var canvas:Canvas?=null
    private val mPath = ArrayList<CoustomPath>()
    private val mUndoPath = ArrayList<CoustomPath>()
    private val mRedoPath = ArrayList<CoustomPath>()

    init {
        setUpDrawing()
    }

    fun onClickUndoPath(){
        if (mPath.size > 0){
            mUndoPath.add(mPath.removeAt(mPath.size - 1))
            invalidate()
        }
    }
//    fun onClickRedoPath(){
//        if (mPath.size > 0){
//            mRedoPath.add(mUndoPath.removeAt(mUndoPath.size - 1))
//            invalidate()
//        }
//    }

    private fun setUpDrawing(){
        mDrawPaint = Paint()
        mDrawPath = CoustomPath(color,mBrushSize)
        mDrawPaint!!.color = color
        mDrawPaint!!.style = Paint.Style.STROKE
        mDrawPaint!!.strokeJoin = Paint.Join.ROUND
        mDrawPaint!!.strokeCap = Paint.Cap.ROUND
        mCanvasPaint = Paint(Paint.DITHER_FLAG)
        //mBrushSize = 20.toFloat()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCanvasBitmap = Bitmap.createBitmap(w,h, Bitmap.Config.ARGB_8888)
        canvas = Canvas(mCanvasBitmap!!)
    }
// Change Canvas to Canvas? if fails
    override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    canvas.drawBitmap(mCanvasBitmap!!, 0f, 0f, mCanvasPaint)

    for (path in mPath){
        mDrawPaint!!.strokeWidth = path.BrushThickness
        mDrawPaint!!.color = path.color
        canvas.drawPath(path, mDrawPaint!!)
    }

    if (!mDrawPath!!.isEmpty) {
        mDrawPaint!!.strokeWidth = mDrawPath!!.BrushThickness
        mDrawPaint!!.color = mDrawPath!!.color
        canvas.drawPath(mDrawPath!!, mDrawPaint!!)
    }
}

    override fun onTouchEvent(event: MotionEvent?): Boolean {

        var touchX = event?.x
        var touchY = event?.y

        when(event?.action){
            MotionEvent.ACTION_DOWN -> {
                mDrawPath!!.color = color
                mDrawPath!!.BrushThickness = mBrushSize

                mDrawPath!!.reset()
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.moveTo(touchX,touchY)
                    }
                }
            }

            MotionEvent.ACTION_MOVE->{
                if (touchX != null) {
                    if (touchY != null) {
                        mDrawPath!!.lineTo(touchX,touchY)
                    }
                }
            }
            MotionEvent.ACTION_UP->{
                mPath.add(mDrawPath!!)
                mDrawPath = CoustomPath(color,mBrushSize)
            }
            else -> return false
        }
        invalidate()
        return true

        return true

    }

    fun setSizeFORBrush(newSize : Float){

        mBrushSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, newSize, resources.displayMetrics)
        mDrawPaint!!.strokeWidth = mBrushSize

    }
    fun setColor(newColor : String){
        color = Color.parseColor(newColor)
        mDrawPaint!!.color = color
    }

    internal inner class CoustomPath(var color: Int , var BrushThickness: Float): Path(){

    }


}