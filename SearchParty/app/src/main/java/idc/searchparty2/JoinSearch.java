package idc.searchparty2;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class JoinSearch extends AppCompatActivity {
    public static final String MESSAGE_NAME_JOIN = "NAME_Join";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_search);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    public void joinButtonOnPress(View view){
        Intent intent = new Intent(this, MapsActivity.class);
        EditText nickField = (EditText) findViewById(R.id.nicknamePrompt);
        String[] data = {"1", nickField.getText().toString()};
        intent.putExtra(MESSAGE_NAME_JOIN, data);
        startActivity(intent);
    }
}
