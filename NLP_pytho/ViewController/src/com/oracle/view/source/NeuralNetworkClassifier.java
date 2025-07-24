package com.oracle.view.source;
import java.util.*;
import java.util.stream.Collectors;


import java.util.*;
import java.util.stream.Collectors;

/**
 * Simple neural network implementation for query classification
 */
public class NeuralNetworkClassifier {
    
    private final int inputSize;
    private final int hiddenSize;
    private final int outputSize;
    private final double[][] weightsInputHidden;
    private final double[][] weightsHiddenOutput;
    private final double[] hiddenBias;
    private final double[] outputBias;
    private final double learningRate;
    
    private final Map<String, Integer> vocabularyIndex;
    private final List<String> outputLabels;
    
    public NeuralNetworkClassifier() {
        this.inputSize = 100; // Feature vector size
        this.hiddenSize = 50;
        this.outputSize = 10; // Number of query types
        this.learningRate = 0.01;
        
        // Initialize weights randomly
        this.weightsInputHidden = initializeWeights(inputSize, hiddenSize);
        this.weightsHiddenOutput = initializeWeights(hiddenSize, outputSize);
        this.hiddenBias = new double[hiddenSize];
        this.outputBias = new double[outputSize];
        
        // Initialize vocabulary and labels
        this.vocabularyIndex = buildVocabulary();
        this.outputLabels = Arrays.asList(
            "CONTRACT_DETAILS", "CONTRACT_BY_DATE", "CONTRACT_BY_CUSTOMER", 
            "CONTRACT_STATUS", "PART_DETAILS", "PART_STATUS", "PARTS_IN_CONTRACT",
            "FAILED_PARTS", "HELP", "GENERAL_QUERY"
        );
    }
    
    /**
     * Classify query using neural network
     */
    public String classify(String input) {
        double[] features = extractFeatures(input);
        double[] output = forward(features);
        
        // Find the class with highest probability
        int maxIndex = 0;
        for (int i = 1; i < output.length; i++) {
            if (output[i] > output[maxIndex]) {
                maxIndex = i;
            }
        }
        
        return outputLabels.get(maxIndex);
    }
    
    /**
     * Train the neural network with a single example
     */
    public void train(String input, String expectedOutput) {
        double[] features = extractFeatures(input);
        double[] target = createTargetVector(expectedOutput);
        
        // Forward pass
        double[] hiddenOutput = new double[hiddenSize];
        double[] finalOutput = new double[outputSize];
        
        // Calculate hidden layer
        for (int i = 0; i < hiddenSize; i++) {
            double sum = hiddenBias[i];
            for (int j = 0; j < inputSize; j++) {
                sum += features[j] * weightsInputHidden[j][i];
            }
            hiddenOutput[i] = sigmoid(sum);
        }
        
        // Calculate output layer
        for (int i = 0; i < outputSize; i++) {
            double sum = outputBias[i];
            for (int j = 0; j < hiddenSize; j++) {
                sum += hiddenOutput[j] * weightsHiddenOutput[j][i];
            }
            finalOutput[i] = sigmoid(sum);
        }
        
        // Backward pass (simplified)
        double[] outputError = new double[outputSize];
        for (int i = 0; i < outputSize; i++) {
            outputError[i] = (target[i] - finalOutput[i]) * finalOutput[i] * (1 - finalOutput[i]);
        }
        
        // Update weights (simplified gradient descent)
        for (int i = 0; i < hiddenSize; i++) {
            for (int j = 0; j < outputSize; j++) {
                weightsHiddenOutput[i][j] += learningRate * outputError[j] * hiddenOutput[i];
            }
        }
        
        // Update hidden layer weights
        double[] hiddenError = new double[hiddenSize];
        for (int i = 0; i < hiddenSize; i++) {
            double error = 0;
            for (int j = 0; j < outputSize; j++) {
                error += outputError[j] * weightsHiddenOutput[i][j];
            }
            hiddenError[i] = error * hiddenOutput[i] * (1 - hiddenOutput[i]);
        }
        
        for (int i = 0; i < inputSize; i++) {
            for (int j = 0; j < hiddenSize; j++) {
                weightsInputHidden[i][j] += learningRate * hiddenError[j] * features[i];
            }
        }
    }
    
    private double[] forward(double[] input) {
        // Hidden layer
        double[] hiddenOutput = new double[hiddenSize];
        for (int i = 0; i < hiddenSize; i++) {
            double sum = hiddenBias[i];
            for (int j = 0; j < inputSize; j++) {
                sum += input[j] * weightsInputHidden[j][i];
            }
            hiddenOutput[i] = sigmoid(sum);
        }
        
        // Output layer
        double[] output = new double[outputSize];
        for (int i = 0; i < outputSize; i++) {
            double sum = outputBias[i];
            for (int j = 0; j < hiddenSize; j++) {
                sum += hiddenOutput[j] * weightsHiddenOutput[j][i];
            }
            output[i] = sigmoid(sum);
        }
        
        return output;
    }
    
