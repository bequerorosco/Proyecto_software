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

import com.itwizard.mezzofanti.Mezzofanti;
import com.uah.explorador.ExploradorArchivos;
import com.uah.servicioocr.R;
//import com.itwizard.mezzofanti.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ServicioOCR extends Activity implements ServiceUpdateUIListener {
	
	TextView text;
	EditText textoRuta;

	Context context=this;
	private String ruta = "";
	private static final int NOTIF_ALERTA_ID = 1;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main2);
        
        textoRuta = (EditText) findViewById(R.id.ruta);
        Bundle recibido = getIntent().getExtras();
        String rutaImagen = "";
        if(recibido !=null)
        {
        	rutaImagen = recibido.getString("Ruta");
        	ruta=rutaImagen;
        	textoRuta.setText(rutaImagen);
        }
        
        Button exploreButton = (Button) findViewById(R.id.explorar);
        
       exploreButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				startExplorer();
			}
		});
       
       Button sendButton = (Button) findViewById(R.id.enviar);
              
       sendButton.setOnClickListener(new View.OnClickListener() {

           public void onClick(View view) {
        	Editable rutaImagen = textoRuta.getText();
        	if (rutaImagen.toString().contains(".JPG")||rutaImagen.toString().contains(".jpg")){
        		AlertDialog.Builder confirmacion = new AlertDialog.Builder(context);
        		confirmacion.setMessage(R.string.confirmation)
        		.setCancelable(false)
        		.setPositiveButton(R.string.affirmation, new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int id) {
        			//Acción del botón de confirmación
        			/** Inicio notificación */
        			//Enviamos notificación cuando comienza la traducción
        			String ns = Context.NOTIFICATION_SERVICE;
        			NotificationManager notManager = (NotificationManager) getSystemService(ns);
        			//Configuramos la notificación
        			int icono = com.uah.servicioocr.R.drawable.processing;
        			CharSequence textoEstado = "Traducción OCR en proceso";
        			long hora = System.currentTimeMillis();
        			 
        			Notification notif =  new Notification(icono, textoEstado, hora);
        			Context contexto = getApplicationContext();
        			CharSequence titulo = "Traducción OCR en proceso";
        			CharSequence descripcion = "Se está realizando la traducción";
        	        			
        			Intent intent = new Intent(contexto,
        				    ServicioOCR.class);
        			
        			PendingIntent contIntent = PendingIntent.getActivity(
        			    contexto, 0, intent, 0);
        			 
        			notif.setLatestEventInfo(
        			    contexto, titulo, descripcion, contIntent);
        			
        			//AutoCancel: cuando se pulsa la notificaión ésta desaparece
        			notif.flags |= Notification.FLAG_AUTO_CANCEL;
        			 
        			//Añadir sonido, vibración y luces
        			//notif.defaults |= Notification.DEFAULT_SOUND;
        			//notif.defaults |= Notification.DEFAULT_VIBRATE;
        			notif.defaults |= Notification.DEFAULT_LIGHTS;

        			//Enviar notificación
        			notManager.notify(NOTIF_ALERTA_ID, notif);
        			/**Fin notificación*/
        			finish();
        			startBackgroundService(); 
        		}
        		})
        		.setNegativeButton(R.string.negation, new DialogInterface.OnClickListener() {
        		public void onClick(DialogInterface dialog, int id) {
        		//Acción al pulsar el botón "No"
        		dialog.cancel();
        		}
        		});
        		AlertDialog alert = confirmacion.create();
        		//Título para el cuadro de advertencia
        		alert.setTitle(R.string.send_confirmation);
        		alert.show();       		       		
        	}else{
        		new AlertDialog.Builder(context)
                .setTitle(R.string.no_seleccion1)
                .setMessage(R.string.no_seleccion2)
                .setNeutralButton(R.string.cancelar, new DialogInterface.OnClickListener(){
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                }).create().show();
        	}
           }
         
       });
       
        MiServicio.setUpdateListener(this);
        ServicioBackground.setUpdateListener(this);
        
        
    }

    private void startExplorer(){
    	Intent svc = new Intent(ServicioOCR.this, ExploradorArchivos.class);
    	finish();
    	startActivity(svc);
    }
    
    private void startBackgroundService() {
    	Intent svc = new Intent(ServicioOCR.this, Mezzofanti.class);
    	svc.putExtra("Ruta", ruta);
    	finish();
    	startActivity(svc);
    }

	@Override
	public void update(int count) {
		text.setText(R.string.count + count);
	}
    
}