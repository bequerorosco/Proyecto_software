package pe.networkingunajma.com.reconocimientovozoficial_sii;



import android.app.Activity;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.util.Locale;

import static pe.networkingunajma.com.reconocimientovozoficial_sii.R.menu.menu_main;


public class MainActivity_leer extends Activity implements OnInitListener {

    //Motor de voz
    private TextToSpeech tts;
    //
    private EditText texto;
    private EditText editText;
    private EditText editText2;
    private EditText editText3;

    private Button button1;
    private Button button2;
    private Button button3;
    private Button button4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_leer);

        //=====================================================
        tts = new TextToSpeech( this, this );
        texto = (EditText) findViewById(R.id.texto );
        editText2 = (EditText) findViewById(R.id.editText2 );
        editText = (EditText) findViewById(R.id.editText );
        editText3 = (EditText) findViewById(R.id.editText3 );
        button1 = (Button) findViewById(R.id.btn1 );
        button2 = (Button) findViewById(R.id.btn2 );
        button3 = (Button) findViewById(R.id.btn3 );
        button4 = (Button) findViewById(R.id.button );

        // Habla normal
        button1.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                dime_algo( texto.getText().toString() );
            }

        });
        //Habla rapido
        button2.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                dime_algo(editText.getText().toString());
            }

        });

        button3.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                dime_algo(editText2.getText().toString());
            }

        });
        button4.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                dime_algo(editText3.getText().toString());
            }

        });
        //=====================================================
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(menu_main, menu);
        return true;
    }

    //Inicia TTS
    @Override
    public void onInit(int status) {

        if ( status == TextToSpeech.SUCCESS ) {

            //coloca lenguaje por defecto en el celular, en nuestro caso el lenguaje es aspa?ol ;)
            int result = tts.setLanguage( Locale.getDefault() );

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                button1.setEnabled(false);
                button2.setEnabled(false);
                button3.setEnabled(false);
                Log.e("TTS", "This Language is not supported");
            } else {
                button1.setEnabled(true);
                button2.setEnabled(true);
                button3.setEnabled(true);
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }

    /**
     * metodo para convertir texto a voz
     //* @param  texto
     * */
    private void dime_algo( String texto ) {
        tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null);
    }

    //Cuando se cierra la aplicacion se destruye el TTS
    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

}//-->fin clas
