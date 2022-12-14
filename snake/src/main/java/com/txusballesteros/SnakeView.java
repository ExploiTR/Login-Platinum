/*
 * Actual file : Copyright Txus Ballesteros 2015 (@txusballesteros)
 *
 * This file is part of some open source application.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * Contact: Txus Ballesteros <txus.ballesteros@gmail.com>
 *
 *
 * This is a heavily modified version of SnakeView by @txusballesteros
 * with a legend chart and max value indicator
 */
package com.txusballesteros;

import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SnakeView extends View {
	public static final int DEFAULT_ANIMATION_DURATION = 100;
	public static final float BEZIER_FINE_FIT = 0.5f;
	public static final int DEF_STYLE_ATTR = 0;
	public static final int DEF_STYLE_RES = 0;
	public static final float DEFAULT_MIN_VALUE = 0f;
	public static final float DEFAULT_MAX_VALUE = 1f;
	public static final int MINIMUM_NUMBER_OF_VALUES = 3;
	public static final int SCALE_MODE_FIXED = 0;
	public static final int SCALE_MODE_AUTO = 1;
	public static final int CHART_STILE_STROKE = 0;
	public static final int CHART_STILE_FILL = 1;
	public static final int CHART_STILE_FILL_STROKE = 2;
	private final static int DEFAULT_MAXIMUM_NUMBER_OF_VALUES_FOR_DESIGNER = 3;
	private final static int DEFAULT_MAXIMUM_NUMBER_OF_VALUES_FOR_RUNTIME = 10;
	private final static int DEFAULT_STROKE_COLOR = 0xff78c257;
	private final static int DEFAULT_FILL_COLOR = 0x8078c257;
	private final static int DEFAULT_STROKE_WIDTH_IN_DP = 0;
	private int maximumNumberOfValues = DEFAULT_MAXIMUM_NUMBER_OF_VALUES_FOR_RUNTIME;
	private int strokeColor = DEFAULT_STROKE_COLOR;
	private int strokeWidthInPx = (int) dp2px();
	private int fillColor = DEFAULT_FILL_COLOR;
	private RectF drawingArea;
	private Paint paint;
	private Paint fillPaint;
	private Paint opaquePaint;
	private Queue<Float> valuesCache;
	private List<Float> previousValuesCache;
	private List<Float> currentValuesCache;
	private int animationDuration = DEFAULT_ANIMATION_DURATION;
	private float animationProgress = 1.0f;
	private float scaleInX = 0f;
	private float scaleInY = 0f;
	private float minValue = DEFAULT_MIN_VALUE;
	private float maxValue = DEFAULT_MAX_VALUE;
	private int scaleMode = SCALE_MODE_FIXED;
	private int chartStyle = CHART_STILE_STROKE;

	private TextPaint legendPaint;
	private TextPaint legendPaint2;
	private TextPaint maxValuePaint;
	private Paint legendCirclePaintYellow, legendCirclePaintBlue, maxLinePaint;
	private StaticLayout mStaticLayoutYellow;
	private StaticLayout mStaticLayoutBlue;
	private StaticLayout mStaticLayoutRed;
	private String yellowText = "average";
	private String blueText = "realtime";
	private String redText = "200ms";
	private Path opaQuePath;
	private Path circlePath;
	private Path maxLinePath;
	private boolean built = false;
	private boolean showMeter = true;


	public SnakeView(Context context) {
		super(context);
		initializeView();
		initLabelView();
	}

	public SnakeView(Context context, AttributeSet attrs) {
		super(context, attrs);
		configureAttributes(attrs);
		initializeView();
		initLabelView();
	}

	public SnakeView(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
		configureAttributes(attrs);
		initializeView();
	}

	public SnakeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
		configureAttributes(attrs);
		initializeView();
	}

	private void initLabelView() {
		legendPaint = new TextPaint();
		legendPaint.setAntiAlias(true);
		legendPaint.setTextSize(16 * getResources().getDisplayMetrics().density);
		legendPaint.setColor(Color.parseColor("#FFD700"));

		legendPaint2 = new TextPaint();
		legendPaint2.setAntiAlias(true);
		legendPaint2.setTextSize(16 * getResources().getDisplayMetrics().density);
		legendPaint2.setColor(Color.parseColor("#1A237E"));

		maxValuePaint = new TextPaint();
		maxValuePaint.setAntiAlias(true);
		maxValuePaint.setTextSize(12 * getResources().getDisplayMetrics().density);
		maxValuePaint.setColor(Color.parseColor("#1A237E"));

		int width = (int) legendPaint.measureText(yellowText);
		mStaticLayoutYellow =
				new StaticLayout(yellowText, legendPaint,
						width, Layout.Alignment.ALIGN_NORMAL,
						1.0f, 0, false);

		width = (int) legendPaint2.measureText(blueText);
		mStaticLayoutBlue =
				new StaticLayout(blueText, legendPaint2,
						width, Layout.Alignment.ALIGN_NORMAL,
						1.0f, 0, false);

		width = (int) maxValuePaint.measureText(redText);
		mStaticLayoutRed =
				new StaticLayout(redText, maxValuePaint,
						width, Layout.Alignment.ALIGN_NORMAL,
						1.0f, 0, false);

		Typeface plain = Typeface.createFromAsset(getContext().getAssets(), "mosk.ttf");
		legendPaint.setTypeface(plain);
		legendPaint2.setTypeface(plain);
		maxValuePaint.setTypeface(plain);

		opaQuePath = new Path();
		circlePath = new Path();
		maxLinePath = new Path();

		float size = 16 * getResources().getDisplayMetrics().density;
		buildPathOpaque();
		buildPathCircles();
	}

	public void setMaximumNumberOfValues(int maximumNumberOfValues) {
		if (maximumNumberOfValues < MINIMUM_NUMBER_OF_VALUES) {
			throw new IllegalArgumentException("The maximum number of values cannot be less than three.");
		}
		this.maximumNumberOfValues = maximumNumberOfValues;
		calculateScales();
		initializeCaches();
	}

	public void setScaleMode(int scaleMode) {
		this.scaleMode = scaleMode;
		calculateScales();
		initializeCaches();
	}

	public void setMinValue(float minValue) {
		this.minValue = minValue;
		calculateScales();
		initializeCaches();
	}

	public void setMaxValue(float maxValue) {
		this.maxValue = maxValue;
		calculateScales();
		initializeCaches();
	}

	public void addValue(float value) {
		if (scaleMode == SCALE_MODE_FIXED) {
			if (value < minValue || value > maxValue) {
				throw new IllegalArgumentException("The value is out of min or max limits. Value = " + value);
			}
		}
		previousValuesCache = cloneCache();
		if (valuesCache.size() == maximumNumberOfValues) {
			valuesCache.poll();
		}
		valuesCache.add(value);
		currentValuesCache = cloneCache();
		if (scaleMode == SCALE_MODE_AUTO) {
			calculateScales();
		}
		playAnimation();
	}

	public void clear() {
		initializeCaches();
		invalidate();
	}

	public void setFillColor(int color) {
		fillColor = color;
		initializePaint();
	}

	private void configureAttributes(AttributeSet attrs) {
		TypedArray attributes = getContext().getTheme()
				.obtainStyledAttributes(attrs, R.styleable.SnakeView,
						DEF_STYLE_ATTR, DEF_STYLE_RES);
		scaleMode = attributes.getInteger(R.styleable.SnakeView_scaleMode, scaleMode);
		strokeColor = attributes.getColor(R.styleable.SnakeView_strokeColor, strokeColor);
		fillColor = attributes.getColor(R.styleable.SnakeView_fillColor, fillColor);
		strokeWidthInPx = attributes.getDimensionPixelSize(R.styleable.SnakeView_strokeWidth, strokeWidthInPx);
		chartStyle = attributes.getInteger(R.styleable.SnakeView_chartStyle, chartStyle);
		if (scaleMode == SCALE_MODE_FIXED) {
			minValue = attributes.getFloat(R.styleable.SnakeView_minValue, DEFAULT_MIN_VALUE);
			maxValue = attributes.getFloat(R.styleable.SnakeView_maxValue, DEFAULT_MAX_VALUE);
		}
		int defaultMaximumNumberOfValues = DEFAULT_MAXIMUM_NUMBER_OF_VALUES_FOR_RUNTIME;
		if (isInEditMode()) {
			defaultMaximumNumberOfValues = DEFAULT_MAXIMUM_NUMBER_OF_VALUES_FOR_DESIGNER;
		}
		maximumNumberOfValues = attributes.getInteger(R.styleable.SnakeView_maximumNumberOfValues,
				defaultMaximumNumberOfValues);
		animationDuration = attributes.getInteger(R.styleable.SnakeView_animationDuration,
				DEFAULT_ANIMATION_DURATION);
		if (maximumNumberOfValues < MINIMUM_NUMBER_OF_VALUES) {
			throw new IllegalArgumentException("The maximum number of values cannot be less than three.");
		}
		showMeter = attributes.getBoolean(R.styleable.SnakeView_showMeter,showMeter);

		attributes.recycle();
	}

	private void initializeView() {
		initializePaint();
		initializeCaches();
	}

	private void initializePaint() {
		paint = new Paint();
		paint.setFlags(Paint.ANTI_ALIAS_FLAG);
		paint.setColor(strokeColor);
		paint.setStyle(Paint.Style.STROKE);
		if (chartStyle == CHART_STILE_STROKE) {
			paint.setStrokeCap(Paint.Cap.ROUND);
		}
		paint.setStrokeWidth(strokeWidthInPx);

		fillPaint = new Paint();
		if (okayForAA())
			fillPaint.setFlags(Paint.ANTI_ALIAS_FLAG);

		fillPaint.setColor(fillColor);
		fillPaint.setStyle(Paint.Style.FILL);

		opaquePaint = new Paint();
		if (okayForAA())
			opaquePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		opaquePaint.setColor(Color.parseColor("#40ffffff"));
		opaquePaint.setStyle(Paint.Style.FILL);

		legendCirclePaintYellow = new Paint();
		if (okayForAA())
			legendCirclePaintYellow.setFlags(Paint.ANTI_ALIAS_FLAG);
		legendCirclePaintYellow.setColor(Color.parseColor("#FFD700"));
		legendCirclePaintYellow.setStyle(Paint.Style.FILL);

		legendCirclePaintBlue = new Paint();
		if (okayForAA())
			legendCirclePaintBlue.setFlags(Paint.ANTI_ALIAS_FLAG);
		legendCirclePaintBlue.setColor(Color.parseColor("#1A237E"));
		legendCirclePaintBlue.setStyle(Paint.Style.FILL);

		maxLinePaint = new Paint();
		if (okayForAA())
			maxLinePaint.setFlags(Paint.ANTI_ALIAS_FLAG);
		maxLinePaint.setColor(Color.parseColor("#1A237E"));
		maxLinePaint.setStyle(Paint.Style.STROKE);
		maxLinePaint.setStrokeWidth(2f);
	}

	private boolean okayForAA() {
		ActivityManager actManager = (ActivityManager) getContext()
				.getSystemService(Context.ACTIVITY_SERVICE);
		ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
		actManager.getMemoryInfo(memInfo);
		return memInfo.totalMem > 3500; //>3500 must be 4gb
	}

	private void initializeCaches() {
		if (isInEditMode()) {
			initializeCacheForDesigner();
		} else {
			initializeCacheForRuntime();
		}
		previousValuesCache = cloneCache();
		currentValuesCache = cloneCache();
	}

	private void initializeCacheForDesigner() {
		valuesCache = new ConcurrentLinkedQueue<>();
		for (int counter = 0; counter < maximumNumberOfValues; counter++) {
			if (counter % 2 == 0) {
				valuesCache.add(minValue);
			} else {
				valuesCache.add(maxValue);
			}
		}
	}

	private void initializeCacheForRuntime() {
		valuesCache = new ConcurrentLinkedQueue<>();
		for (int counter = 0; counter < maximumNumberOfValues; counter++) {
			valuesCache.add(minValue);
		}
	}

	private List<Float> cloneCache() {
		return new ArrayList<>(valuesCache);
	}

	private float dp2px() {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, (float) SnakeView.DEFAULT_STROKE_WIDTH_IN_DP,
				getResources().getDisplayMetrics());
	}

	@Override
	protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
		calculateDrawingArea(width, height);
		calculateScales();
	}

	private void calculateDrawingArea(int width, int height) {
		int left = getPaddingLeft();
		int top = getPaddingTop();
		int right = width - getPaddingRight();
		int bottom = height - getPaddingBottom();
		drawingArea = new RectF(left, top, right, bottom);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		canvas.save();

		if (!valuesCache.isEmpty()) {
			if (chartStyle == CHART_STILE_FILL || chartStyle == CHART_STILE_FILL_STROKE) {
				Path fillPath = buildPath(true);
				canvas.drawPath(fillPath, fillPaint);
			}

			if (chartStyle == CHART_STILE_STROKE || chartStyle == CHART_STILE_FILL_STROKE) {
				Path strokePath = buildPath(false);
				canvas.drawPath(strokePath, paint);
			}
		}


		canvas.translate(getPaddingStart() + getWidth() * 0.1f,
				getPaddingTop() + getHeight() * 0.4f);
		canvas.drawPath(opaQuePath, opaquePaint);

		canvas.translate(32, 32);
		canvas.drawPath(circlePath, legendCirclePaintYellow);
		canvas.translate(32, -32);
		mStaticLayoutYellow.draw(canvas);

		canvas.translate(-32, 96);
		canvas.drawPath(circlePath, legendCirclePaintBlue);
		canvas.translate(32, -32);
		mStaticLayoutBlue.draw(canvas);

		canvas.restore();

		if(showMeter){
			buildMaxLinePath();
			canvas.translate(20, 0);
			canvas.drawPath(maxLinePath, maxLinePaint);

			canvas.translate(48, 0);
			mStaticLayoutRed.draw(canvas);
		}
	}

	private void buildPathOpaque() {
		opaQuePath.reset();
		Rect rect = new Rect();
		legendPaint.getTextBounds(blueText, 0, 7, rect);
		opaQuePath.addRoundRect(0, 0, rect.width() * 2f, rect.height() * 3f + 32,
				new float[]{20f, 20f,
						20f, 20f,
						20f, 20f,
						20f, 20f},
				Path.Direction.CW);
	}

	private void buildPathCircles() {
		circlePath.reset();
		circlePath.addCircle(0, 0, 12, Path.Direction.CW);
	}

	private void buildMaxLinePath() {
		if (built)
			return;
		maxLinePath.reset();
		maxLinePath.moveTo(0, 0);
		maxLinePath.lineTo(24, 0);
		maxLinePath.lineTo(12, 0);
		maxLinePath.lineTo(12, getHeight() - 5f); //24 + 1 - 20adj for -20 translate stroke
		maxLinePath.lineTo(24, getHeight() - 5f);
		maxLinePath.lineTo(0, getHeight() - 5f);
		maxLinePath.lineTo(12, getHeight() - 5f);
		maxLinePath.lineTo(12, 0);
		maxLinePath.moveTo(0, 0);
		built = true;
	}

	private Path buildPath(boolean forFill) {
		int strokePadding = (strokeWidthInPx / 2);
		Path path = new Path();
		if (forFill) {
			path.moveTo(drawingArea.left, drawingArea.bottom + strokePadding);
		}
		float previousX = drawingArea.left;
		float previousY = drawingArea.bottom;
		for (int index = 0; index < currentValuesCache.size(); index++) {
			float previousValue = previousValuesCache.get(index);
			float currentValue = currentValuesCache.get(index);
			float pathValue = previousValue + ((currentValue - previousValue) * animationProgress);
			float x = drawingArea.left + (scaleInX * index);
			float y = drawingArea.bottom - ((pathValue - minValue) * scaleInY);
			if (index == 0) {
				if (forFill) {
					path.lineTo(x, y);
				} else {
					path.moveTo(x, y);
				}
			} else {
				float bezierControlX = previousX + ((x - previousX) * BEZIER_FINE_FIT);
				path.cubicTo(bezierControlX, previousY,
						bezierControlX, y,
						x, y);
			}
			previousX = x;
			previousY = y;
		}
		if (forFill) {
			path.lineTo(drawingArea.right, drawingArea.bottom + strokePadding);
		}
		return path;
	}

	private void calculateScales() {
		if (drawingArea != null) {
			if (scaleMode == SCALE_MODE_AUTO) {
				for (int index = 0; index < currentValuesCache.size(); index++) {
					float previousValue = previousValuesCache.get(index);
					float currentValue = currentValuesCache.get(index);
					if (index == 0) {
						minValue = Math.min(currentValue, previousValue);
						maxValue = Math.max(currentValue, previousValue);
					} else {
						minValue = Math.min(minValue, currentValue);
						maxValue = Math.max(maxValue, currentValue);
						minValue = Math.min(minValue, previousValue);
						maxValue = Math.max(maxValue, previousValue);
					}
				}
			}
			scaleInX = (drawingArea.width() / (maximumNumberOfValues - 1));
			scaleInY = (drawingArea.height() / (maxValue - minValue));
		} else {
			scaleInY = 0f;
			scaleInX = 0f;
		}
	}

	private void playAnimation() {
		ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
		animator.addUpdateListener(animation -> {
			SnakeView.this.animationProgress = (float) animation.getAnimatedValue();
			invalidate();
		});
		animator.setDuration(animationDuration);
		animator.setInterpolator(new LinearInterpolator());
		animator.start();
	}
}