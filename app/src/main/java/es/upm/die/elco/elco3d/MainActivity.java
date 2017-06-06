package es.upm.die.elco.elco3d;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
//import android.util.Log;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private final String upLoadServerUri = "/subir.php";
    private final String finServerUri = "/final.php";
    private String dirBase;
    private int nFoto;

    private SurfaceView preview = null;
    private Button boton = null;
    private SurfaceHolder previewHolder = null;
    private Camera camera = null;
    private boolean inPreview = false;
    private boolean cameraConfigured = false;
    private View.OnClickListener empezar, hacer, parar;
    private int maxFotos = 64;
    private boolean ejecutando;
    private EditText cambio;
    private int tamano = 0;

    Camera.PictureCallback mandar = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            ByteArrayInputStream datos = new ByteArrayInputStream(data);
            UploadToServer mandarFoto = new UploadToServer();
            mandarFoto.execute(datos);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        preview = (SurfaceView)findViewById(R.id.preview);
        //boton = (Button)findViewById(R.id.bot);
        previewHolder = preview.getHolder();
        previewHolder.addCallback(surfaceCallback);
        previewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        ejecutando = false;
        dirBase = "http://192.168.1.140";
        /*empezar = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPreview();
                boton.setText("Tomar foto");
                boton.setOnClickListener(hacer);
            }
        };
        hacer = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enfocar();
                boton.setText("Parar");
                boton.setOnClickListener(parar);
            }
        };
        parar = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopPreview();
                boton.setText("Empezar");
                boton.setOnClickListener(empezar);
            }
        };
        boton.setOnClickListener(empezar);*/
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        MenuInflater conversor = getMenuInflater();
        conversor.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int seleccion = item.getItemId();
        switch (seleccion){
            case R.id.play:
                if (!ejecutando) {
                    nFoto = 0;
                    startPreview();
                    enfocar();
                    item.setTitle("Stop");
                } else {
                    stopPreview();
                    startPreview();
                    item.setTitle("Play");
                }
                ejecutando = !ejecutando;
                break;
            case R.id.direccion:
                if (!ejecutando){
                    cambio = new EditText(this);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Cambiar direccion de la Raspberry").setView(cambio);
                    builder.setMessage("Dirección (sin http://)");
                    builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dirBase = "http://" + cambio.getText().toString();
                        }
                    });
                    builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int i) {
                        }
                    });
                    builder.setCancelable(false);
                    builder.create().show();
                }
                break;
            case R.id.foto:
                if (!ejecutando){
                    cambio = new EditText(this);
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Número de fotos a realizar").setView(cambio);
                    builder.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            maxFotos = Integer.parseInt(cambio.getText().toString());
                        }
                    });
                    builder.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int i) {
                        }
                    });
                    builder.setCancelable(false);
                    builder.create().show();
                }
                break;
        }
        return false;
    }

    @Override
    public void onResume() {
        //Log.d("CAMARA", "Estamos en el resume");
        super.onResume();

        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.CAMERA}, 50);
            }
            releaseCameraAndPreview();
            camera = Camera.open();
            Camera.Parameters params = camera.getParameters();

// Check what resolutions are supported by your camera
            List<Camera.Size> sizes = params.getSupportedPictureSizes();

