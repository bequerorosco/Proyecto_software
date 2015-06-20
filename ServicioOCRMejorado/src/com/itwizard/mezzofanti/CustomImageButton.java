/*
 * Copyright (C) 2011 José Manuel Cernuda
 * http://androidelibre.wordpress.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.itwizard.mezzofanti;

import com.uah.servicioocr.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Class for Buttons that hold images.
 *
 */
public class CustomImageButton extends View 
{
	private int m_nWidthPadding = 0;		// width padding 
	private int m_nHeightPadding = 0;		// height padding
	private final String m_sLabel;		// the label
	private int m_iImageResId;			// the image red-id
	private Bitmap m_bmImage;			// the image
	private Context m_oContext; // Activity context in which the button view is being placed for.
	
	/**
	* Constructor.
	*
	* @param context
	*        Activity context in which the button view is being placed for.
	*
	* @param resImage
	*        Image to put on the button. This image should have been placed
	*        in the drawable resources directory.
	*
	* @param label
	*        The text label to display for the custom button.
	*/
	public CustomImageButton(Context context, int resImage, String label, int stretchWidth, int stretchHeigth) 
	{
		super(context);
		m_oContext = context;
		this.m_sLabel = label;
		this.m_iImageResId = resImage;
		if (stretchHeigth == 0 && stretchWidth == 0)
		{
			this.m_bmImage = BitmapFactory.decodeResource(context.getResources(), m_iImageResId);
		}
		else
		{
			Bitmap imageAux = BitmapFactory.decodeResource(context.getResources(),m_iImageResId);
			this.m_bmImage = Bitmap.createScaledBitmap(imageAux, stretchWidth, stretchHeigth, true);
		}	
		setFocusable(true);
		setBackgroundColor(Color.TRANSPARENT);
		
		setClickable(true);
	}
	/**
	* Constructor.
	*
	* @param context
	*        Activity context in which the button view is being placed for.
	*
	* @param attrs
	*/
	public CustomImageButton(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		m_oContext = context;
		this.m_sLabel = "";
		this.m_iImageResId = R.drawable.switch_32;
		this.m_bmImage = BitmapFactory.decodeResource(context.getResources(), m_iImageResId);
		setBackgroundColor(Color.TRANSPARENT);
		
		setClickable(true);
	}
	/**
	 * Set the internal image to be used for the button.
	 * @param resImage the resource-id
	 * @param stretchWidth stretch factor for the width
	 * @param stretchHeigth stretch factor for the height
	 */
	public void SetImage(int resImage, int stretchWidth, int stretchHeigth)
	{
		this.m_iImageResId = resImage;
		if (stretchHeigth == 0 && stretchWidth == 0)
		{
			this.m_bmImage = BitmapFactory.decodeResource(m_oContext.getResources(), m_iImageResId);
		}
		else
		{
			Bitmap imageAux = BitmapFactory.decodeResource(m_oContext.getResources(), m_iImageResId);
			this.m_bmImage = Bitmap.createScaledBitmap(imageAux, stretchWidth, stretchHeigth, true);
		}
	}
	
	/**
	 * Set the padding.
	 * @param width_padding width padding
	 * @param height_padding height padding
	 */
	public void SetMargins(int width_padding, int height_padding)
	{
		m_nWidthPadding = width_padding;
		m_nHeightPadding = height_padding;
	}

	/**
	* The method that is called when the focus is changed to or from this view.
	*/
	protected void onFocusChanged(boolean gainFocus, int direction,Rect previouslyFocusedRect)
	{
		if (gainFocus == true)
		{
		    this.setBackgroundColor(Color.rgb(255, 165, 0));
		}
		else
		{
			setBackgroundColor(Color.TRANSPARENT);
		}
	}	

	/**
	* Method called on to render the view.
	*/
	protected void onDraw(Canvas canvas)
	{
		Paint textPaint = new Paint();
		canvas.drawBitmap(m_bmImage, m_nWidthPadding / 2, m_nHeightPadding / 2, null);
		if (m_sLabel != null)
			canvas.drawText(m_sLabel, m_nWidthPadding / 2, (m_nHeightPadding / 2) + m_bmImage.getHeight() + 8, textPaint);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) 
	{
		setMeasuredDimension(MeasureWidth(widthMeasureSpec), MeasureHeight(heightMeasureSpec));
	}
	
	/**
	 * Measure the width of the image
	 * @param measureSpec the measure specification to extract the size from
	 * @return size in pixels
	 */
	private int MeasureWidth(int measureSpec)
	{
		int preferred = m_bmImage.getWidth();
		return GetMeasurement(measureSpec, preferred);
	}

	/**
	 * Measure the width of the image
	 * @param measureSpec the measure specification to extract the size from
	 * @return size in pixels
	 */
	private int MeasureHeight(int measureSpec)
	{
		int preferred = m_bmImage.getHeight();
		return GetMeasurement(measureSpec, preferred);
	}
	
	/**
	 * Get the width/height according to the internal MeasureSpec mode
	 * 
	 * @param measureSpec the measure specification to extract the size from
	 * @param preferred preferred size in pixels
	 * @return size in pixels
	 */
	private int GetMeasurement(int measureSpec, int preferred)
	{
		int specSize = MeasureSpec.getSize(measureSpec);
		int measurement = 0;
	
		switch(MeasureSpec.getMode(measureSpec))
		{
			case MeasureSpec.EXACTLY:
					// This means the width of this view has been given.
					measurement = specSize;
					break;
			case MeasureSpec.AT_MOST:
					// Take the minimum of the preferred size and what
					// we were told to be.
					measurement = Math.min(preferred, specSize);
					break;
			default:
					measurement = preferred;
			break;
		}

		return measurement;
	}
	

	/**
	 * @return the label of the button.
	 */
	public String GetLabel()
	{
		return m_sLabel;
	}

	/**
	 * @return 	the resource id of the image.
	 */
	public int GetImageResId()
	{
		return m_iImageResId;
	}
	
}
    

	