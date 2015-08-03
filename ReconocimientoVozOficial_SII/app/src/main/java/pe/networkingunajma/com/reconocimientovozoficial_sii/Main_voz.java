package pe.networkingunajma.com.reconocimientovozoficial_sii;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by user on 22/07/2015.
 */
public class Main_voz  extends Activity{

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

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(userNameEditText.getText().toString().equals("admin")&&
                        passwordEditText.getText().toString().equals("admin")
                        ){
                    Intent vozform= new Intent(Main_voz.this,MainActivity.class);
                    startActivity(vozform);

                }else
                {
                    Toast.makeText(getApplication(),"Usuario Incorrecto",Toast.LENGTH_SHORT).show();
                }

            }
        });



    }
}
