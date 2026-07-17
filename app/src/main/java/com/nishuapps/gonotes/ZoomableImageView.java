package com.nishuapps.gonotes;

import android.content.Context;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageView;

public class ZoomableImageView extends ImageView {
	
	private static final float MIN_SCALE = 1f;
	private static final float MAX_SCALE = 5f;
	
	private final Matrix matrix = new Matrix();
	private final float[] matrixValues = new float[9];
	
	private ScaleGestureDetector scaleDetector;
	private GestureDetector gestureDetector;
	
	private float lastTouchX, lastTouchY;
	private int mode = 0; // 0 = none, 1 = drag
	
	private int viewWidth, viewHeight;
	private float saveScale = 1f;
	
	public ZoomableImageView(Context context) {
		super(context);
		init(context);
	}
	
	public ZoomableImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}
	
	public ZoomableImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init(context);
	}
	
	private void init(Context context) {
		setScaleType(ScaleType.MATRIX);
		scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
		gestureDetector = new GestureDetector(context, new GestureListener());
		
		setOnTouchListener((v, event) -> {
			scaleDetector.onTouchEvent(event);
			gestureDetector.onTouchEvent(event);
			
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
				case MotionEvent.ACTION_DOWN:
				lastTouchX = event.getX();
				lastTouchY = event.getY();
				mode = 1;
				break;
				
				case MotionEvent.ACTION_MOVE:
				if (mode == 1 && saveScale > MIN_SCALE) {
					float dx = event.getX() - lastTouchX;
					float dy = event.getY() - lastTouchY;
					matrix.postTranslate(dx, dy);
					fixTranslation();
					lastTouchX = event.getX();
					lastTouchY = event.getY();
				}
				break;
				
				case MotionEvent.ACTION_UP:
				case MotionEvent.ACTION_POINTER_UP:
				mode = 0;
				break;
			}
			setImageMatrix(matrix);
			invalidate();
			return true;
		});
	}
	
	@Override
	public void setImageDrawable(android.graphics.drawable.Drawable drawable) {
		super.setImageDrawable(drawable);
		resetZoom();
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		viewWidth = w;
		viewHeight = h;
		resetZoom();
	}
	
	public void resetZoom() {
		matrix.reset();
		saveScale = 1f;
		if (getDrawable() != null && viewWidth > 0 && viewHeight > 0) {
			int dWidth = getDrawable().getIntrinsicWidth();
			int dHeight = getDrawable().getIntrinsicHeight();
			if (dWidth > 0 && dHeight > 0) {
				float scaleX = (float) viewWidth / dWidth;
				float scaleY = (float) viewHeight / dHeight;
				float initialScale = Math.min(scaleX, scaleY);
				float dx = (viewWidth - dWidth * initialScale) / 2f;
				float dy = (viewHeight - dHeight * initialScale) / 2f;
				matrix.setScale(initialScale, initialScale);
				matrix.postTranslate(dx, dy);
			}
		}
		setImageMatrix(matrix);
		invalidate();
	}
	
	private void fixTranslation() {
		if (getDrawable() == null) return;
		matrix.getValues(matrixValues);
		float transX = matrixValues[2];
		float transY = matrixValues[5];
		float curScaleX = matrixValues[0];
		float curScaleY = matrixValues[4];
		
		float contentW = getDrawable().getIntrinsicWidth() * curScaleX;
		float contentH = getDrawable().getIntrinsicHeight() * curScaleY;
		
		float fixX = 0, fixY = 0;
		
		if (contentW <= viewWidth) {
			fixX = (viewWidth - contentW) / 2f - transX;
			} else if (transX > 0) {
			fixX = -transX;
			} else if (transX < -(contentW - viewWidth)) {
			fixX = -(contentW - viewWidth) - transX;
		}
		
		if (contentH <= viewHeight) {
			fixY = (viewHeight - contentH) / 2f - transY;
			} else if (transY > 0) {
			fixY = -transY;
			} else if (transY < -(contentH - viewHeight)) {
			fixY = -(contentH - viewHeight) - transY;
		}
		
		matrix.postTranslate(fixX, fixY);
	}
	
	private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			float scaleFactor = detector.getScaleFactor();
			float newScale = saveScale * scaleFactor;
			
			if (newScale < MIN_SCALE) {
				scaleFactor = MIN_SCALE / saveScale;
				newScale = MIN_SCALE;
				} else if (newScale > MAX_SCALE) {
				scaleFactor = MAX_SCALE / saveScale;
				newScale = MAX_SCALE;
			}
			saveScale = newScale;
			
			matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
			fixTranslation();
			setImageMatrix(matrix);
			return true;
		}
	}
	
	private class GestureListener extends GestureDetector.SimpleOnGestureListener {
		@Override
		public boolean onDoubleTap(MotionEvent e) {
			if (saveScale > MIN_SCALE) {
				resetZoom();
				} else {
				float targetScale = 3f;
				matrix.postScale(targetScale, targetScale, e.getX(), e.getY());
				saveScale = targetScale;
				fixTranslation();
				setImageMatrix(matrix);
			}
			invalidate();
			return true;
		}
	}
}