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

package com.uah.servicioocr;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

public class ServicioBackground extends Service {

	private Timer timer = new Timer();
	private static final long UPDATE_INTERVAL = 1000;
	public static ServiceUpdateUIListener UI_UPDATE_LISTENER;
	
	private int count = 0;
	
	public static void setUpdateListener(ServiceUpdateUIListener l) {
		UI_UPDATE_LISTENER = l;
	}
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		_startService();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		_shutdownService();
	}
	
	private void _startService() {
		timer.scheduleAtFixedRate(
			new TimerTask() {
				public void run() {
					count++;
					handler.sendEmptyMessage(0);
				}
			},
			0,
			UPDATE_INTERVAL);
	}
	
	private void _shutdownService() {
		if (timer != null) timer.cancel();
	}
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			UI_UPDATE_LISTENER.update(count);
		}
	};

}
