package pe.networkingunajma.com.reconocimientovozoficial_sii;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by user on 22/07/2015.
 */
public class MainVoz  extends Activity{

    private EditText userNameEditText;
    private EditText passwordEditText;

    private Button loginButton;
    @Override
    public void onCreate (Bundle savedInstance){
        super.onCreate(savedInstance);
        setContentView(R.layout.main_voz);

        userNameEditText = (EditText) findViewById(R.id.editTextUsername);
        passwordEditText= (EditText) findViewById(R.id.editTextPassword);

        loginButton=(Button)findViewById(R.id.buttonLogin);





    }
}