    private double[] extractFeatures(String input) {
        double[] features = new double[inputSize];
        String[] words = input.toLowerCase().split("\\s+");
        
        // Bag of words features
        for (String word : words) {
            Integer index = vocabularyIndex.get(word);
            if (index != null && index < inputSize - 20) { // Reserve last 20 for special features
                features[index] = 1.0;
            }
        }
        
        // Special features
        int specialFeatureStart = inputSize - 20;
        features[specialFeatureStart] = input.matches(".*\\d{6}.*") ? 1.0 : 0.0; // Has contract number
        features[specialFeatureStart + 1] = input.matches(".*\\d{4}.*") ? 1.0 : 0.0; // Has year
        features[specialFeatureStart + 2] = input.toLowerCase().contains("customer") ? 1.0 : 0.0;
        features[specialFeatureStart + 3] = input.toLowerCase().contains("contract") ? 1.0 : 0.0;
        features[specialFeatureStart + 4] = input.toLowerCase().contains("part") ? 1.0 : 0.0;
        features[specialFeatureStart + 5] = input.toLowerCase().contains("show") ? 1.0 : 0.0;
        features[specialFeatureStart + 6] = input.toLowerCase().contains("get") ? 1.0 : 0.0;
        features[specialFeatureStart + 7] = input.toLowerCase().contains("created") ? 1.0 : 0.0;
        features[specialFeatureStart + 8] = input.toLowerCase().contains("expired") ? 1.0 : 0.0;
        features[specialFeatureStart + 9] = input.toLowerCase().contains("active") ? 1.0 : 0.0;
        features[specialFeatureStart + 10] = input.toLowerCase().contains("failed") ? 1.0 : 0.0;
        features[specialFeatureStart + 11] = input.toLowerCase().contains("help") ? 1.0 : 0.0;
        features[specialFeatureStart + 12] = input.toLowerCase().contains("details") ? 1.0 : 0.0;
        features[specialFeatureStart + 13] = input.toLowerCase().contains("status") ? 1.0 : 0.0;
        features[specialFeatureStart + 14] = input.toLowerCase().contains("metadata") ? 1.0 : 0.0;
        features[specialFeatureStart + 15] = input.toLowerCase().contains("account") ? 1.0 : 0.0;
        features[specialFeatureStart + 16] = input.toLowerCase().contains("number") ? 1.0 : 0.0;
        features[specialFeatureStart + 17] = input.toLowerCase().contains("date") ? 1.0 : 0.0;
        features[specialFeatureStart + 18] = input.toLowerCase().contains("price") ? 1.0 : 0.0;
        features[specialFeatureStart + 19] = words.length > 5 ? 1.0 : 0.0; // Long query
        
        return features;
    }
    
    private double[] createTargetVector(String expectedOutput) {
        double[] target = new double[outputSize];
        int index = outputLabels.indexOf(expectedOutput);
        if (index >= 0) {
            target[index] = 1.0;
        }
        return target;
    }
    
    private double[][] initializeWeights(int rows, int cols) {
        double[][] weights = new double[rows][cols];
        Random random = new Random();
        
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                weights[i][j] = (random.nextGaussian() * 0.1); // Small random weights
            }
        }
        
        return weights;
    }
    
    private Map<String, Integer> buildVocabulary() {
        Map<String, Integer> vocab = new HashMap<>();
        String[] commonWords = {
            "show", "get", "contract", "contracts", "customer", "account", "number", "created",
            "date", "details", "info", "status", "expired", "active", "part", "parts", "failed",
            "help", "metadata", "effective", "price", "list", "project", "type", "summary",
            "find", "all", "for", "by", "in", "with", "after", "before", "between", "and",
            "or", "not", "is", "are", "was", "were", "has", "have", "had", "do", "does",
            "did", "will", "would", "could", "should", "can", "may", "might", "must",
            "siemens", "honeywell", "boeing", "vinod", "mary", "ae125", "ae126", "corporate",
            "opportunity", "code", "specifications", "available", "stock", "lead", "time",
            "manufacturer", "issues", "defects", "warranty", "period", "compatible", "discontinued",
            "validation", "loaded", "missing", "rejected", "skipped", "passed", "pricing",
            "mismatch", "master", "data", "successful", "error", "cost", "today", "month",
            "year", "last", "first", "next", "previous", "current", "new", "old", "recent"
        };
        
        for (int i = 0; i < commonWords.length && i < inputSize - 20; i++) {
            vocab.put(commonWords[i], i);
        }
        
        return vocab;
    }
    
    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }
}