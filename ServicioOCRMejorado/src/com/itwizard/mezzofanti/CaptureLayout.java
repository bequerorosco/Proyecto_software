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
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * This view is overlaid on top of the camera preview.
 */
public final class CaptureLayout extends View 
{
	private static final String TAG = "MLOG: CaptureLayout.java: ";

	private final int MAX_CHARS_PER_LINE = 50;
	private final int MAX_LINES_TO_DISPLAY = 5;
	
	private final Paint m_pFocus = new Paint();
	private final Rect m_Box;
	private final int m_nMaskColor;
	private final int m_nFrameColor;
	private Context m_oContext;
	private final Paint m_Paint = new Paint();
	private boolean m_bDisplayFocusImage = false;
	private boolean m_bOrientationFocusImage = false;  // horizontal= false vertical=true
	private boolean m_bFocused = false; 
	private boolean m_bDrawFocused = false; 
	private boolean m_bLineMode = true;
	private String m_sWaitingText = "";
	private String m_sUpperText ="";
	
	/**
	 *  This constructor is used when the class is built from an XML resource.
	 * @param context
	 * @param attrs
	 */
	public CaptureLayout(Context context, AttributeSet attrs) 
	{
		super(context, attrs);
		m_oContext = context;
		m_Box = new Rect();
		Resources resources = getResources();
		m_nMaskColor = resources.getColor(R.color.capturelayout_mask);
		m_nFrameColor = resources.getColor(R.color.capturelayout_frame);
	}

	/**
	 * Enable the drawing of 2 arrow icons for focus
	 * @param bEnable enable/disable
	 * @param orientation horizontal/vertical
	 */
	public void DrawFocusIcon(boolean bEnable, boolean orientation)
	{
		m_bDisplayFocusImage = bEnable;
		m_bOrientationFocusImage = orientation;
		invalidate();
	}
  
	/**
	 * Enable the drawing of green/red circle for focus ok/bad 
	 * @param bEnable enable/disable
	 * @param focused focused/not focused
	 */
	public void DrawFocused(boolean bEnable, boolean focused)
	{
		m_bDrawFocused = bEnable;
		m_bFocused = focused;
		invalidate();
	}

	/**
	 * Switch in between modes
	 * @param mode true=line false=all
	 */
	public void SetLineMode(boolean mode)
	{
		m_bLineMode = mode;
		invalidate();
	}

	/**
	 * Set result text for the LineMode
	 * @param text = the result
	 */
	public void SetText(String text)
	{
		m_sUpperText = text;
		invalidate();
	}
	
	/**
	 * Show the waiting (text/icon)
	 * @param val
	 */
	public void ShowWaiting(String wait_text)
	{
		m_sWaitingText = wait_text;
		invalidate();
	}

	@Override
	public void onDraw(Canvas canvas) 
	{
		try 
		{
			Rect frame = CameraManager.get().GetFramingRect(m_bLineMode);
			
			int width = canvas.getWidth();
			int height = canvas.getHeight();

			// Draw the exterior (i.e. outside the framing rect) darkened
			m_Paint.setColor(m_nMaskColor);
			m_Box.set(0, 0, width, frame.top);
			canvas.drawRect(m_Box, m_Paint);
			m_Box.set(0, frame.top, frame.left, frame.bottom + 1);
			canvas.drawRect(m_Box, m_Paint);
			m_Box.set(frame.right + 1, frame.top, width, frame.bottom + 1);
			canvas.drawRect(m_Box, m_Paint);
			m_Box.set(0, frame.bottom + 1, width, height);
			canvas.drawRect(m_Box, m_Paint);
			
			// Draw black frame
			m_Paint.setColor(m_nFrameColor);
			m_Box.set(frame.left, frame.top, frame.right + 1, frame.top + 2);
			canvas.drawRect(m_Box, m_Paint);
			m_Box.set(frame.left, frame.top + 2, frame.left + 2, frame.bottom - 1);
			canvas.drawRect(m_Box, m_Paint);
			m_Box.set(frame.right - 1, frame.top, frame.right + 1, frame.bottom - 1);
			canvas.drawRect(m_Box, m_Paint);
			m_Box.set(frame.left, frame.bottom - 1, frame.right + 1, frame.bottom + 1);
			canvas.drawRect(m_Box, m_Paint);
		
			if (!m_bLineMode)
			{
				if (m_bDisplayFocusImage)
				{
					Bitmap bm;
					if (m_bOrientationFocusImage)
					{ // Horizontal
						bm = BitmapFactory.decodeResource(m_oContext.getResources(), R.drawable.landscape);
						canvas.drawBitmap(bm, 20, 20, null);
					}
					else
					{ // Vertical
						bm = BitmapFactory.decodeResource(m_oContext.getResources(), R.drawable.portrait);
						canvas.drawBitmap(bm, 20, height - 80, null);
					}
				}
			}
			if (m_bDrawFocused)
			{
				if (m_bFocused)
					m_pFocus.setColor(Color.GREEN);
				else
					m_pFocus.setColor(Color.RED);
				Paint pblack = new Paint();
				pblack.setColor(Color.BLACK);
				canvas.drawCircle((width - 25), (height - 55) , 11, pblack);
				canvas.drawCircle((width - 25), (height - 55) , 10, m_pFocus);
			}
			
			// Display Upper text
			if (m_bLineMode)
			{
				int i = 0;
				
				int start = m_sUpperText.length() - MAX_CHARS_PER_LINE * MAX_LINES_TO_DISPLAY;
				m_sUpperText = m_sUpperText.substring((start<0 ? 0 : start), m_sUpperText.length());
				
				Paint p = new Paint();
				p.setColor(Color.LTGRAY);
				p.setTextSize(p.getTextSize() + 5);
				
				byte[] buff = new byte[m_sUpperText.length()];
				buff = m_sUpperText.getBytes("UTF-8");
				for(; i < m_sUpperText.length() / MAX_CHARS_PER_LINE; i++)
				{
					String s = new String(buff, i*MAX_CHARS_PER_LINE, MAX_CHARS_PER_LINE);
					canvas.drawText(s, 20, 20 + i * 20, p);
				}
				if ((m_sUpperText.length() % MAX_CHARS_PER_LINE) != 0)
				{
					String s = new String(buff, i*MAX_CHARS_PER_LINE, (m_sUpperText.length() % MAX_CHARS_PER_LINE));
					canvas.drawText(s, 20, 20 + i * 20, p);					
				}
				
				// display waiting text/icon
				if (m_sWaitingText.length() > 0)
				{
					p = new Paint();
					p.setColor(Color.LTGRAY);
					p.setStyle(Paint.Style.FILL_AND_STROKE);
					canvas.drawText(m_sWaitingText, 
							frame.left, frame.bottom + 20, p);
				}
			}
			
		} 
		catch (Exception ex)
		{
			Log.v(TAG, ex.toString());
		}
	}
}
