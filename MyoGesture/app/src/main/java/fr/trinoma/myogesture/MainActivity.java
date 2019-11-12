package fr.trinoma.myogesture;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;

import fr.trinoma.myogesture.delsys.DelsysApi;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static MyoGestureRecognition gestureRecognition;
    public static FileProvider fileProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_devices, R.id.navigation_record, R.id.navigation_detect)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        // Where logged data will be stored
        fileProvider = FileProvider.makeOnSdCard(this);

        DelsysApi delsysApi = DelsysApi.getInstance();

        gestureRecognition = new MyoGestureRecognition();

        // TODO wrapper return success
        // I had to remove byte order marks from the files provided by Delsys
        try {
            delsysApi.initialize(
                    readLicenceAsset("PublicKey.lic"),
                    readLicenceAsset("License.lic"));
            gestureRecognition.useDataAcquisitionApi(delsysApi);
        } catch (IOException e) {
            Log.e(TAG, "Unable to read PublicKey.lic and License.lic from assets");
        }

        checkAppPermissions();
    }

    // Put license files in app/src/main/assets/
    String readLicenceAsset(String fileName) throws IOException {
        StringBuilder sb = new StringBuilder();
        InputStream is = getAssets().open(fileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        String str;
        while ((str = br.readLine()) != null) {
            sb.append(str);
        }
        br.close();
        // Had to remove byte order mark from Delsys license files
        return sb.toString().replace("\ufeff", "");
    }

    void checkAppPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }
    }
}
