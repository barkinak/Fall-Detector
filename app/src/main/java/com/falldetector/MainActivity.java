package com.falldetector;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.provider.ContactsContract;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private Context context = this;
    SensorListener sensorListener;
    Boolean running = false;
    Location location;
    String alert_message = "http://www.google.com/maps/place/";
    LocationManager mLocationManager;
    AlertDialog alertDialog;

    static final int PICK_CONTACT = 1;
    double longitude;
    double latitude;
    Boolean location_found = false;
    Boolean fall_detected = false;

    double[] buffer = new double[256];
    double[] signal = new double[256];
    int[] signal2 = new int[128];
    ArrayList<String> contactList = new ArrayList<>();
    ArrayList<String> phoneList = new ArrayList<>();

    double[] meyer_scale_function =
            {0.0080, -0.0116, 0.0052, -0.0003, 0.0186, -0.0267, -0.0085, 0.0095, 0.0633,
                    -0.0556, -0.0656, 0.0155, 0.2186, -0.0829, -0.5931, 1.0162, -0.5931,
                    -0.0829, 0.2186, 0.0155, -0.0656, -0.0556, 0.0633, 0.0095, -0.0085,
                    -0.0267, 0.0186, -0.0003, 0.0052, -0.0116, 0.0080, -0.0049};

    int[] feature_fall = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0,
            1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0};

    private GraphView mGraph;
    private ListView mEmergencyList;
    private ImageButton mContactButton, mSettingsButton;
    private Switch mSwitch;
    private LineGraphSeries<DataPoint> mSeries;
    private CustomAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);
        initialize();
        check_fall_thread();
        check_location_enabled();
        start_location_thread();
        generate_empty_list();
        adapter = new CustomAdapter(context, contactList, phoneList);
        mEmergencyList.setAdapter(adapter);
        action_listener();
    }

    private void initialize() {
        mEmergencyList = (ListView) findViewById(R.id.emergency_list);
        mGraph = (GraphView) findViewById(R.id.graph);
        mSeries = new LineGraphSeries<DataPoint>();
        mSeries.setColor(getResources().getColor(R.color.cursor));
        sensorListener = new SensorListener(context);
        mSwitch = (Switch) findViewById(R.id.real_time_switch);
        mContactButton = (ImageButton) findViewById(R.id.contact_button);
        mSettingsButton = (ImageButton) findViewById(R.id.settings_button);

        // Graph parameters:
        mGraph.setBackgroundColor(getResources().getColor(R.color.white));
        //mGraph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.NONE);// It will remove the background grids
        mGraph.getGridLabelRenderer().setHorizontalLabelsVisible(false);// remove horizontal x labels and line
        //mGraph.getGridLabelRenderer().setVerticalLabelsVisible(false);
        mGraph.getGridLabelRenderer().setNumHorizontalLabels(256); // Partition x-axis
        mGraph.getGridLabelRenderer().setNumVerticalLabels(6); // Partition y-axis
        mGraph.getViewport().setMinY(0);
        mGraph.getViewport().setMaxY(35);
        //mGraph.getViewport().setXAxisBoundsManual(true); // Update indexes of x-axis manually (not automatic)
        mGraph.getViewport().setYAxisBoundsManual(true);

        NumberFormat nf = NumberFormat.getInstance();
        nf.setMinimumFractionDigits(1);
        nf.setMinimumIntegerDigits(1);
        mGraph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(nf, nf));

        // Actionbar:
        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.setTitleTextColor(getResources().getColor(R.color.blue));

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        alertDialog = new AlertDialog.Builder(this).create();
    }

    private void check_fall_thread() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(1000);
                        if (fall_detected) {
                            fall_detected = false;

                            // Separate Thread to update GUI:
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    generate_alert();
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    private void check_location_enabled() {
        if (!isLocationEnabled()) {
            showAlert();
        }
    }

    private void start_location_thread() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (!location_found) {
                        Thread.sleep(10000);
                        location = getLastKnownLocation();
                        get_location(location);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    private void generate_empty_list() {
        for (int i = 0; i < 10; i++) {
            contactList.add("Contact Name");
            phoneList.add("Phone Number: ----/---/--/--");
        }
    }

    private void action_listener() {
        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
                if (!isChecked) {
                    running = false;
                } else {
                    running = true;
                }
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            while (running) {
                                Thread.sleep(1000);
                                buffer = sensorListener.getBuffer();
                                signal = convolve(buffer, meyer_scale_function);
                                signal2 = decimation(signal, 2);
                                fall_detected = isFall(signal2, feature_fall);
                                Log.d(TAG, "fall detected: " + fall_detected);

                                // Seperate Thread to update GUI:
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        update_graph(buffer);
                                    }
                                });
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                };
                thread.start();
            }
        });

        mContactButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                read_contact();
            }
        });

        mSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }

    public void read_contact() {
        try {
            Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
            startActivityForResult(intent, PICK_CONTACT);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onActivityResult(int reqCode, int resultCode, Intent data) {
        super.onActivityResult(reqCode, resultCode, data);

        switch (reqCode) {
            case (PICK_CONTACT):
                if (resultCode == Activity.RESULT_OK) {
                    Uri contactData = data.getData();
                    Cursor c = context.getContentResolver().query(contactData, null, null, null, null);

                    if (c.moveToFirst()) {
                        String id = c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
                        String hasPhone = c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

                        if (hasPhone.equalsIgnoreCase("1")) {
                            Cursor phones = getContentResolver().query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + id,
                                    null, null);
                            phones.moveToFirst();
                            String cNumber = phones.getString(phones.getColumnIndex("data1"));
                            phoneList.add(0, cNumber);
                        }
                        String name = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                        contactList.add(0, name);
                        adapter.notifyDataSetChanged();
                    }
                }
                break;
        }
    }

    private void alert_SMS(ArrayList<String> phoneList) {
        for (int i = 0; i < phoneList.size(); i++) {
            if (phoneList.get(i).length() > 5 && phoneList.get(i).length() < 20) {
                sendSMS(phoneList.get(i), alert_message, latitude, longitude);
            }
        }
    }

    private void sendSMS(String phoneNumber, String message, double lat, double lon) {
        message = message + lat + "," + lon;
        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, null, null);
    }

    private void generate_alert() {
        alertDialog.setTitle("Fall Detected!");
        alertDialog.setMessage("00:10");
        alertDialog.show();

        new CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                alertDialog.setMessage("00:" + (millisUntilFinished / 1000));
            }

            @Override
            public void onFinish() {
                alertDialog.dismiss();
                alert_SMS(phoneList);
            }
        }.start();
    }

    private void get_location(Location location) {
        if (location == null) {
        } else {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        }
    }

    private Location getLastKnownLocation() {
        List<String> providers = mLocationManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {
            @SuppressLint("MissingPermission") Location l = mLocationManager.getLastKnownLocation(provider);

            if (l == null) {
                continue;
            }
            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                bestLocation = l;
            }
        }
        if (bestLocation == null) {
            return null;
        }
        return bestLocation;
    }

    private boolean isLocationEnabled() {
        return mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " +
                        "use this app")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        Intent viewIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(viewIntent);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    }
                });
        dialog.show();
    }

    public void update_graph(double[] buffer) {
        mGraph.removeAllSeries();
        mSeries = new LineGraphSeries<DataPoint>(generateDataArray(buffer));
        mGraph.addSeries(mSeries);
    }

    // Generate data points for the graph:
    public static DataPoint[] generateDataArray(double[] data_array) {
        int array_length = data_array.length;
        DataPoint[] values = new DataPoint[array_length];
        for (int i = 0; i < array_length; i++) {
            double y = data_array[i];
            DataPoint v = new DataPoint(i, y);
            values[i] = v;
        }
        return values;
    }

    // Convolve input signal with filter coefficients:
    public double[] convolve(double[] input, double[] coef) {
        int length = input.length;
        int length2 = coef.length;
        double[] output = new double[length];
        for (int i = 0; i < length; i++) {
            output[i] = 0;
            for (int j = 0; j < length2; j++) {
                if (i > j) {
                    output[i] += input[i - j] * coef[j];
                }
            }
        }
        return output;
    }

    public int[] decimation(double[] data, int D) {
        int L = data.length;
        int[] signal = new int[L / D];
        for (int i = 0; i < L / D; i++) {
            if (Math.abs((data[i * D])) > 5)
                signal[i] = 1;
            else
                signal[i] = 0;
        }
        return signal;
    }

    public boolean isFall(int[] feature, int[] signal) {
        int Sitting = 0;
        int falling = 0;

        for (int i = 0; i < signal.length; i++) {
            Sitting = Sitting + signal[i];
            falling = falling + Math.abs(signal[i] - feature[i]);
        }
        if (Sitting <= falling)
            return false;
        else
            return true;
    }

}