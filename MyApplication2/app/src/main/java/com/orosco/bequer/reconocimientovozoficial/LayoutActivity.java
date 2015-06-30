package com.orosco.bequer.reconocimientovozoficial;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * Created by user on 29/06/2015.
 */
public class LayoutActivity extends Activity {

    private EditText userEditText;
    private EditText paswordEditText;

    private Button loginButton;
    @Override
    public void onCreate (Bundle saveInstance){
    super.onCreate(saveInstance);
    setContentView(R.layout.table_layout);

        userEditText=(EditText) findViewById(R.id.editTextUser);
        paswordEditText=(EditText) findViewById(R.id.editTextPasword);

        loginButton = (Button) findViewById(R.id.buttonLogin);

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(userEditText.getText().toString().equals("admin") ||
                        paswordEditText.getText().toString().equals("admin")){

        finish();
                }
                else {
                    Log.d("Login", "wrog user/pasword");
                }
            }
        });
    }
}
