/*

# video experiments using mobile devices
# provided by volunteers
#
# (C)opyright, dividiti
# 2016
# BSD 3-clause license
#
# Powered by Collective Knowledge
# http://github.com/ctuning/ck

# Developers: Daniil Efremov and Grigori Fursin

*/

package openscience.crowdsource.video.experiments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.net.Uri;
import android.opengl.GLES10;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;

import org.ctuning.openme.openme;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MainActivity extends Activity {

    private static final int REQUEST_IMAGE_CAPTURE = 100;
    private static final int REQUEST_IMAGE_SELECT = 200;
    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final String PRELOAD_BUTTON = "Preload";


    String welcome = "This application let you participate in experiment crowdsourcing " +
            "to collaboratively solve complex problems! " +
            "Please, press 'Update' button to obtain shared scenarios such as " +
            "collaborative benchmarking, optimization and tuning of a popular Caffe CNN image recognition library!\n" +
            "NOTE: you should have an unlimited Internet since some scenario may require to download 300Mb+ code and datasets! " +
            "Also some anonymized statistics will be collected about your platform and code execution " +
            "(performance, accuracy, power consumption, cost, etc) at cknowledge.org/repo " +
            "to let the community improve algorithms for diverse hardware!\n\n ";

    String problem = "maybe be overloaded or down! Please report this problem to Grigori.Fursin@cTuning.org!";

    String path_opencl = "/system/vendor/lib/libOpenCL.so";

    String s_line = "====================================\n";
    String s_line1 = "------------------------------------\n";

    String url_sdk = "http://github.com/ctuning/ck";
    String url_about = "https://github.com/ctuning/ck/wiki/Advanced_usage_crowdsourcing";
    String url_stats = "http://cknowledge.org/repo/web.php?action=index&module_uoa=wfe&native_action=show&native_module_uoa=program.optimization&scenario=experiment.bench.caffe.mobile";
    String url_users = "http://cTuning.org/crowdtuning-timeline";

    String url_cserver = "http://cTuning.org/shared-computing-resources-json/ck.json";
    String repo_uoa = "upload";

    String s_b_start = "Update";
    String s_b_stop = "Exit";

    String s_thanks = "Thank you for participation!\n";

    int iterations = 1;
    static String email = "";

    EditText log = null;
    Button b_start = null;

    private Button btnSelect;

    private ProgressDialog dialog;
    private Bitmap bmp;

    private Uri fileUri;
    File sdcard = Environment.getExternalStorageDirectory();

    private static String[] IMAGENET_CLASSES;

    String cemail = "email.txt";
    String path1 = "ck-crowd";

    static String externalSDCardPath = "";
    static String externalSDCardOpensciencePath = "";

    static String pemail="";

    private AsyncTask crowdTask = null;
    Boolean running = false;

    static String pf_gpu = "";
    static String pf_gpu_vendor = "";

    static String path = ""; // Path to local tmp files
    static String path0 = "";

    static Button b_clean;
    EditText t_email;

    String fpack = "ck-pack.zip";

    String chmod744 = "/system/bin/chmod 744";

    boolean skip_freq_check = true;
    private GoogleApiClient client;
    private String takenPictureFilPath;

    /**
     * Create a file Uri for saving an image or video
     */
    private static Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private static File getOutputMediaFile(int type) {
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.

        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES), "Caffe-Android-Demo");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.


        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("MyCameraApp", "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File mediaFile;
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                    "IMG_" + timeStamp + ".jpg");
        } else {
            return null;
        }

        return mediaFile;
    }

    Camera camera;
    boolean isCameraStarted = false;
    SurfaceView surfaceView;
    SurfaceHolder surfaceHolder;

    Button startStopCam, recognize;

    private Boolean isPreloadRunning = false;
    private Boolean isPreloadMode = true;
    private Spinner scenarioSpinner;
    private ArrayAdapter<String> spinnerAdapter;
    private List<RecognitionScenario> recognitionScenarios = new LinkedList<>();


    /**
     * @return absolute path to image
     */
    private void captureImageFromCameraPreviewAndPredict() {
        camera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                try {
                    takenPictureFilPath = String.format("/sdcard/%d.jpg", System.currentTimeMillis());
                    FileOutputStream fos = new FileOutputStream(takenPictureFilPath);
                    fos.write(data);
                    fos.close();
                    stopCameraPreview();
                    predictImage(takenPictureFilPath);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void startCameraPreview() {
        if (!isCameraStarted) {
            try {
                camera = Camera.open();
                camera.setPreviewDisplay(surfaceHolder);
                camera.setDisplayOrientation(90);
                camera.startPreview();
                startStopCam.setText("Stop");
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            isCameraStarted = true;
        }
        return;
    }

    private void stopCameraPreview() {
        if (isCameraStarted) {
            if (camera != null) {
                camera.release();
            }
            camera = null;
            isCameraStarted = false;
            startStopCam.setText("Start");
        }
    }

    private RecognitionScenario getSelectedRecognitionScenario() {
        for (RecognitionScenario recognitionScenario : recognitionScenarios) {
            if(recognitionScenario.getTitle().equalsIgnoreCase(scenarioSpinner.getSelectedItem().toString())) {
                return recognitionScenario;
            }
        }
        return null;
    }
    /*************************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        startStopCam = (Button) findViewById(R.id.btn_start);
        startStopCam.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View arg0) {
                if (!isCameraStarted) {
                    startCameraPreview();
                } else {
                    stopCameraPreview();
                }
            }
        });
        recognize = (Button) findViewById(R.id.recognize);
        recognize.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                RecognitionScenario recognitionScenario = getSelectedRecognitionScenario();
                if ( recognitionScenario == null ) {
                    log.append(" Scenarios was not selected! Please select recognitions scenario first! \n");
                    return;
                }

                if (isCameraStarted) {
                    captureImageFromCameraPreviewAndPredict();
                    return;
                }

                // Record email - ugly - in many places - should be in a separate function ...
                String email1 = t_email.getText().toString().replaceAll("(\\r|\\n)", "");
                if (email1.equals("")) {
                    email1 = openme.gen_uid();
                }
                if (!email1.equals(email)) {
                    email = email1;
                    if (!save_one_string_file(pemail, email)) {
                        log.append("ERROR: can't write local configuration (" + pemail + "!");
                        return;
                    }
                    t_email.setText(email.trim());
                }

                // Call prediction
                predictImage(null);
            }
        });

        surfaceView = (SurfaceView)findViewById(R.id.surfaceView1);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
            }
        });

        b_start = (Button) findViewById(R.id.b_start);
        b_start.setText(s_b_start);

        scenarioSpinner = (Spinner)findViewById(R.id.s_scenario);
        spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, android.R.id.text1);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        scenarioSpinner.setAdapter(spinnerAdapter);


        b_start = (Button) findViewById(R.id.b_start);
        b_start.setText(s_b_start);

        t_email = (EditText) findViewById(R.id.t_email);

        btnSelect = (Button) findViewById(R.id.btnSelect);
        btnSelect.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                initPrediction();
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(i, REQUEST_IMAGE_SELECT);
            }
        });

        addListenersOnButtons();

        log = (EditText) findViewById(R.id.log);
        log.append(welcome);

        // Prepare dirs (possibly pre-load from config
        externalSDCardPath = File.separator + "sdcard";
        externalSDCardOpensciencePath = externalSDCardPath + File.separator + "openscience" + File.separator;

        pemail=externalSDCardOpensciencePath + cemail;

        // Getting local tmp path (for this app)
        File fpath = getFilesDir();
        path0 = fpath.toString();
        path = path0 + File.separator + path1;

        File fp = new File(path);
        if (!fp.exists()) {
            if (!fp.mkdirs()) {
                log.append("\nERROR: can't create directory for local tmp files!\n");
                return;
            }
        }

        /* Read email config */
        File externalSDCardFile = new File(externalSDCardOpensciencePath);
        if (!externalSDCardFile.exists()) {
            if (!externalSDCardFile.mkdirs()) {
                log.append("\nError creating dir (" + externalSDCardOpensciencePath + ") ...\n\n");
                return;
            }
        }

        email = read_one_string_file(pemail);
        if (email == null) email = "";
        if (!email.equals("")) {
            t_email.setText(email.trim());
        }

        // TBD: need to be redone!
        //Get GPU name via GLES10 **************************************************
/*        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(800);
                    MainActivity.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            pf_gpu_vendor = String.valueOf(GLES10.glGetString(GL10.GL_VENDOR));
                            if (pf_gpu_vendor.equals("null")) pf_gpu_vendor = "";

                            String x = String.valueOf(GLES10.glGetString(GL10.GL_RENDERER));
                            if (x.equals("null")) pf_gpu = "";
                            else pf_gpu = pf_gpu_vendor + " " + x;
                        }
                    });
                } catch (InterruptedException e) {
                    log.append("\nWarning: "+e.getMessage() + "\n\n");
                }
            }
        }).start();
*/
        preloadScenarioses();
    }
    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopCameraPreview();
    }
    /*************************************************************************/
    public void addListenersOnButtons() {
        Button b_sdk = (Button) findViewById(R.id.b_sdk);
        Button b_about = (Button) findViewById(R.id.b_about);
        b_clean = (Button) findViewById(R.id.b_clean);
        Button b_stats = (Button) findViewById(R.id.b_stats);
        Button b_users = (Button) findViewById(R.id.b_users);

        /*************************************************************************/
        b_sdk.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings({"unused", "unchecked"})
            @Override
            public void onClick(View arg0) {
                log.append("\nOpening " + url_sdk + " ...\n");

                Intent browserIntent =
                        new Intent(Intent.ACTION_VIEW, Uri.parse(url_sdk));

                startActivity(browserIntent);
            }
        });

        /*************************************************************************/
        b_about.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings({"unused", "unchecked"})
            @Override
            public void onClick(View arg0) {
                log.append("\nOpening " + url_about + " ...\n");

                Intent browserIntent =
                        new Intent(Intent.ACTION_VIEW, Uri.parse(url_about));

                startActivity(browserIntent);
            }
        });

        /*************************************************************************/
        b_clean.setOnClickListener(new View.OnClickListener() {
            public void onClick(View arg0) {
                log.setText("");
                log.setText("Cleaning local tmp files ...\n");
                if (!clean_log_tmp())
                    log.setText("  ERROR: Can't create directory " + path + " ...\n");
            }
        });

        /*************************************************************************/
        b_stats.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings({"unused", "unchecked"})
            @Override
            public void onClick(View arg0) {
                log.append("\nOpening " + url_stats + " ...\n");

                Intent browserIntent =
                        new Intent(Intent.ACTION_VIEW, Uri.parse(url_stats));

                startActivity(browserIntent);
            }
        });

        /*************************************************************************/
        b_users.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings({"unused", "unchecked"})
            @Override
            public void onClick(View arg0) {
                log.append("\nOpening " + url_users + " ...\n");

                Intent browserIntent =
                        new Intent(Intent.ACTION_VIEW, Uri.parse(url_users));

                startActivity(browserIntent);
            }
        });

        /*************************************************************************/
        b_start.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings({"unused", "unchecked"})
            @Override
            public void onClick(View arg0) {
                if (running) {
                    running = false;

                    b_start.setEnabled(false);

                    log.append(s_line);
                    log.append(s_thanks);
                    log.append("Interrupting crowd-tuning and quitting program ...");

                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            finish();
                            System.exit(0);
                        }
                    }, 1500);

                } else {
                    running = true;
                    b_start.setText(s_b_stop);
                    b_clean.setEnabled(false);

                    String email1 = t_email.getText().toString().replaceAll("(\\r|\\n)", "");
                    if (email1.equals("")) {
                        email1 = openme.gen_uid();
                    }
                    if (!email1.equals(email)) {
                        email = email1;
                        if (!save_one_string_file(pemail, email)) {
                            log.append("ERROR: can't write local configuration (" + pemail + "!");
                            return;
                        }
                        t_email.setText(email.trim());
                    }

//                    CheckBox c_continuous = (CheckBox) findViewById(R.id.c_continuous);
//                    if (c_continuous.isChecked()) iterations = -1;
                    isPreloadMode = false;
                    crowdTask = new RunCodeAsync().execute("");
                }
            }
        });
    }

    private void preloadScenarioses() {
        isPreloadRunning = true;
        spinnerAdapter.add("Preloading...");
        isPreloadMode = true;
        spinnerAdapter.clear();
        spinnerAdapter.notifyDataSetChanged();
        updateControlStatusPreloading(false);
        crowdTask = new RunCodeAsync().execute("");
    }

    private void updateControlStatusPreloading(boolean isEnable) {
        scenarioSpinner.setEnabled(isEnable);
        startStopCam.setEnabled(isEnable);
        recognize.setEnabled(isEnable);
        btnSelect.setEnabled(isEnable);
        b_start.setEnabled(isEnable);
    }

    /*************************************************************************/
    private void alertbox(String title, String mymessage) {
        // TODO Auto-generated method stub
        new AlertDialog.Builder(this)
                .setMessage(mymessage)
                .setTitle(title)
                .setCancelable(true)
                .setNeutralButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                            }
                        })
                .show();
    }

    /*************************************************************************/
    /* delete files and directories recursively */
    private void rmdirs(File start) {
        if (start.isDirectory()) {
            for (File leaf : start.listFiles())
                rmdirs(leaf);
        }
        start.delete();
    }

    /*************************************************************************/
    /* read one string file */
    private String read_one_string_file(String fname) {
        String ret = null;
        Boolean fail = false;

        BufferedReader fp = null;
        try {
            fp = new BufferedReader(new FileReader(fname));
        } catch (IOException ex) {
            fail = true;
        }

        if (!fail) {
            try {
                ret = fp.readLine();
            } catch (IOException ex) {
                fail = true;
            }
        }

        try {
            if (fp != null) fp.close();
        } catch (IOException ex) {
            fail = true;
        }

        return ret;
    }

    /*************************************************************************/
    /* read one string file */
    private boolean save_one_string_file(String fname, String text) {

        FileOutputStream o = null;
        try {
            o = new FileOutputStream(fname, false);
        } catch (FileNotFoundException e) {
            return false;
        }

        OutputStreamWriter oo = new OutputStreamWriter(o);

        try {
            oo.append(text);
            oo.flush();
            oo.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /*************************************************************************/
    /* exchange info with CK server */
    private JSONObject exchange_info_with_ck_server(JSONObject ii) {
        JSONObject r = null;

        try {
            r = openme.remote_access(ii);
        } catch (JSONException e) {
            try {
                r = new JSONObject();
                r.put("return", 32);
                r.put("error", "Error calling OpenME interface (" + e.getMessage() + ")");
            } catch (JSONException e1) {
                return null;
            }
            return r;
        }

        int rr = 0;
        if (!r.has("return")) {
            try {
                r = new JSONObject();
                r.put("return", 32);
                r.put("error", "Error obtaining key 'return' from OpenME output");
            } catch (JSONException e1) {
                return null;
            }
            return r;
        }

        try {
            Object rx = r.get("return");
            if (rx instanceof String) rr = Integer.parseInt((String) rx);
            else rr = (Integer) rx;
        } catch (JSONException e) {
            try {
                r = new JSONObject();
                r.put("return", 32);
                r.put("error", "Error obtaining key 'return' from OpenME output (" + e.getMessage() + ")");
            } catch (JSONException e1) {
                return null;
            }
            return r;
        }

        //Update return with integer
        try {
            r.put("return", rr);
        } catch (JSONException e) {
        }

        if (rr > 0) {
            String err = "";
            try {
                err = (String) r.get("error");
            } catch (JSONException e) {
                try {
                    r = new JSONObject();
                    r.put("return", 32);
                    r.put("error", "Error obtaining key 'error' from OpenME output (" + e.getMessage() + ")");
                } catch (JSONException e1) {
                    return null;
                }
            }
        }

        return r;
    }

    /*************************************************************************/
    boolean clean_log_tmp() {
        File fp = new File(path);
        rmdirs(fp);
        if (!fp.mkdirs()) {
            return false;
        }
        return true;
    }

    /*************************************************************************/
    /* get CPU frequencies */
    private List<Double[]> get_cpu_freqs() {
        int num_procs = 0;

        List<Double[]> cpu_list = new ArrayList<Double[]>();

        JSONObject r = null;

        String xpath = "";
        String val = "";
        double fval = 0;

        for (int i = 0; i < 1024; i++) {

            Double[] cpu = new Double[3];

            // Check if online
            xpath = "/sys/devices/system/cpu/cpu" + Integer.toString(i) + "/online";

            val = read_one_string_file(xpath);
            if (val == null) break;

            val = val.trim();
            fval = 0;
            if (!val.equals("")) fval = Float.parseFloat(val);

            cpu[0] = fval;

            // Check max freq
            xpath = "/sys/devices/system/cpu/cpu" + Integer.toString(i) + "/cpufreq/cpuinfo_max_freq";

            val = read_one_string_file(xpath);
            if (val == null) val = "";

            val = val.trim();
            fval = 0;
            if (!val.equals("")) fval = Float.parseFloat(val) / 1E3;

            cpu[1] = fval;

            // Check max freq
            xpath = "/sys/devices/system/cpu/cpu" + Integer.toString(i) + "/cpufreq/scaling_cur_freq";

            val = read_one_string_file(xpath);
            if (val == null) val = "";

            val = val.trim();
            fval = 0;
            if (!val.equals("")) fval = Float.parseFloat(val) / 1E3;

            cpu[2] = fval;

            //adding to final array
            cpu_list.add(cpu);
        }

        return cpu_list;
    }

    /*************************************************************************/
    private class RunCodeAsync extends AsyncTask<String, String, String> {

        /*************************************************************************/
        public String get_shared_computing_resource(String url) {
            String s = "";

            try {
                //Connect
                URL u = new URL(url);

                URLConnection urlc = u.openConnection();

                BufferedReader br = new BufferedReader(new InputStreamReader(urlc.getInputStream()));

                String line = "";
                while ((line = br.readLine()) != null)
                    s += line + '\n';
                br.close();
            } catch (Exception e) {
                return "ERROR: " + e.getMessage();
            }

            /* Trying to convert to dict from JSON */
            JSONObject a = null;

            try {
                a = new JSONObject(s);
            } catch (JSONException e) {
                return "ERROR: Can't convert string to JSON:\n" + s + "\n(" + e.getMessage() + ")\n";
            }

            /* For now just take default one, later add random or balancing */
            JSONObject rs = null;
            try {
                if (a.has("default"))
                    rs = (JSONObject) a.get("default");
                if (rs != null) {
                    if (rs.has("url"))
                        s = (String) rs.get("url");
                }
            } catch (JSONException e) {
                return "ERROR: Cant' convert string to JSON:\n" + s + "\n(" + e.getMessage() + ")\n";
            }

            if (s == null)
                s = "";
            else if (!s.endsWith("?"))
                s += "/?";

            return s;
        }

        /*************************************************************************/
        public void copy_bin_file(String src, String dst) throws IOException {
            File fin = new File(src);
            File fout = new File(dst);

            InputStream in = new FileInputStream(fin);
            OutputStream out = new FileOutputStream(fout);

            // Transfer bytes from in to out
            int l = 0;
            byte[] buf = new byte[16384];
            while ((l = in.read(buf)) > 0) out.write(buf, 0, l);

            in.close();
            out.close();
        }

        /*************************************************************************/
        protected void onPostExecute(String x) {
            b_start.setText(s_b_start);
            b_clean.setEnabled(true);
            running = false;
            isPreloadRunning = false;
        }

        /*************************************************************************/
        protected void onProgressUpdate(String... values) {
            if (values[0] != "") {
                log.append(values[0]);
                log.setSelection(log.getText().length());
            } else if (values[1] != "") {
                alertbox(values[1], values[2]);
            }
        }

        /*************************************************************************/
        @Override
        protected String doInBackground(String... arg0) {
            String pf_system = "";
            String pf_system_vendor = "";
            String pf_system_model = "";
            String pf_cpu = "";
            String pf_cpu_subname = "";
            String pf_cpu_features = "";
            String pf_cpu_abi = "";
            String pf_cpu_num = "";
            String pf_gpu_opencl = "";
            String pf_gpu_openclx = "";
            String pf_memory = "";
            String pf_os = "";
            String pf_os_short = "";
            String pf_os_long = "";
            String pf_os_bits = "32";

            JSONObject r = null;
            JSONObject ii = null;
            JSONObject ft_cpu = null;
            JSONObject ft_os = null;
            JSONObject ft_gpu = null;
            JSONObject ft_plat = null;
            JSONObject ft = null;
            JSONObject ftuoa = null;

//             Example of alert box for errors
//             publishProgress("", "Error", "Internal problem");

            /*********** Printing local tmp directory **************/
            publishProgress(s_line);
            publishProgress("Local tmp directory: " + path + "\n");
            publishProgress("User ID: " + email + "\n");

            /*********** Obtaining CK server **************/
            publishProgress(s_line);
            publishProgress("Obtaining list of public Collective Knowledge servers from " + url_cserver + " ...\n");
            String curl = get_shared_computing_resource(url_cserver);

            publishProgress("\n");

            if (curl.startsWith("ERROR")) {
                publishProgress(curl);
                publishProgress("\n");
                return null;
            } else {
                publishProgress("Public Collective Knowledge Server found:\n");
                publishProgress(curl);
                publishProgress("\n");
            }

            publishProgress("\n");
            publishProgress("Testing Collective Knowledge server ...\n");

            ii = new JSONObject();
            try {
                ii.put("remote_server_url", curl);
                ii.put("action", "test");
                ii.put("module_uoa", "program.optimization");
                ii.put("email", email);
                ii.put("type", "mobile-crowdtuning");
                ii.put("out", "json");
            } catch (JSONException e) {
                publishProgress("\nError with JSONObject ...\n\n");
                return null;
            }

            try {
                r = openme.remote_access(ii);
            } catch (JSONException e) {
                publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            int rr = 0;
            if (!r.has("return")) {
                publishProgress("\nError obtaining key 'return' from OpenME output ...\n\n");
                return null;
            }

            try {
                Object rx = r.get("return");
                if (rx instanceof String) rr = Integer.parseInt((String) rx);
                else rr = (Integer) rx;
            } catch (JSONException e) {
                publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            if (rr > 0) {
                String err = "";
                try {
                    err = (String) r.get("error");
                } catch (JSONException e) {
                    publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                publishProgress("\nProblem accessing CK server: " + err + "\n");
                publishProgress("\nPossible reason: " + problem + "\n");
                return null;
            }

            String status = "";
            try {
                status = (String) r.get("status");
            } catch (JSONException e) {
                publishProgress("\nError obtaining key 'string' from OpenME output (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            publishProgress("    " + status + "\n");

            /*********** Getting local information about platform **************/
            publishProgress(s_line);
            publishProgress("Detecting some of your platform features ...\n");

            //Get system info **************************************************
            try {
                r = openme.read_text_file_and_convert_to_json("/system/build.prop", "=", false, false);
            } catch (JSONException e) {
                publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            try {
                if ((Long) r.get("return") > 0)
                    publishProgress("\nProblem during OpenME: " + (String) r.get("error") + "\n\n");
            } catch (JSONException e) {
                publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            String model = "";
            String manu = "";

            JSONObject ra = null;

            try {
                ra = (JSONObject) r.get("dict");
            } catch (JSONException e) {
                publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            if (ra != null) {
                try {
                    model = (String) ra.get("ro.product.model");
                } catch (JSONException e) {
                    publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                try {
                    manu = (String) ra.get("ro.product.manufacturer");
                } catch (JSONException e) {
                    publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                if (!model.equals("") && !manu.equals(""))
                    if (model.toLowerCase().startsWith(manu.toLowerCase()))
                        model = model.substring(manu.length() + 1, model.length());

                if (manu.equals("") && !model.equals("")) manu = model;

                manu = manu.toUpperCase();
                model = model.toUpperCase();

                pf_system = manu;
                if (!model.equals("")) pf_system += ' ' + model;
                pf_system_model = model;
                pf_system_vendor = manu;
            }

            //Get processor info **************************************************
            //It's not yet working properly on heterogeneous CPU, like big/little
            //So results can't be trusted and this part should be improved!
            try {
                r = openme.read_text_file_and_convert_to_json("/proc/cpuinfo", ":", false, false);
            } catch (JSONException e) {
                publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            try {
                if ((Long) r.get("return") > 0)
                    publishProgress("\nProblem during OpenME: " + (String) r.get("error") + "\n\n");
            } catch (JSONException e) {
                publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            String processor_file = null;

            try {
                processor_file = (String) r.get("file_as_string");
            } catch (JSONException e) {
                publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                return null;
            }
            if (processor_file == null) processor_file = "";

            try {
                ra = (JSONObject) r.get("dict");
            } catch (JSONException e) {
                publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            if (ra != null) {
                String x1 = null;
                String x2 = null;
                String x3 = null;
                String x4 = null;

                try {
                    x1 = (String) ra.get("Processor");
                } catch (JSONException e) {
                }
                try {
                    x2 = (String) ra.get("model_name");
                } catch (JSONException e) {
                }
                try {
                    x3 = (String) ra.get("Hardware");
                } catch (JSONException e) {
                }
                try {
                    x4 = (String) ra.get("Features");
                } catch (JSONException e) {
                }

                if (x2 != null && !x2.equals("")) pf_cpu_subname = x2;
                else if (x1 != null && !x1.equals("")) pf_cpu_subname = x1;

                if (x3 != null) pf_cpu = x3;
                if (x4 != null) pf_cpu_features = x4;

                // On Intel-based Android
                if (pf_cpu.equals("") && !pf_cpu_subname.equals(""))
                    pf_cpu = pf_cpu_subname;

                // If CPU is empty, possibly new format
                if (pf_cpu.equals("")) {
                    String ic1 = "";
                    String ic2 = "";
                    String ic3 = "";
                    String ic4 = "";
                    String ic5 = "";

                    try {
                        ic1 = (String) ra.get("CPU implementer");
                    } catch (JSONException e) {
                    }

                    try {
                        ic2 = (String) ra.get("CPU architecture");
                    } catch (JSONException e) {
                    }

                    try {
                        ic3 = (String) ra.get("CPU variant");
                    } catch (JSONException e) {
                    }

                    try {
                        ic4 = (String) ra.get("CPU part");
                    } catch (JSONException e) {
                    }

                    try {
                        ic5 = (String) ra.get("CPU revision");
                    } catch (JSONException e) {
                    }

                    pf_cpu += ic1 + "-" + ic2 + "-" + ic3 + "-" + ic4 + "-" + ic5;
                }

                // If CPU is still empty, send report to CK to fix ...
                if (pf_cpu.equals("")) {
                    publishProgress("\nPROBLEM: we could not detect CPU name and features on your device :( ! Please, report to authors!\n\n");

                    ii = new JSONObject();
                    try {
                        ii.put("remote_server_url", curl);
                        ii.put("action", "problem");
                        ii.put("module_uoa", "program.optimization");
                        ii.put("email", email);
                        ii.put("problem", "mobile_crowdtuning_cpu_name_empty");
                        ii.put("problem_data", processor_file);
                        ii.put("out", "json");
                    } catch (JSONException e) {
                        publishProgress("\nError with JSONObject ...\n\n");
                        return null;
                    }

                    try {
                        r = openme.remote_access(ii);
                    } catch (JSONException e) {
                        publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                        return null;
                    }

                    rr = 0;
                    if (!r.has("return")) {
                        publishProgress("\nError obtaining key 'return' from OpenME output ...\n\n");
                        return null;
                    }

                    try {
                        Object rx = r.get("return");
                        if (rx instanceof String) rr = Integer.parseInt((String) rx);
                        else rr = (Integer) rx;
                    } catch (JSONException e) {
                        publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
                        return null;
                    }

                    if (rr > 0) {
                        String err = "";
                        try {
                            err = (String) r.get("error");
                        } catch (JSONException e) {
                            publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                            return null;
                        }

                        publishProgress("\nProblem accessing CK server: " + err + "\n");
                        return null;
                    }

                    return null;
                }
            }

            //Get memory info **************************************************
            try {
                r = openme.read_text_file_and_convert_to_json("/proc/meminfo", ":", false, false);
            } catch (JSONException e) {
                publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            try {
                if ((Long) r.get("return") > 0 && (Long) r.get("return") != 16)
                    publishProgress("\nProblem during OpenME: " + (String) r.get("error") + "\n\n");
            } catch (JSONException e) {
                publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            try {
                ra = (JSONObject) r.get("dict");
            } catch (JSONException e) {
                publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            if (ra != null) {
                String mem_tot = "";

                try {
                    mem_tot = (String) ra.get("memtotal");
                } catch (JSONException e) {
                }

                if (mem_tot == null || mem_tot.equals(""))
                    try {
                        mem_tot = (String) ra.get("MemTotal");
                    } catch (JSONException e) {
                    }

                if (mem_tot != null && !mem_tot.equals("")) {
                    int i = mem_tot.indexOf(' ');
                    if (i > 0) {
                        String mem1 = mem_tot.substring(0, i).trim();
                        String mem2 = "1";
                        if (mem1.length() > 3) mem2 = mem1.substring(0, mem1.length() - 3);
                        pf_memory = mem2 + " MB";
                    }
                }
            }

            //Get available processors and frequencies **************************************************
            List<Double[]> cpus = get_cpu_freqs();
            Double[] cpu = null;

            int cpu_num = cpus.size();
            pf_cpu_num = Integer.toString(cpu_num);

            pf_cpu_abi = Build.CPU_ABI; //System.getProperty("os.arch"); - not exactly the same!

            //Get OS info **************************************************
            pf_os = "Android " + android.os.Build.VERSION.RELEASE;

            try {
                r = openme.read_text_file_and_convert_to_json("/proc/version", ":", false, false);
            } catch (JSONException e) {
                publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            try {
                if ((Long) r.get("return") > 0)
                    publishProgress("\nProblem during OpenME: " + (String) r.get("error") + "\n\n");
            } catch (JSONException e) {
                publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            try {
                pf_os_long = (String) r.get("file_as_string");
            } catch (JSONException e) {
            }
            if (pf_os_long == null) pf_os_long = "";
            else {
                pf_os_long = pf_os_long.trim();

                pf_os_short = pf_os_long;

                int ix = pf_os_long.indexOf(" (");

                if (ix >= 0) {
                    int ix1 = pf_os_long.indexOf("-");
                    if (ix1 >= 0 && ix1 < ix) ix = ix1;
                    pf_os_short = pf_os_long.substring(0, ix).trim();
                }
            }

            int ix = pf_os_long.indexOf("_64");
            if (ix > 0) pf_os_bits = "64";

            //Get OpenCL info **************************************************
            File fopencl = new File(path_opencl);
            long lfopencl = fopencl.length();

            if (lfopencl > 0) {
                pf_gpu_opencl = "libOpenCL.so - found (" + Long.toString(lfopencl) + " bytes)";
                pf_gpu_openclx = "yes";
            } else {
                pf_gpu_opencl = "libOpenCL.so - not found";
                pf_gpu_openclx = "no";
            }

            //Print **************************************************
            publishProgress("\n");
            publishProgress("PLATFORM:   " + pf_system + "\n");
            publishProgress("* VENDOR:   " + pf_system_vendor + "\n");
            publishProgress("* MODEL:   " + pf_system_model + "\n");
            publishProgress("OS:   " + pf_os + "\n");
            publishProgress("* SHORT:   " + pf_os_short + "\n");
            publishProgress("* LONG:   " + pf_os_long + "\n");
            publishProgress("* BITS:   " + pf_os_bits + "\n");
            publishProgress("MEMORY:   " + pf_memory + "\n");
            publishProgress("CPU:   " + pf_cpu + "\n");
            publishProgress("* SUBNAME:   " + pf_cpu_subname + "\n");
            publishProgress("* ABI:   " + pf_cpu_abi + "\n");
            publishProgress("* FEATURES:   " + pf_cpu_features + "\n");
            publishProgress("* CORES:   " + pf_cpu_num + "\n");
            for (int i = 0; i < cpu_num; i++) {
                String x = "    " + Integer.toString(i) + ") ";
                cpu = cpus.get(i);
                double x0 = cpu[0];
                ;
                double x1 = cpu[1];
                double x2 = cpu[2];

                if (x0 == 0) x += "offline";
                else x += "online; " + Double.toString(x2) + " of " + Double.toString(x1) + " MHz";

                publishProgress(x + "\n");
            }
            publishProgress("GPU:   " + pf_gpu + "\n");
            publishProgress("* VENDOR:   " + pf_gpu_vendor + "\n");
            publishProgress("* OPENCL:   " + pf_gpu_opencl + "\n");

            //Delay program for 1 sec
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }

            //Communicate with CK **************************************************
            JSONObject j_os, j_cpu, j_gpu, j_sys;

            String j_os_uid = "";
            String j_cpu_uid = "";
            String j_gpu_uid = "";
            String j_sys_uid = "";

            publishProgress(s_line);
            publishProgress("Exchanging info about your platform with CK server to retrieve latest meta for crowdtuning ...");

            ii = new JSONObject();
            ft = new JSONObject();

            // OS ******
            publishProgress("\n    Exchanging OS info ...\n");

            try {
                ft_os = new JSONObject();

                ft_os.put("name", pf_os);
                ft_os.put("name_long", pf_os_long);
                ft_os.put("name_short", pf_os_short);
                ft_os.put("bits", pf_os_bits);

                ft.put("features", ft_os);

                ii.put("remote_server_url", curl);
                ii.put("action", "exchange");
                ii.put("module_uoa", "platform");
                ii.put("sub_module_uoa", "platform.os");
                ii.put("data_name", pf_os);
                ii.put("repo_uoa", repo_uoa);
                ii.put("all", "yes");
                ii.put("dict", ft);
                ii.put("out", "json");
            } catch (JSONException e) {
                publishProgress("\nError with JSONObject ...\n\n");
                return null;
            }

            j_os = exchange_info_with_ck_server(ii);
            try {
                Object rx = j_os.get("return");
                if (rx instanceof String) rr = Integer.parseInt((String) rx);
                else rr = (Integer) rx;
            } catch (JSONException e) {
                publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            if (rr > 0) {
                String err = "";
                try {
                    err = (String) j_os.get("error");
                } catch (JSONException e) {
                    publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                publishProgress("\nProblem accessing CK server: " + err + "\n");
                return null;
            } else {
                String found = "";
                try {
                    found = (String) j_os.get("found");
                } catch (JSONException e) {
                }

                if (found.equals("yes")) {
                    try {
                        j_os_uid = (String) j_os.get("data_uid");
                    } catch (JSONException e) {
                    }

                    String x = "         Already exists";
                    if (!j_os_uid.equals("")) x += " (CK UOA=" + j_os_uid + ")";
                    x += "!\n";
                    publishProgress(x);
                }
            }

            // GPU ******
            if (!pf_gpu.equals("")) {
                publishProgress("    Exchanging GPU info ...\n");

                try {
                    ft_gpu = new JSONObject();

                    ft_gpu.put("name", pf_gpu);
                    ft_gpu.put("vendor", pf_gpu_vendor);

                    ft.put("features", ft_gpu);

                    ii.put("remote_server_url", curl);
                    ii.put("action", "exchange");
                    ii.put("module_uoa", "platform");
                    ii.put("sub_module_uoa", "platform.gpu");
                    ii.put("data_name", pf_gpu);
                    ii.put("repo_uoa", repo_uoa);
                    ii.put("all", "yes");
                    ii.put("dict", ft);
                    ii.put("out", "json");
                } catch (JSONException e) {
                    publishProgress("\nError with JSONObject ...\n\n");
                    return null;
                }

                j_gpu = exchange_info_with_ck_server(ii);
                try {
                    Object rx = j_gpu.get("return");
                    if (rx instanceof String) rr = Integer.parseInt((String) rx);
                    else rr = (Integer) rx;
                } catch (JSONException e) {
                    publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                if (rr > 0) {
                    String err = "";
                    try {
                        err = (String) j_gpu.get("error");
                    } catch (JSONException e) {
                        publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                        return null;
                    }

                    publishProgress("\nProblem accessing CK server: " + err + "\n");
                    return null;
                } else {
                    String found = "";
                    try {
                        found = (String) j_gpu.get("found");
                    } catch (JSONException e) {
                    }

                    if (found.equals("yes")) {
                        try {
                            j_gpu_uid = (String) j_gpu.get("data_uid");
                        } catch (JSONException e) {
                        }

                        String x = "         Already exists";
                        if (!j_gpu_uid.equals("")) x += " (CK UOA=" + j_gpu_uid + ")";
                        x += "!\n";
                        publishProgress(x);
                    }
                }
            }

            // CPU ******
            publishProgress("    Exchanging CPU info ...\n");

            try {
                ft_cpu = new JSONObject();

                ft_cpu.put("name", pf_cpu);
                ft_cpu.put("sub_name", pf_cpu_subname);
                ft_cpu.put("cpu_features", pf_cpu_features);
                ft_cpu.put("cpu_abi", pf_cpu_abi);
                ft_cpu.put("num_proc", pf_cpu_num);

                JSONObject freq_max = new JSONObject();
                for (int i = 0; i < cpu_num; i++) {
                    String x = "    " + Integer.toString(i) + ") ";
                    cpu = cpus.get(i);
                    double x1 = cpu[1];
                    if (x1 != 0)
                        freq_max.put(Integer.toString(i), x1);
                }
                ft_cpu.put("max_freq", freq_max);

                ft.put("features", ft_cpu);

                ii.put("remote_server_url", curl);
                ii.put("action", "exchange");
                ii.put("module_uoa", "platform");
                ii.put("sub_module_uoa", "platform.cpu");
                ii.put("data_name", pf_cpu);
                ii.put("repo_uoa", repo_uoa);
                ii.put("all", "yes");
                ii.put("dict", ft);
                ii.put("out", "json");
            } catch (JSONException e) {
                publishProgress("\nError with JSONObject ...\n\n");
                return null;
            }

            j_cpu = exchange_info_with_ck_server(ii);
            try {
                Object rx = j_cpu.get("return");
                if (rx instanceof String) rr = Integer.parseInt((String) rx);
                else rr = (Integer) rx;
            } catch (JSONException e) {
                publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            if (rr > 0) {
                String err = "";
                try {
                    err = (String) j_cpu.get("error");
                } catch (JSONException e) {
                    publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                publishProgress("\nProblem accessing CK server: " + err + "\n");
                return null;
            } else {
                String found = "";
                try {
                    found = (String) j_cpu.get("found");
                } catch (JSONException e) {
                }

                if (found.equals("yes")) {
                    try {
                        j_cpu_uid = (String) j_cpu.get("data_uid");
                    } catch (JSONException e) {
                    }

                    String x = "         Already exists";
                    if (!j_cpu_uid.equals("")) x += " (CK UOA=" + j_cpu_uid + ")";
                    x += "!\n";
                    publishProgress(x);
                }
            }

            // Platform ******
            publishProgress("    Exchanging platform info ...\n");

            try {
                ft_plat = new JSONObject();

                ft_plat.put("name", pf_system);
                ft_plat.put("vendor", pf_system_vendor);
                ft_plat.put("model", pf_system_model);

                ft.put("features", ft_plat);

                ii.put("remote_server_url", curl);
                ii.put("action", "exchange");
                ii.put("module_uoa", "platform");
                ii.put("sub_module_uoa", "platform");
                ii.put("data_name", pf_system);
                ii.put("repo_uoa", repo_uoa);
                ii.put("all", "yes");
                ii.put("dict", ft);
                ii.put("out", "json");
            } catch (JSONException e) {
                publishProgress("\nError with JSONObject ...\n\n");
                return null;
            }

            j_sys = exchange_info_with_ck_server(ii);
            try {
                Object rx = j_sys.get("return");
                if (rx instanceof String) rr = Integer.parseInt((String) rx);
                else rr = (Integer) rx;
            } catch (JSONException e) {
                publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            if (rr > 0) {
                String err = "";
                try {
                    err = (String) j_sys.get("error");
                } catch (JSONException e) {
                    publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                publishProgress("\nProblem accessing CK server: " + err + "\n");
                return null;
            } else {
                String found = "";
                try {
                    found = (String) j_sys.get("found");
                } catch (JSONException e) {
                }

                if (found.equals("yes")) {
                    try {
                        j_sys_uid = (String) j_sys.get("data_uid");
                    } catch (JSONException e) {
                    }

                    String x = "         Already exists";
                    if (!j_sys_uid.equals("")) x += " (CK UOA=" + j_sys_uid + ")";
                    x += "!\n";
                    publishProgress(x);
                }
            }

            //Delay program for 1 sec
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }

            // Sending request to CK server to obtain available scenarios
             /*######################################################################################################*/
            publishProgress("\n    Sending request to CK server to obtain available collaborative experiment scenarios for your mobile device ...\n\n");

            ii = new JSONObject();
            try {
                ft = new JSONObject();

                ft.put("cpu", ft_cpu);
                ft.put("cpu_uid", j_cpu_uid);
                ft.put("cpu_uoa", j_cpu_uid);

                ft.put("gpu", ft_gpu);
                ft.put("gpu_uid", j_gpu_uid);
                ft.put("gpu_uoa", j_gpu_uid);

                // Need to tell CK server if OpenCL present
                // for collaborative OpenCL optimization using mobile devices
                JSONObject ft_gpu_misc = new JSONObject();
                ft_gpu_misc.put("opencl_lib_present", pf_gpu_openclx);
                ft.put("gpu_misc", ft_gpu_misc);

                ft.put("os", ft_os);
                ft.put("os_uid", j_os_uid);
                ft.put("os_uoa", j_os_uid);

                ft.put("platform", ft_plat);
                ft.put("platform_uid", j_sys_uid);
                ft.put("platform_uoa", j_sys_uid);

                ii.put("remote_server_url", curl);
                ii.put("action", "get");
                ii.put("module_uoa", "experiment.scenario.mobile");
                ii.put("email", email);
                ii.put("platform_features", ft);
                ii.put("out", "json");
            } catch (JSONException e) {
                publishProgress("\nError with JSONObject ...\n\n");
                return null;
            }

            try {
                r = openme.remote_access(ii);
            } catch (JSONException e) {
                publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            rr = 0;
            if (!r.has("return")) {
                publishProgress("\nError obtaining key 'return' from OpenME output ...\n\n");
                return null;
            }

            try {
                Object rx = r.get("return");
                if (rx instanceof String) rr = Integer.parseInt((String) rx);
                else rr = (Integer) rx;
            } catch (JSONException e) {
                publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            if (rr > 0) {
                String err = "";
                try {
                    err = (String) r.get("error");
                } catch (JSONException e) {
                    publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                    return null;
                }

                publishProgress("\nProblem at CK server: " + err + "\n");
                return null;
            }

            try {
                JSONArray scenarios = r.getJSONArray("scenarios");
                if (scenarios.length() == 0) {
                    publishProgress("\nUnfortunately, no scenarios found for your device ...\n\n");
                    return null;
                }

//                 String externalSDCard = System.getenv("SECONDARY_STORAGE"); //this is correct way to detect externalSDCard but there is problem with permissions ls -l
//                 String externalSDCard = Environment.getExternalStorageDirectory().getAbsolutePath(); // actually  this is internal emulated sdcard storage
//                 String externalSDCardOpensciencePath = externalSDCard + File.separator + "openscience" + File.separator;
//                String externalSDCardPath = File.separator + "sdcard";
//                String externalSDCardOpensciencePath = externalSDCardPath + File.separator + "openscience" + File.separator;
                String localAppPath = path + File.separator + "openscience" + File.separator;

                File externalSDCardFile = new File(externalSDCardOpensciencePath);
                if (!externalSDCardFile.exists()) {
                    if (!externalSDCardFile.mkdirs()) {
                        publishProgress("\nError creating dir (" + externalSDCardOpensciencePath + ") ...\n\n");
                        return null;
                    }
                }

                String libPath = null;
                String executablePath = null;
                String imageFilePath = null;
                String imageFileName = null;
                for (int i = 0; i < scenarios.length(); i++) {
                    JSONObject scenario = scenarios.getJSONObject(i);
                    final String module_uoa = scenario.getString("module_uoa");
                    final String dataUID = scenario.getString("data_uid");
                    final String data_uoa = scenario.getString("data_uoa");

                    scenario.getJSONObject("search_dict");
                    scenario.getString("ignore_update");
                    scenario.getString("search_string");
                    JSONObject meta = scenario.getJSONObject("meta");

                    if (!isPreloadRunning && getSelectedRecognitionScenario() == null) {
                        continue;
                    }

                    JSONArray files = meta.getJSONArray("files");
                    for (int j = 0; j < files.length(); j++) {
                        JSONObject file = files.getJSONObject(j);
                        String fileName = file.getString("filename");
                        String fileDir = externalSDCardOpensciencePath + file.getString("path");
                        File fp = new File(fileDir);
                        if (!fp.exists()) {
                            if (!fp.mkdirs()) {
                                publishProgress("\nError creating dir (" + fileDir + ") ...\n\n");
                                return null;
                            }
                        }

                        final String targetFilePath = fileDir + File.separator + fileName;
                        String finalTargetFilePath = targetFilePath;
                        String finalTargetFileDir = fileDir;
                        String url = file.getString("url");
                        String md5 = file.getString("md5");
                        if (!isPreloadMode && downloadFileAndCheckMd5(
                                url,
                                targetFilePath,
                                md5,
                                new ProgressPublisher() {
                                    @Override
                                    public void publish(int percent) {
                                        publishProgress("\n Downloading file " + targetFilePath + " : " + percent + "%\n\n");
                                    }

                                    @Override
                                    public void println(String text) {
                                        publishProgress("\n" + text + "\n");
                                    }
                                })) {
                            String copyToAppSpace = null;
                            try {
                                copyToAppSpace = file.getString("copy_to_app_space");
                            } catch (JSONException e) {
                                // copyToAppSpace is not mandatory
                            }
                            if (copyToAppSpace != null && copyToAppSpace.equalsIgnoreCase("yes")) {
                                String fileAppDir = localAppPath + file.getString("path");
                                File appfp = new File(fileAppDir);
                                if (!appfp.exists()) {
                                    if (!appfp.mkdirs()) {
                                        publishProgress("\nError creating dir (" + fileAppDir + ") ...\n\n");
                                        return null;
                                    }
                                }

                                final String targetAppFilePath = fileAppDir + File.separator + fileName;
                                try {
                                    copy_bin_file(targetFilePath, targetAppFilePath);
                                    finalTargetFileDir = fileAppDir;
                                    finalTargetFilePath = targetAppFilePath;
                                    publishProgress("\nFile " + targetFilePath + " sucessfully copied to " + targetAppFilePath + "\n\n");
                                } catch (IOException e) {
                                    e.printStackTrace();
                                    publishProgress("\nError copying file " + targetFilePath + " to " + targetAppFilePath + " ...\n\n");
                                    return null;
                                }

                            }

                            String executable = null;
                            try {
                                executable = file.getString("executable");
                            } catch (JSONException e) {
                                // executable is not mandatory
                            }
                            if (executable != null && executable.equalsIgnoreCase("yes")) {
                                if (finalTargetFilePath.contains(".so")) { //todo add parameter image=yes to response like for executable
                                    libPath = finalTargetFileDir;
                                } else {
                                    executablePath = finalTargetFileDir;
                                }
                                String[] chmodResult = openme.openme_run_program(chmod744 + " " + finalTargetFilePath, null, finalTargetFileDir);
                                if (chmodResult[0].isEmpty() && chmodResult[1].isEmpty() && chmodResult[2].isEmpty()) {
                                    publishProgress("\nFile " + finalTargetFilePath + " sucessfully set as executable \n\n");
                                } else {
                                    publishProgress("\nError seting  file " + targetFilePath + " as executable ...\n\n");
                                    return null;
                                }
                            }
                        }


                        if (finalTargetFilePath.contains("jpg")) { //todo add parameter image=yes to response like for executable
                            imageFilePath = finalTargetFilePath;
                            imageFileName = fileName;
                        }

                    }

                    RecognitionScenario selectedRecognitionScenario = getSelectedRecognitionScenario();
                    if (selectedRecognitionScenario != null && selectedRecognitionScenario.getImagePath() != null) {
                        imageFilePath = selectedRecognitionScenario.getImagePath();
                        imageFileName = selectedRecognitionScenario.getImagePath(); //todo
                    }

                    if (isPreloadMode) {
                        final RecognitionScenario recognitionScenario = new RecognitionScenario();
                        recognitionScenario.setModuleUOA(module_uoa);
                        recognitionScenario.setDataUOA(data_uoa);
                        recognitionScenario.setRawJSON(scenario);
                        recognitionScenario.setImagePath(imageFilePath);
                        recognitionScenario.setTitle(meta.getString("title"));
                        recognitionScenarios.add(recognitionScenario);

                        publishProgress("\n Preloaded scenario info:  " + recognitionScenario.toString() + "\n\n");

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                //stuff that updates ui
                                spinnerAdapter.add(recognitionScenario.getTitle());
                                updateControlStatusPreloading(true);
                                spinnerAdapter.notifyDataSetChanged();
                            }
                        });
                        continue;
                    }

                    if (imageFilePath == null) {
                        publishProgress("\nError image file path does not set.\n\n");
                        return null;
                    }

                    if (libPath == null) {
                        publishProgress("\nError lib path does not set.\n\n");
                        return null;
                    }

                    String scenarioCmd = meta.getString("cmd");

                    String[] scenarioEnv = {
                            "CT_REPEAT_MAIN=" + String.valueOf(1),
                            "LD_LIBRARY_PATH=" + libPath + ":$LD_LIBRARY_PATH",
                    };

                    scenarioCmd = scenarioCmd.replace("$#local_path#$", externalSDCardPath + File.separator);
                    scenarioCmd = scenarioCmd.replace("$#image#$", imageFilePath);

                    //TBD: should make a loop and aggregate timing in one list
                    //In the future we may read json output and aggregate it too (openMe)
                    publishProgress("\nRecognition started (statistical repetition: 1 out of 3)...\n\n");
                    long startTime = System.currentTimeMillis();
                    String[] recognitionREsult = openme.openme_run_program(scenarioCmd, scenarioEnv, executablePath); //todo fix ck response cmd value: add appropriate path to executable from according to path value at "file" json
                    long processingTime1 = System.currentTimeMillis() - startTime;
                    if (recognitionREsult[0] != null && !recognitionREsult[0].trim().equals("")) {
                        publishProgress("\nRecognition errors: " + recognitionREsult[0] + "\n\n");
                    }
                    String recognitionResultText = recognitionREsult[1];

                    publishProgress("\nRecognition time 1: " + processingTime1 + " ms \n");

                    // 2nd - tbd: move to loop
                    publishProgress("\nRecognition started (statistical repetition: 2 out of 3)...\n\n");
                    startTime = System.currentTimeMillis();
                    recognitionREsult = openme.openme_run_program(scenarioCmd, scenarioEnv, executablePath); //todo fix ck response cmd value: add appropriate path to executable from according to path value at "file" json
                    long processingTime2 = System.currentTimeMillis() - startTime;
                    if (recognitionREsult[0] != null && !recognitionREsult[0].trim().equals("")) {
                        publishProgress("\nRecognition errors: " + recognitionREsult[0] + "\n\n");
                    }

                    publishProgress("\nRecognition time 2: " + processingTime1 + " ms \n");

                    // 3rd - tbd: move to loop
                    publishProgress("\nRecognition started (statistical repetition: 3 out of 3)...\n\n");
                    startTime = System.currentTimeMillis();
                    recognitionREsult = openme.openme_run_program(scenarioCmd, scenarioEnv, executablePath); //todo fix ck response cmd value: add appropriate path to executable from according to path value at "file" json
                    long processingTime3 = System.currentTimeMillis() - startTime;
                    if (recognitionREsult[0] != null && !recognitionREsult[0].trim().equals("")) {
                        publishProgress("\nRecognition errors: " + recognitionREsult[0] + "\n\n");
                    }

                    publishProgress("\nRecognition time 3: " + processingTime1 + " ms \n\n");
                    publishProgress("\nRecognition result:\n " + recognitionResultText + "\n\n");

//                     publishProgress("\nRecognition warnings:"+recognitionREsult[2]+"\n\n"); //todo now it prints text like: ANDROID_ROOT not set, it's better do not display it

//                    RecognitionResult recognitionResult = new RecognitionResult();
//                    recognitionResult.setCrowdUID(dataUID);  // I'm not sure about it
//                    recognitionResult.setProcessingTime(processingTime);
//                    recognitionResult.setImageFileName(imageFileName);
                    // todo load image frnm  imageFilePath and get image siza
//                    recognitionResult.setImageHeight(3024);  //todo remove hardcoded values from loaded image
//                    recognitionResult.setImageWidth(4032);   //todo remove hardcoded values from loaded image

                    // todo implement publishRecognitionResultToserver and uncomment
                    publishProgress("Submitting results and unexpected behavior (if any) to Collective Knowledge Aggregator ...\n");

                    JSONObject publishREquest = new JSONObject();
                    try {
                        JSONObject results = new JSONObject();
                        results.put("time1", processingTime1); // TBD: should make a list
                        results.put("time2", processingTime2); // TBD: should make a list
                        results.put("time3", processingTime3); // TBD: should make a list
                        results.put("prediction", recognitionResultText);
                        results.put("image_width", 3024); // Fix that with real width
                        results.put("image_height", 4032); // Fix that with real height

                        publishREquest.put("remote_server_url", curl);
                        publishREquest.put("out", "json");
                        publishREquest.put("action", "process");
                        publishREquest.put("module_uoa", "experiment.bench.caffe.mobile");

                        publishREquest.put("email", email);
                        publishREquest.put("crowd_uid", dataUID);

                        publishREquest.put("platform_features", ft);
                        publishREquest.put("raw_results", results);

                    } catch (JSONException e) {
                        publishProgress("\nError with JSONObject ...\n\n");
                        return null;
                    }

                    JSONObject response;
                    try {
                        response = openme.remote_access(publishREquest);
                    } catch (JSONException e) {
                        publishProgress("\nError calling OpenME interface (" + e.getMessage() + ") ...\n\n");
                        return null;
                    }

                    int responseCode = 0;
                    if (!response.has("return")) {
                        publishProgress("\nError obtaining key 'return' from OpenME output ...\n\n");
                        return null;
                    }

                    try {
                        Object rx = response.get("return");
                        if (rx instanceof String) responseCode = Integer.parseInt((String) rx);
                        else responseCode = (Integer) rx;
                    } catch (JSONException e) {
                        publishProgress("\nError obtaining key 'return' from OpenME output (" + e.getMessage() + ") ...\n\n");
                        return null;
                    }

                    if (responseCode > 0) {
                        String err = "";
                        try {
                            err = (String) response.get("error");
                        } catch (JSONException e) {
                            publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                            return null;
                        }

                        publishProgress("\nProblem accessing CK server: " + err + "\n");
                        return null;
                    }

                    status = "";

                    try {
                        status = (String) response.get("status");
                    } catch (JSONException e) {
                        publishProgress("\nError obtaining key 'status' from OpenME output (" + e.getMessage() + ") ...\n\n");
                    }

                    publishProgress('\n' + status + '\n');

                    //Delay program for 1 sec
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ex) {
                    }
                }

            } catch (JSONException e) {
                publishProgress("\nError obtaining key 'error' from OpenME output (" + e.getMessage() + ") ...\n\n");
                return null;
            }

            //Delay program for 1 sec
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
            }

            if (isPreloadRunning) {
                publishProgress(s_line);
                publishProgress("Finished pre-loading shared scenarios for crowdsourcing!\n\n");
                publishProgress("Crowd engine is READY!\n");
            }
            return null;
        }


        class RecognitionResult {
            private long processingTime;
            private String crowdUID;
            private String recognitionResultText;
            private String imageFileName;
            private int imageHeight;
            private int imageWidth;

            public long getProcessingTime() {
                return processingTime;
            }

            public void setProcessingTime(long processingTime) {
                this.processingTime = processingTime;
            }

            public String getCrowdUID() {
                return crowdUID;
            }

            public void setCrowdUID(String crowdUID) {
                this.crowdUID = crowdUID;
            }

            public String getRecognitionResultText() {
                return recognitionResultText;
            }

            public void setRecognitionResultText(String recognitionResultText) {
                this.recognitionResultText = recognitionResultText;
            }

            public String getImageFileName() {
                return imageFileName;
            }

            public void setImageFileName(String imageFileName) {
                this.imageFileName = imageFileName;
            }

            public int getImageHeight() {
                return imageHeight;
            }

            public void setImageHeight(int imageHeight) {
                this.imageHeight = imageHeight;
            }

            public int getImageWidth() {
                return imageWidth;
            }

            public void setImageWidth(int imageWidth) {
                this.imageWidth = imageWidth;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                RecognitionResult that = (RecognitionResult) o;

                if (processingTime != that.processingTime) return false;
                if (imageHeight != that.imageHeight) return false;
                if (imageWidth != that.imageWidth) return false;
                if (crowdUID != null ? !crowdUID.equals(that.crowdUID) : that.crowdUID != null)
                    return false;
                if (recognitionResultText != null ? !recognitionResultText.equals(that.recognitionResultText) : that.recognitionResultText != null)
                    return false;
                return imageFileName != null ? imageFileName.equals(that.imageFileName) : that.imageFileName == null;

            }

            @Override
            public int hashCode() {
                int result = (int) (processingTime ^ (processingTime >>> 32));
                result = 31 * result + (crowdUID != null ? crowdUID.hashCode() : 0);
                result = 31 * result + (recognitionResultText != null ? recognitionResultText.hashCode() : 0);
                result = 31 * result + (imageFileName != null ? imageFileName.hashCode() : 0);
                result = 31 * result + imageHeight;
                result = 31 * result + imageWidth;
                return result;
            }
        }

    }

    // Recognize image ********************************************************************************
    private void predictImage(String imgPath) {

        if (imgPath != null) {
            bmp = BitmapFactory.decodeFile(imgPath);
            log.append("Processing image path: " + imgPath + "\n");
            log.append("Processing image height: " + String.valueOf(bmp.getHeight()) + "\n");
            log.append("Processing image width: " + String.valueOf(bmp.getWidth()) + "\n");
        } else {
            log.append("Image does not provided \n");
        }

        isPreloadMode = false;
        getSelectedRecognitionScenario().setImagePath(imgPath);
        crowdTask = new RunCodeAsync().execute("");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((requestCode == REQUEST_IMAGE_CAPTURE || requestCode == REQUEST_IMAGE_SELECT) && resultCode == RESULT_OK) {
            String imgPath;

            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                imgPath = fileUri.getPath();
            } else {
                Uri selectedImage = data.getData();
                String[] filePathColumn = {MediaStore.Images.Media.DATA};
                Cursor cursor = MainActivity.this.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
                cursor.moveToFirst();
                int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                imgPath = cursor.getString(columnIndex);
                cursor.close();
                predictImage(imgPath);
            }
        } else {
            btnSelect.setEnabled(true);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    private void initPrediction() {
        btnSelect.setEnabled(false);
    }

    private boolean downloadFileAndCheckMd5(String urlString, String localPath, String md5, ProgressPublisher progressPublisher) {
        try {
            String existedlocalPathMD5 = fileToMD5(localPath);
            if (existedlocalPathMD5 != null && existedlocalPathMD5.equalsIgnoreCase(md5)) {
                return true;
            }

            int BUFFER_SIZE = 1024;
            byte data[] = new byte[BUFFER_SIZE];
            int count;

            URL url = new URL(urlString);
            URLConnection conection = url.openConnection();
            conection.connect();

            int lenghtOfFile = conection.getContentLength();
            InputStream input = new BufferedInputStream(url.openStream());
            OutputStream output = new FileOutputStream(localPath);


            long total = 0;
            int progressPercent = 0;
            int prevProgressPercent = 0;
            while ((count = input.read(data)) != -1) {
                total += count;
                if (lenghtOfFile > 0) {
                    progressPercent = (int) ((total * 100) / lenghtOfFile);
                }
                if (progressPercent != prevProgressPercent) {
                    progressPublisher.publish(progressPercent);
                    prevProgressPercent = progressPercent;
                }
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();


            String gotMD5 = fileToMD5(localPath);
            if (!gotMD5.equalsIgnoreCase(md5)) {
                progressPublisher.println("ERROR: MD5 is not satisfied, please try again.");
                return false;
            } else {
                progressPublisher.println("File succesfully downloaded from " + urlString + " to local files " + localPath);
                return true;
            }
        } catch (FileNotFoundException e) {
            progressPublisher.println("ERROR: downloading from " + urlString + " to local files " + localPath + " " + e.getLocalizedMessage());
            return false;
        } catch (IOException e) {
            progressPublisher.println("ERROR: downloading from " + urlString + " to local files " + localPath + " " + e.getLocalizedMessage());
            return false;
        } catch (Throwable e) {
            e.printStackTrace();
            progressPublisher.println("ERROR: downloading from " + urlString + " to local files " + localPath + " " + e.getLocalizedMessage());
            return false;
        }
    }

    public static String fileToMD5(String filePath) {
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            MessageDigest digest = MessageDigest.getInstance("MD5");
            int numRead = 0;
            while (numRead != -1) {
                numRead = inputStream.read(buffer);
                if (numRead > 0)
                    digest.update(buffer, 0, numRead);
            }
            byte[] md5Bytes = digest.digest();
            return convertHashToString(md5Bytes);
        } catch (Exception e) {
            return null;
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private static String convertHashToString(byte[] md5Bytes) {
        String returnVal = "";
        for (int i = 0; i < md5Bytes.length; i++) {
            returnVal += Integer.toString((md5Bytes[i] & 0xff) + 0x100, 16).substring(1);
        }
        return returnVal.toUpperCase();
    }

    interface ProgressPublisher {
        void publish(int percent);

        void println(String text);
    }


    class RecognitionScenario {
        private String imagePath;
        private String dataUOA;
        private String moduleUOA;
        private String title;
        private JSONObject rawJSON; //todo move out to file


        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }

        public String getDataUOA() {
            return dataUOA;
        }

        public void setDataUOA(String dataUOA) {
            this.dataUOA = dataUOA;
        }

        public String getModuleUOA() {
            return moduleUOA;
        }

        public void setModuleUOA(String moduleUOA) {
            this.moduleUOA = moduleUOA;
        }

        public JSONObject getRawJSON() {
            return rawJSON;
        }

        public void setRawJSON(JSONObject rawJSON) {
            this.rawJSON = rawJSON;
        }

        @Override
        public String toString() {
            return "RecognitionScenario{" +
                    "dataUOA='" + dataUOA + '\'' +
                    ", moduleUOA='" + moduleUOA + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            RecognitionScenario that = (RecognitionScenario) o;

            if (dataUOA != null ? !dataUOA.equals(that.dataUOA) : that.dataUOA != null)
                return false;
            return moduleUOA != null ? moduleUOA.equals(that.moduleUOA) : that.moduleUOA == null;

        }

        @Override
        public int hashCode() {
            int result = dataUOA != null ? dataUOA.hashCode() : 0;
            result = 31 * result + (moduleUOA != null ? moduleUOA.hashCode() : 0);
            return result;
        }
    }
}
