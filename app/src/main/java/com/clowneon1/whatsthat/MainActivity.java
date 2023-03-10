package com.clowneon1.whatsthat;

import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraProvider;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;

import com.clowneon1.whatsthat.databinding.ActivityMainBinding;
import com.clowneon1.whatsthat.utils.Draw;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.objects.DetectedObject;
import com.google.mlkit.vision.objects.ObjectDetection;
import com.google.mlkit.vision.objects.ObjectDetector;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private ObjectDetector objectDetector;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //dataBinding
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        //Listen to the cameras
        cameraProviderFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                    // Bind camera provider to camera preview
                    bindPreview(cameraProvider);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        }, ContextCompat.getMainExecutor(this));

        //create a local object detection model from tflite file
        LocalModel localModel = new LocalModel.Builder()
                .setAssetFilePath("object_detection.tflite")
                .build();

        //Set object detection options
        CustomObjectDetectorOptions customObjectDetectorOptions = new CustomObjectDetectorOptions.Builder(localModel)
                .setDetectorMode(CustomObjectDetectorOptions.STREAM_MODE)
                .enableClassification()
                .setClassificationConfidenceThreshold(0.5f)
                .setMaxPerObjectLabelCount(3)
                .build();

        //create an object detector with custom options.
        objectDetector = ObjectDetection.getClient(customObjectDetectorOptions);
    }

    //Bind the cameraProvider to cameraPreview.
    @SuppressLint("UnsafeOptInUsageError")
    private void bindPreview(ProcessCameraProvider cameraProvider){
        Preview preview = new Preview.Builder().build(); //create a preview

        //Select the camera
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        //set surface provider for preview from binding
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());

        //create an Image Analysis object
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(1280 ,720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
 
        //set analyzer
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(ImageProxy imageProxy) {
                //get rotation of image
                int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();

                //get the image from image proxy
                Image image = imageProxy.getImage();

                if (image != null) {
                    //create an Input image for processing
                    InputImage processImage = InputImage.fromMediaImage(image, rotationDegrees);

                    //process the image with the help of objectDetector
                    objectDetector
                            .process(processImage)
                            .addOnSuccessListener(new OnSuccessListener<List<DetectedObject>>() {
                                @Override
                                public void onSuccess(List<DetectedObject> detectedObjects) {
                                    for(DetectedObject object : detectedObjects){
                                        if(binding.parentLayout.getChildCount() > 1) binding.parentLayout.removeViewAt(1);
                                        Draw element = new Draw(MainActivity.this, object.getBoundingBox(),
                                                object.getLabels().isEmpty() ? "Undefined" : object.getLabels().get(0).getText());
                                        binding.parentLayout.addView(element);
                                    }
                                    imageProxy.close();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(Exception e) {
                                    Log.v("MainActivity","Error - ${it.message}");
                                    imageProxy.close();
                                }
                            });
                }

            }
        });

        //bind camera provider to lifecycle.
        cameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis, preview);

    }
}
