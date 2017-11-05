package idc.searchparty2;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

/**
 *  This class serves as the main class for the help screen, which is initialized when the user
 *  presses the small help button in the bottom right of the landing page. This page displays
 *  essential instructions regarding the usage of Spot.
 */
public class HelpScreen extends AppCompatActivity {

    /**
     *  This method is called when the object is initialized. It initializes the .xml file that
     *  contains the GUI layout of the page and references to appropriate text instructions. Also
     *  initializes the Android back button which takes the user back to the landing page specified
     *  in the ../../manifests/AndroidManifest.xml file.
     *
     * @param savedInstanceState    Default argument passed into the <code> onCreate() </code>
     *                              method. Used to restore the object to a previous state using
     *                              the data in the <code> Bundle </code> object.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_screen);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }
}
