package raven.yolo.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Training configuration model
 */
public class TrainingConfig {
    
    @JsonProperty("epochs")
    private int epochs = 100;
    
    @JsonProperty("batch_size")
    private int batchSize = 16;
    
    @JsonProperty("image_size")
    private int imageSize = 640;
    
    @JsonProperty("learning_rate")
    private double learningRate = 0.01;
    
    @JsonProperty("model_variant")
    private String modelVariant = "yolov8n.pt"; // yolov8n, yolov8s, yolov8m, yolov8l, yolov8x
    
    @JsonProperty("device")
    private String device = "auto"; // auto, cpu, cuda
    
    @JsonProperty("workers")
    private int workers = 8;
    
    @JsonProperty("patience")
    private int patience = 50;
    
    @JsonProperty("save_period")
    private int savePeriod = -1;
    
    @JsonProperty("cache")
    private boolean cache = false;
    
    @JsonProperty("augment")
    private boolean augment = true;
    
    @JsonProperty("mosaic")
    private double mosaic = 1.0;
    
    @JsonProperty("mixup")
    private double mixup = 0.0;
    
    @JsonProperty("copy_paste")
    private double copyPaste = 0.0;
    
    // Constructors
    public TrainingConfig() {}
    
    // Getters and Setters
    public int getEpochs() {
        return epochs;
    }
    
    public void setEpochs(int epochs) {
        this.epochs = epochs;
    }
    
    public int getBatchSize() {
        return batchSize;
    }
    
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    
    public int getImageSize() {
        return imageSize;
    }
    
    public void setImageSize(int imageSize) {
        this.imageSize = imageSize;
    }
    
    public double getLearningRate() {
        return learningRate;
    }
    
    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }
    
    public String getModelVariant() {
        return modelVariant;
    }
    
    public void setModelVariant(String modelVariant) {
        this.modelVariant = modelVariant;
    }
    
    public String getDevice() {
        return device;
    }
    
    public void setDevice(String device) {
        this.device = device;
    }
    
    public int getWorkers() {
        return workers;
    }
    
    public void setWorkers(int workers) {
        this.workers = workers;
    }
    
    public int getPatience() {
        return patience;
    }
    
    public void setPatience(int patience) {
        this.patience = patience;
    }
    
    public int getSavePeriod() {
        return savePeriod;
    }
    
    public void setSavePeriod(int savePeriod) {
        this.savePeriod = savePeriod;
    }
    
    public boolean isCache() {
        return cache;
    }
    
    public void setCache(boolean cache) {
        this.cache = cache;
    }
    
    public boolean isAugment() {
        return augment;
    }
    
    public void setAugment(boolean augment) {
        this.augment = augment;
    }
    
    public double getMosaic() {
        return mosaic;
    }
    
    public void setMosaic(double mosaic) {
        this.mosaic = mosaic;
    }
    
    public double getMixup() {
        return mixup;
    }
    
    public void setMixup(double mixup) {
        this.mixup = mixup;
    }
    
    public double getCopyPaste() {
        return copyPaste;
    }
    
    public void setCopyPaste(double copyPaste) {
        this.copyPaste = copyPaste;
    }
}
