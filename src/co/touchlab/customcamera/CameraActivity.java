package co.touchlab.customcamera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.*;
import android.widget.*;
import co.touchlab.customcamera.util.CWLog;
import co.touchlab.customcamera.util.ZipUtil;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CameraActivity extends Activity
{
    private Camera camera;
    private MediaRecorder mediaRecorder;

    private CameraPreviewView cameraPreviewView;
    private Camera.Size previewVideoSize;
    private Camera.Size cameraVideoSize;
    private boolean cameraInitialized;

    //Dimensions for resizing preview
    private int largePreviewWidth;
    private int largePreviewHeight;
    private Integer smallPreviewWidth;
    private Integer smallPreviewHeight;

    private AtomicBoolean isRecording;

    //Values set if video message opened
    private File openedMessageVideoFile;
    private VideoView openedMessageVideoView;

    private Button mainActionButton;

    //Timing code for delayed reaction start
    private Handler videoMonitorHandler;
    private CheckVideoTimeRunnable checkVideoTimeRunnable;
    private MessageMetadata openedMessageMetadata;
    private long myMessageStartTime;
    private long myMessageEndTime;
    private long replyMessageEndTime;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        initPreviewSizes();

        openedMessageVideoView = (VideoView) findViewById(R.id.videoView);
        cameraPreviewView = (CameraPreviewView) findViewById(R.id.camera_preview);
        cameraPreviewView.setZOrderOnTop(true);

        openAndConfigureCamera(false);
        isRecording = new AtomicBoolean(false);

        mainActionButton = (Button) findViewById(R.id.camera_button);
        mainActionButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                triggerButtonAction();
            }
        });

        videoMonitorHandler = new Handler();
        checkVideoTimeRunnable = new CheckVideoTimeRunnable();

        extractFileAttachment();
    }

    private void triggerButtonAction()
    {
        if (isRecording.compareAndSet(false, true))
        {
            startMessageSession();
        }
        else
        {
            mainActionButton.setText("Start Reply");
            isRecording.set(false);
            stopVideo();
            sendEmail();
        }
    }

    private void startMessageSession()
    {
        if (openedMessageVideoFile != null)
        {
            ViewGroup.LayoutParams layoutParams = cameraPreviewView.getLayoutParams();
            layoutParams.width = smallPreviewWidth;
            layoutParams.height = smallPreviewHeight;
            cameraPreviewView.setLayoutParams(layoutParams);

            playLastVideo();
            mainActionButton.setText("Stop Reply");
        }
        else
        {
            mainActionButton.setText("Stop Message");
        }

        if(openedMessageVideoFile != null && openedMessageMetadata.startRecording > 0)
        {
            videoMonitorHandler.postDelayed(checkVideoTimeRunnable, 100);
        }
        else
        {
            startRecording();
        }
    }

    private class CheckVideoTimeRunnable implements Runnable
    {
        @Override
        public void run()
        {
            int currentPosition = openedMessageVideoView.getCurrentPosition();
            if(currentPosition >= (int)(openedMessageMetadata.startRecording * 1000))
            {
                startRecording();
            }
            else
            {
                videoMonitorHandler.postDelayed(checkVideoTimeRunnable, 100);
            }
        }
    }


    private void extractFileAttachment()
    {
        Uri uri = getIntent().getData();
        if (uri != null)
        {
            try
            {
                InputStream is = getContentResolver().openInputStream(uri);
                File file = new File(getFilesDir(), "vid_" + System.currentTimeMillis() + ".wala");
                FileOutputStream os = new FileOutputStream(file);

                byte[] buffer = new byte[4096];
                int count;

                while ((count = is.read(buffer)) > 0)
                    os.write(buffer, 0, count);

                os.close();
                is.close();

                File outFolder = new File(getFilesDir(), "chat_" + System.currentTimeMillis());
                outFolder.mkdirs();

                ZipUtil.unzipFiles(file, outFolder);

                openedMessageVideoFile = new File(outFolder, "video.mp4");
                FileInputStream input = new FileInputStream(new File(outFolder, "metadata.json"));
                openedMessageMetadata = new MessageMetadata();
                openedMessageMetadata.init(new JSONObject(IOUtils.toString(input)));

                input.close();

                videoMonitorHandler.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        triggerButtonAction();
                    }
                }, 2000);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            catch (JSONException e)
            {
                throw new RuntimeException(e);
            }
        }
    }


    @SuppressWarnings("unchecked")
    private void sendEmail()
    {
        new AsyncTask()
        {
            @Override
            protected Object doInBackground(Object... params)
            {
                File buildDir = new File(Environment.getExternalStorageDirectory(), "chat_" + System.currentTimeMillis());
                buildDir.mkdirs();

                File walaFile = new File(buildDir, "video.mp4");

                File outZip;
                try
                {

                    FileOutputStream output = new FileOutputStream(walaFile);
                    FileInputStream input = new FileInputStream(openedMessageVideoFile);
                    IOUtils.copy(input, output);

                    input.close();
                    output.close();

                    File metadataFile = new File(buildDir, "metadata.json");

                    MessageMetadata sendMessageMetadata = openedMessageMetadata == null ? new MessageMetadata() : openedMessageMetadata.copy();

                    sendMessageMetadata.incrementForNewMessage();

                    long startRecordingMillis = replyMessageEndTime == 0 ? 0 : replyMessageEndTime - myMessageStartTime;
                    sendMessageMetadata.startRecording = ((double)startRecordingMillis)/1000d;

                    sendMessageMetadata.senderId = "kevin@touchlab.co";

                    FileWriter fileWriter = new FileWriter(metadataFile);

                    fileWriter.append(sendMessageMetadata.toJsonString());

                    fileWriter.close();

                    outZip = new File(Environment.getExternalStorageDirectory(), "chat.wala");

                    ZipUtil.zipFiles(outZip, Arrays.asList(buildDir.listFiles()));
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
                catch (JSONException e)
                {
                    throw new RuntimeException(e);
                }

                return outZip;
            }

            @Override
            protected void onPostExecute(Object o)
            {
                File outZip = (File)o;
                Intent intent = new Intent(Intent.ACTION_SEND);

                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"kevin@touchlab.co"});
                intent.putExtra(Intent.EXTRA_SUBJECT, "test reply");
                intent.putExtra(Intent.EXTRA_TEXT, "the video");

                Uri uri = Uri.fromFile(outZip);
                intent.putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(Intent.createChooser(intent, "Send email..."));
            }
        }.execute();
    }

    private void initPreviewSizes()
    {
        largePreviewWidth = Math.round(getResources().getDimension(R.dimen.large_preview_width));
        largePreviewHeight = Math.round(getResources().getDimension(R.dimen.large_preview_height));

        smallPreviewWidth = Math.round(getResources().getDimension(R.dimen.small_preview_width));
        smallPreviewHeight = Math.round(getResources().getDimension(R.dimen.small_preview_height));
    }

    private void openAndConfigureCamera(boolean afterPaused)
    {
        try
        {
            camera = Camera.open(getFrontCameraId());

            Camera.Parameters params = camera.getParameters();
            previewVideoSize = getVideoSize(params);
            cameraVideoSize = findCameraVideoSize(params);
            params.setPreviewSize(previewVideoSize.width, previewVideoSize.height);
            camera.setParameters(params);

            cameraPreviewView.initSurface(camera);

            cameraInitialized = true;
        }
        catch (Exception e)
        {
            Log.d("##################", "Error opening camera: " + e.getMessage());
            releaseCamera();
        }
    }

    private int getFrontCameraId() throws Exception
    {
        int numberOfCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < numberOfCameras; i++)
        {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
            {
                return i;
            }
        }

        return 0;
    }

    private Camera.Size getVideoSize(Camera.Parameters parameters)
    {
        //Create new array in case this is immutable
        List<Camera.Size> supportedVideoSizes = new ArrayList<Camera.Size>(parameters.getSupportedPreviewSizes());
        return findBestFitCameraSize(supportedVideoSizes);
    }

    private Camera.Size findCameraVideoSize(Camera.Parameters parameters)
    {
        //Create new array in case this is immutable
        List<Camera.Size> supportedVideoSizes = new ArrayList<Camera.Size>(parameters.getSupportedVideoSizes());
        return findBestFitCameraSize(supportedVideoSizes);
    }

    private Camera.Size findBestFitCameraSize(List<Camera.Size> supportedVideoSizes)
    {
        int minWidth = getResources().getInteger(R.integer.video_min_width);
        Camera.Size best = supportedVideoSizes.get(0);

        for(int i=1; i<supportedVideoSizes.size(); i++)
        {
            Camera.Size size = supportedVideoSizes.get(i);
            if(size.width >= minWidth && size.width < best.width)
                best = size;
        }

        assert best.width >= minWidth;

        CWLog.i("width: " + best.width + "/height: " + best.height);

        return best;
    }

    private void startRecording()
    {
        camera.stopPreview();
        camera.unlock();

        if (prepareMediaRecorder())
        {
            toggleRecordSettings();

            mediaRecorder.start();
            myMessageStartTime = System.currentTimeMillis();
            camera.startPreview();
        }
        else
        {
            releaseMediaRecorder();
        }
    }

    private void playLastVideo()
    {
        openedMessageVideoView.setVideoPath(openedMessageVideoFile.getPath());
        openedMessageVideoView.start();
        openedMessageVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener()
        {
            @Override
            public void onCompletion(MediaPlayer mp)
            {
                playbackDone();
            }
        });
    }

    private void playbackDone()
    {
        ViewGroup.LayoutParams layoutParams = cameraPreviewView.getLayoutParams();
        layoutParams.width = largePreviewWidth;
        layoutParams.height = largePreviewHeight;
        cameraPreviewView.setLayoutParams(layoutParams);
        replyMessageEndTime = System.currentTimeMillis();
    }

    private boolean prepareMediaRecorder()
    {
        mediaRecorder = new MediaRecorder();

        // Step 1: Attach camera to media recorder
        mediaRecorder.setCamera(camera);

        // Step 2: Set sources

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setVideoFrameRate(getResources().getInteger(R.integer.video_frame_rate));


        mediaRecorder.setVideoSize(cameraVideoSize.width, cameraVideoSize.height);

        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

        mediaRecorder.setVideoEncodingBitRate(getResources().getInteger(R.integer.video_bid_depth));

        Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        int mrRotate = 0;

        if (display.getRotation() == Surface.ROTATION_0)
            mrRotate = 270;


        if (display.getRotation() == Surface.ROTATION_270)
            mrRotate = 180;

        if (mrRotate != 0)
            mediaRecorder.setOrientationHint(mrRotate);

        // Step 4: Set output file
        File file = new File(Environment.getExternalStorageDirectory(), "vid_" + System.currentTimeMillis() + ".mp4");
        openedMessageVideoFile = file;
        mediaRecorder.setOutputFile(file.getPath());

        // Step 5: Set the preview output
        mediaRecorder.setPreviewDisplay(cameraPreviewView.getHolder().getSurface());

        try
        {
            mediaRecorder.prepare();
        }
        catch (IllegalStateException e)
        {
            Log.d("##################", "MediaRecorder prepared in wrong order: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        catch (IOException e)
        {
            Log.d("##################", "MediaRecorder prepare failed: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }

        return true;
    }



    private void stopVideo()
    {
        mediaRecorder.stop();
        myMessageEndTime = System.currentTimeMillis();
        releaseMediaRecorder();
//        openedMessageVideoView.stopPlayback();

        //todo: pulled out a bunch of code here that handles saving a file with the video

        toggleRecordSettings();
        Toast.makeText(this, "Done video!", Toast.LENGTH_SHORT).show();
    }

    private void toggleRecordSettings()
    {
        int currentOrientation = getResources().getConfiguration().orientation;
        if (!isRecording.get())
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
        else if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE)
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
        else
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (!cameraInitialized)
        {
            openAndConfigureCamera(true);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        releaseMediaRecorder();
        releaseCamera();
    }

    private void releaseCamera()
    {
        if (camera != null)
        {
            camera.release();
        }
        cameraInitialized = false;
    }

    private void releaseMediaRecorder()
    {
        if (mediaRecorder != null)
        {
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            try
            {
                camera.reconnect();
            }
            catch (IOException e)
            {
                Log.d("##################", "Media Recorder - error when releasing camera: " + e.getMessage());
            }
        }
    }
}
