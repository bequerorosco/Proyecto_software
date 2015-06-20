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

package com.uah.explorador;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.uah.servicioocr.R;
//import com.itwizard.mezzofanti.R;
import com.uah.servicioocr.ServicioOCR;

public class ExploradorArchivos extends ListActivity {
	private List<String> elementos = null;
	
	private void rellenarConElRaiz() {
        rellenar(new File("/").listFiles());
    } 
	
	protected void onListItemClick(ListView l, View v, int position, long id) {
        int IDFilaSeleccionada = position;
        if (IDFilaSeleccionada==0){
            rellenarConElRaiz();
        } else {
            final File archivo = new File(elementos.get(IDFilaSeleccionada));
            if (archivo.isDirectory())
                rellenar(archivo.listFiles());
             else
            	 if (esImagen(archivo.getName()))
            	 {
            		 new AlertDialog.Builder(this)
                     .setTitle(R.string.imagen_seleccionada)
                     .setMessage(archivo.getName())
                     .setNeutralButton(R.string.aceptar, new DialogInterface.OnClickListener(){
                         public void onClick(DialogInterface dialog, int whichButton) {
                             volverPrincipal(archivo.getAbsolutePath());
                         }
                     }).create().show();
            	 }else
                 new AlertDialog.Builder(this)
                 .setTitle(R.string.formato_incorrecto)
                 .setMessage(R.string.no_imagen)
                 .setNeutralButton(R.string.aceptar, new DialogInterface.OnClickListener(){
                     public void onClick(DialogInterface dialog, int whichButton) {
                         /* No hacemos nada */
                     }
                 }).create().show();
        }
    }
	
	private void rellenar(File[] archivos) {
        elementos = new ArrayList<String>();
        elementos.add(getString(R.string.raiz));
        for( File archivo: archivos)
            elementos.add(archivo.getPath());
        
        ArrayAdapter<String> listaArchivos= new ArrayAdapter<String>(this, R.layout.fila, elementos);
        setListAdapter(listaArchivos);
    }
	
	public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.listado);
        rellenarConElRaiz();
	}
	
	public boolean esImagen(String nombreArchivo){
		if (nombreArchivo.contains(".JPG")||nombreArchivo.contains(".jpg"))
			return true;
			else
		return false;
	}
	
	private void volverPrincipal(String ruta){
    	Intent svc = new Intent(ExploradorArchivos.this, ServicioOCR.class);
    	svc.putExtra("Ruta", ruta);
    	finish();
    	startActivity(svc);
    }

}
