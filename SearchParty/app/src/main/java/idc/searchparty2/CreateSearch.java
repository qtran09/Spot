package idc.searchparty2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class CreateSearch extends AppCompatActivity {
    public static final String MESSAGE_NAME = "NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_search);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void createButtonOnPress(View view){
        Intent intent = new Intent(this, MapsActivity.class);
        EditText nameTextField = (EditText) findViewById(R.id.namePrompt);
        EditText descTextField = (EditText) findViewById(R.id.descPrompt);
        EditText nickTextField = (EditText) findViewById(R.id.nickPrompt);
        String[] name_desc = {
                "0",
                nameTextField.getText().toString(),
                descTextField.getText().toString(),
                nickTextField.getText().toString()
        };

        intent.putExtra(MESSAGE_NAME, name_desc);
        startActivity(intent);
    }
}