// Iterate through all available resolutions and choose one.
// The chosen resolution will be stored in mSize.
            Camera.Size mSize = null;
            for (Camera.Size size : sizes) {
                Log.i("CAMARA", "Available resolution: "+size.width+" "+size.height);
                mSize = size;
                if (((double)size.width / size.height) == ((double)16/9) || ((int)(((double)size.width / size.height)*10)) == (16)) {
                    tamano = size.width;
                    if (tamano < 3000)
                        break;
                }
            }
            /*if (tamano == 0){
                for (Camera.Size size : sizes) {
                    //Log.i("CAMARA", "Available resolution: "+size.width+" "+size.height);
                    mSize = size;
                    tamano = size.width;
                    break;
                }
            }*/
            //Log.i("CAMARA", "Chosen resolution: "+mSize.width+" "+mSize.height);
            params.setPictureSize(mSize.width, mSize.height);
            camera.setParameters(params);
        } catch (Exception e) {
            //Log.e(getString(R.string.app_name), "failed to open Camera");
            e.printStackTrace();
        }
        startPreview();
    }

    private void releaseCameraAndPreview() {
        if (camera != null) {
            camera.release();
            camera = null;
        }
    }

    @Override
    public void onPause() {
        if (camera != null) {
            if (inPreview) {
                camera.stopPreview();
            }

            camera.release();
            camera = null;
            inPreview = false;
        }

        super.onPause();
    }

    private Camera.Size getBestPreviewSize(int width, int height,
                                           Camera.Parameters parameters) {
        Camera.Size result=null;

        for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
            /*if (size.width<=width && size.height<=height) {
                if (result==null) {
                    result=size;
                }
                else {
                    int resultArea=result.width*result.height;
                    int newArea=size.width*size.height;

                    if (newArea>resultArea) {
                        result=size;
                    }
                }
            }*/
            result = size;
            break;
        }

        return(result);
    }

    private void initPreview(int width, int height) {
        if (camera!=null && previewHolder.getSurface()!=null) {
            try {
                camera.setPreviewDisplay(previewHolder);
            }
            catch (Throwable t) {
                //Log.e("CAMARA", "Exception in setPreviewDisplay()", t);
                Toast
                        .makeText(this, t.getMessage(), Toast.LENGTH_LONG)
                        .show();
            }

            if (!cameraConfigured) {
                Camera.Parameters parameters=camera.getParameters();
                Camera.Size size=getBestPreviewSize(width, height,
                        parameters);

                if (size!=null) {
                    parameters.setPreviewSize(size.width, size.height);
                    camera.setParameters(parameters);
                    cameraConfigured=true;
                }
            }
        }
    }

    private void startPreview() {
        if (cameraConfigured && camera!=null) {
            camera.startPreview();
            inPreview=true;
        }
    }

    private void enfocar() {
        if (inPreview) {
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success){
                        camera.takePicture(null, null, mandar);
                    }
                }
            });
        }
    }

    private void stopPreview() {
        if (cameraConfigured && camera!=null) {
            camera.stopPreview();
            inPreview=false;
        }
    }

    SurfaceHolder.Callback surfaceCallback=new SurfaceHolder.Callback() {
        public void surfaceCreated(SurfaceHolder holder) {
            // no-op -- wait until surfaceChanged()
        }

        public void surfaceChanged(SurfaceHolder holder,
                                   int format, int width,
                                   int height) {
            initPreview(width, height);
            startPreview();
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // no-op
        }
    };

    public void start(View vista){
        startPreview();
    }

    private String leerDatos(InputStream in) {
        BufferedReader lector = new BufferedReader(new InputStreamReader(in));
        String linea;
        String devolver = "";
        try {
            while ((linea = lector.readLine()) != null) {
                devolver = devolver.concat(linea + '\n');
            }
            lector.close();
        }catch (Exception e) {
            return null;
        }
        return devolver;
    }

    private class UploadToServer extends AsyncTask<ByteArrayInputStream, Void, InputStream>{

        HttpURLConnection conn;

        @Override
        protected InputStream doInBackground(ByteArrayInputStream... data) {
            conn = null;
            DataOutputStream dos = null;
            InputStream in = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            String nombre = "Foto" + nFoto + ".jpg";
            ByteArrayInputStream datos = data[0];
            int bytesRead, bytesAvailable, bufferSize, serverResponseCode;
            byte[] buffer;
            int maxBufferSize = 1024 * 1024;

            if (data[0] == null) {

                //Log.e("INTERNET", "Los bytes de la cámara son null");

                return null;

            }
            else
            {
                try {

                    // open a URL connection to the Servlet
                    URL url = new URL(dirBase + upLoadServerUri);

                    // Open a HTTP  connection to  the URL
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoInput(true); // Allow Inputs
                    conn.setDoOutput(true); // Allow Outputs
                    conn.setUseCaches(false); // Don't use a Cached Copy
                    conn.setConnectTimeout(500);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Connection", "Keep-Alive");
                    conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                    conn.setRequestProperty("Cache-Control", "no-cache");
                    conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                    conn.setRequestProperty("uploaded_file", nombre);

                    dos = new DataOutputStream(conn.getOutputStream());

                    dos.writeBytes(twoHyphens + boundary + lineEnd);
                    dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\"; filename=\""
                                    + nombre + "\"" + lineEnd);

                    dos.writeBytes(lineEnd);

                    // create a buffer of  maximum size
                    bytesAvailable = datos.available();

                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    buffer = new byte[bufferSize];

                    // read file and write it into form...
                    bytesRead = datos.read(buffer, 0, bufferSize);

                    while (bytesRead > 0) {

                        dos.write(buffer, 0, bufferSize);
                        bytesAvailable = datos.available();
                        bufferSize = Math.min(bytesAvailable, maxBufferSize);
                        bytesRead = datos.read(buffer, 0, bufferSize);

                    }

                    // send multipart form data necesssary after file data...
                    dos.writeBytes(lineEnd);
                    dos.writeBytes(twoHyphens + boundary + lineEnd);

                    // Responses from the server (code and message)
                    serverResponseCode = conn.getResponseCode();
                    String serverResponseMessage = conn.getResponseMessage();

                    //Log.i("uploadFile", "HTTP Response is : "
                    //        + serverResponseMessage + ": " + serverResponseCode);

                    if(serverResponseCode == 200){

                        in = new BufferedInputStream(conn.getInputStream());
                    }

                    //close the streams //
                    datos.close();
                    dos.flush();
                    dos.close();

                } catch (Exception e) {

                    e.printStackTrace();
                    in = null;

                   // Log.e("INTERNET", "Exception : " + e.getMessage(), e);
                }

                return in;

            }
        }

        @Override
        protected void onPostExecute(InputStream stream){
            if (stream != null) {
                String datosEntrada = leerDatos(stream);
                switch (datosEntrada) {
                    case "success\n":
                        if (nFoto < (maxFotos - 1)) {
                            nFoto++;
                            startPreview();
                            camera.takePicture(null, null, mandar);
                        } else {
                            stopPreview();
                            nFoto = 0;
                            SendFin fin = new SendFin();
                            fin.execute();
                        }
                }
            } else {
                stopPreview();
                startPreview();
            }
            conn.disconnect();
        }

    }

    private class SendFin extends AsyncTask<Void, Void, Void>{

        HttpURLConnection conn;

        @Override
        protected Void doInBackground(Void... arg) {
            conn = null;
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            int serverResponseCode;

            try {

                // open a URL connection to the Servlet
                URL url = new URL(dirBase + finServerUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(false); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setConnectTimeout(200);
                conn.setRequestMethod("POST");

                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes("tamano=" + tamano);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                //Log.i("fin", "HTTP Response is : "
                //        + serverResponseMessage + ": " + serverResponseCode);

                if(serverResponseCode == 200){

                    //Toast.makeText(getApplicationContext(), "Traspaso realizado", Toast.LENGTH_SHORT).show();
                }

                //close the streams //
                dos.flush();
                dos.close();

            } catch (Exception e) {

                e.printStackTrace();

                //Log.e("INTERNET", "Exception : " + e.getMessage(), e);
            }
            startPreview();
            return null;
        }

    }
}
