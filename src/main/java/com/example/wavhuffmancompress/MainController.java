package com.example.wavhuffmancompress;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MainController {
    @FXML
    private Button importBtn;

    @FXML
    private Button resetBtn;

    @FXML
    private Canvas leftChannel;

    @FXML
    private Canvas rightChannel;

    @FXML
    private Text entropy;

    @FXML
    private Text acwLength;

    private int NUM_POINTS = 500;

    private int[] data;

    public void importButtonAction(ActionEvent event) {
        FileChooser fc = new FileChooser();

        // Set extension filter
        FileChooser.ExtensionFilter extensionFilter = new FileChooser.ExtensionFilter("Wav Files", "*.wav");
        fc.getExtensionFilters().add(extensionFilter);
        File selectedFile = fc.showOpenDialog(null);

        if (selectedFile != null) {
            processWavFile(selectedFile);
            HashMap<Integer, String> huffmanCodes = HuffmanTree.buildHuffmanCodes(data);
            HashMap<Integer, Double> probabilityDistribution = calculateProbabilityDistribution(data);

            double entropyValue = calculateEntropy(probabilityDistribution);
            entropy.setText("Entropy: " + String.format("%.4f", entropyValue));

            double averageCodeWordLength = calculateAverageCodeWordLength(probabilityDistribution, huffmanCodes);
            acwLength.setText("Average Code Word Length: " + String.format("%.4f", averageCodeWordLength));
        }
    }

    public void resetButtonAction(ActionEvent event) {
        // Clear the left and right channel canvases
        clearCanvas(leftChannel);
        clearCanvas(rightChannel);

        // Clear the totalSample and frequency Text elements
        entropy.setText("Entropy: ");
        acwLength.setText("Average code word length: ");
    }

    private void clearCanvas(Canvas canvas) {
        // Get the canvas graphics context
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Clear the canvas
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    private void processWavFile(File file) {
        try {
            // Read the WAV file
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(file);

            // Get audio format and sample rate
            AudioFormat audioFormat = audioInputStream.getFormat();
            int sampleRate = (int) audioFormat.getSampleRate();

            // Calculate the total number of samples
            int totalSamples = (int) audioInputStream.getFrameLength();

            // Split audio into left and right channels
            int bytesPerFrame = audioFormat.getFrameSize();
            int bufferSize = totalSamples * bytesPerFrame;

            byte[] leftChannelData = new byte[bufferSize / 2];
            byte[] rightChannelData = new byte[bufferSize / 2];

            byte[] audioData = new byte[bufferSize];
            int bytesRead = audioInputStream.read(audioData);
            this.data = bytesToInts(audioData);

            for (int i = 0; i < bytesRead / 2; i = i + 2) {

                leftChannelData[i] = audioData[2 * i];
                leftChannelData[i + 1] = audioData[2 * i + 1];
                rightChannelData[i] = audioData[2 * i + 2];
                rightChannelData[i + 1] = audioData[2 * i + 3];
            }

            int[] leftChannelInts = bytesToInts(leftChannelData);
            int[] rightChannelInts = bytesToInts(rightChannelData);

            // Plot left channel data on leftChannelCanvas
            plotAudioData(leftChannel, leftChannelInts, audioFormat);

            // Plot right channel data on rightChannelCanvas
            plotAudioData(rightChannel, rightChannelInts, audioFormat);

            // Close the audio input stream
            audioInputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int[] bytesToInts(byte[] byteArray) {
        int[] intArray = new int[byteArray.length / 2];

        for (int i = 0; i < byteArray.length; i += 2) {
            int byte1 = byteArray[i];
            int byte2 = byteArray[i + 1];

            // Combine two bytes into one integer (assuming little-endian format)
            int intValue = (byte2 << 8) | (byte1 & 0xFF);

            // Store the integer value in the resulting array
            intArray[i / 2] = intValue;
        }

        return intArray;
    }

    private void plotAudioData(Canvas canvas, int[] audioData, AudioFormat audioFormat) {
        // Get the canvas graphics context
        GraphicsContext gc = canvas.getGraphicsContext2D();

        // Clear the canvas
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());

        // Calculate the scaling factor for X-axis
        double scaleX = canvas.getWidth() / (double) NUM_POINTS;

        // Calculate the number of samples per point
        int samplesPerPoint = audioData.length / NUM_POINTS;

        // Initialize variables for RMS calculation
        int sampleIndex = 0;
        double[] rmsValues = new double[NUM_POINTS];

        for (int point = 0; point < NUM_POINTS; point++) {
            double sumOfSquares = 0;

            for (int i = 0; i < samplesPerPoint; i++) {
                // Convert byte values to a double in the range [-1.0, 1.0]
                double sampleValue = audioData[sampleIndex] / 128.0;
                sumOfSquares += sampleValue * sampleValue;
                sampleIndex++;
            }

            // Calculate RMS value
            rmsValues[point] = Math.sqrt(sumOfSquares / samplesPerPoint);
        }

        // Normalize and draw the waveform as individual points
        int centerY = (int) canvas.getHeight() / 2;
        double maxRMS = getMaxRMSValue(rmsValues);

        for (int point = 0; point < NUM_POINTS; point++) {
            double x = point * scaleX;
            double rms = rmsValues[point];

            // Calculate the y-coordinate for the top and bottom of the vertical line
            double yTop = centerY - (rms / maxRMS * centerY);
            double yBottom = centerY + (rms / maxRMS * centerY);

            // Draw a vertical line from -RMS to RMS at the x-coordinate
            gc.setStroke(Color.GREEN);
            gc.setLineWidth(1);
            gc.strokeLine(x, yTop, x, yBottom);
        }
    }

    private double getMaxRMSValue(double[] rmsValues) {
        double maxRMS = 0;

        for (double rms : rmsValues) {
            if (rms > maxRMS) {
                maxRMS = rms;
            }
        }

        return maxRMS;
    }

    private HashMap<Integer, Double> calculateProbabilityDistribution(int[] data) {
        HashMap<Integer, Integer> frequencyMap = new HashMap<>();

        // Calculate byte frequencies
        for (int b : data) {
            frequencyMap.put(b, frequencyMap.getOrDefault(b, 0) + 1);
        }

        int totalBytes = data.length;
        HashMap<Integer, Double> probabilityDistribution = new HashMap<>();

        // Calculate probabilities
        for (Map.Entry<Integer, Integer> entry : frequencyMap.entrySet()) {
            Integer value = entry.getKey();
            int frequency = entry.getValue();
            double probability = (double) frequency / totalBytes;
            probabilityDistribution.put(value, probability);
        }

        return probabilityDistribution;
    }

    private double calculateEntropy(Map<Integer, Double> probabilityDistribution) {
        double entropy = 0.0;
        for (double probability : probabilityDistribution.values()) {
            if (probability > 0) {
                entropy += probability * Math.log(probability) / Math.log(2);
            }
        }
        return -entropy;
    }

    private double calculateAverageCodeWordLength(Map<Integer, Double> probabilityDistribution, Map<Integer, String> huffmanCodes) {
        double averageCodeWordLength = 0.0;
        for (Integer value : probabilityDistribution.keySet()) {
            double probability = probabilityDistribution.get(value);
            String codeWord = huffmanCodes.get(value);
            int codeWordLength = codeWord.length();
            averageCodeWordLength += probability * codeWordLength;
        }
        return averageCodeWordLength;
    }
}
