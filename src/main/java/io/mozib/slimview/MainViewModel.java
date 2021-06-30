package io.mozib.slimview;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.embed.swing.SwingFXUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.imgscalr.Scalr;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.mozib.slimview.Common.*;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainViewModel {

    private List<ImageModel> imageModels = new ArrayList<>();
    private LoadDirectory loadDirectory;
    private final ReadOnlyStringWrapper status = new ReadOnlyStringWrapper();
    private final ReadOnlyObjectWrapper<ImageModel> selectedImageModelWrapper = new ReadOnlyObjectWrapper<>();
    private final double zoomStep = 0.1; // how much to zoom on each step

    public ReadOnlyObjectProperty<ImageModel> selectedImageModelProperty() {
        return selectedImageModelWrapper.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty statusProperty() {
        return status.getReadOnlyProperty();
    }

    @SuppressWarnings("unchecked")
    public void loadImage(ImageModel imageModel) {
        // first, show the image requested while the directory is being scanned
        setSelectedImage(imageModel);

        // now scan the rest of the directory
        loadDirectory = new LoadDirectory(new File(imageModel.getPath()).getParent());
        loadDirectory.setOnSucceeded((WorkerStateEvent event) -> {
            imageModels = (List<ImageModel>) event.getSource().getValue();
            setSelectedImage(imageModels.stream().filter(
                    image -> image.getPath().equals(imageModel.getPath())).findFirst().orElse(null));
        });
        status.bind(loadDirectory.messageProperty());
        loadDirectory.start();
        addToRecent(imageModel.getPath());
    }

    public void showFirstImage() {
        if (directoryScanComplete() && imageModels.size() > 0) {
            setSelectedImage(imageModels.get(0));
        }
    }

    public void showLastImage() {
        if (directoryScanComplete() && imageModels.size() > 0) {
            setSelectedImage(imageModels.get(imageModels.size() - 1));
        }
    }

    public void showNextImage() {
        if (directoryScanComplete() && imageModels.size() > 0) {
            if (getCurrentIndex() + 1 > imageModels.size() - 1) {
                setSelectedImage(imageModels.get(0));
            } else {
                setSelectedImage(imageModels.get(getCurrentIndex() + 1));
            }
            unloadInvisibleImages();
        }
    }

    public void showPreviousImage() {
        if (directoryScanComplete() && imageModels.size() > 0) {
            if (getCurrentIndex() - 1 < 0) {
                setSelectedImage(imageModels.get(imageModels.size() - 1));
            } else {
                setSelectedImage(imageModels.get(getCurrentIndex() - 1));
            }
            unloadInvisibleImages();
        }
    }

    public ImageModel getSelectedImageModel() {
        return selectedImageModelWrapper.get();
    }

    public boolean directoryScanComplete() {
        return loadDirectory == null || !loadDirectory.isRunning();
    }

    public void rotateLeft() {
        rotateImage(getSelectedImageModel(), Scalr.Rotation.CW_270);
    }

    public void rotateRight() {
        rotateImage(getSelectedImageModel(), Scalr.Rotation.CW_90);
    }

    public void flipVertically() {
        rotateImage(getSelectedImageModel(), Scalr.Rotation.FLIP_VERT);
    }

    public void flipHorizontally() {
        rotateImage(getSelectedImageModel(), Scalr.Rotation.FLIP_HORZ);
    }

    public void zoomIn() {
        zoomImage(getSelectedImageModel(), zoomStep);
    }

    public void zoomOut() {
        zoomImage(getSelectedImageModel(), -zoomStep);
    }

    public void resetZoom() {
        if (getSelectedImageModel().getResamplePath() == null) {
            return;
        }
        setSelectedImage(imageModels.stream()
                .filter(item -> item.getPath().equals(getSelectedImageModel().getResamplePath()))
                .findFirst()
                .orElse(null));
    }

    public void resizeImage(ImageModel imageModel, int newWidth, int newHeight) {
        var file = new File(Paths.get(cacheDirectory(), imageModel.getShortName()).toString());
        BufferedImage image;

        // resample image to ensure best resizing quality
        if (imageModel.getResamplePath() == null || imageModel.getResamplePath().equals("")) {
            imageModel.setResamplePath(imageModel.getPath());
            image = SwingFXUtils.fromFXImage(imageModel.getImage(), null);
        } else {
            image = SwingFXUtils.fromFXImage(imageModel.getResampleImage(), null);
        }

        var resized = Scalr.resize(image, Scalr.Mode.FIT_EXACT, newWidth, newHeight);
        editImage(resized, file, imageModel.getResamplePath());
    }

    public void saveImage(ImageModel imageModel, String destination) {
        File file = new File(destination);
        try {
            file.createNewFile();
            ImageIO.write(
                    SwingFXUtils.fromFXImage(imageModel.getImage(), null),
                    FilenameUtils.getExtension(destination),
                    file
            );
        } catch (IOException ignored) {

        }
    }

    public void trashImage(ImageModel imageModel) {
        try {
            Desktop.getDesktop().moveToTrash(new File(imageModel.getPath()));
            // remove from list
            imageModels.remove(imageModel);
            showNextImage();
        } catch (Exception ignored) {

        }
    }

    public void openInEditor(ImageModel imageModel) {
        if (getOSType() == OSType.Windows) {
            try {
                Runtime.getRuntime().exec("mspaint \"" + imageModel.getPath() + "\"");
            } catch (IOException ignored) {

            }
        }
    }

    public void openContainingFolder(ImageModel imageModel) {
        if (getOSType() == OSType.Windows) {
            try {
                Runtime.getRuntime().exec("explorer.exe /select, \"\"" + imageModel.getPath() + "\"\"");
            } catch (IOException ignored) {

            }
        } else {
            try {
                Desktop.getDesktop().open(imageModel.getContainingFolder());
            } catch (IOException ignored) {

            }
        }
    }

    public void copyToClipboard(ImageModel imageModel) {
        if (imageModel.getImage() == null) {
            return;
        }
        var transformedImage = SwingFXUtils.fromFXImage(imageModel.getImage(), null);
        var transferableImage = new ImageTransferable(transformedImage);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferableImage, null);
    }

    private String formatTime(long time) {
        Date date = new Date(time);
        Format format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
    }

    private boolean isEdited(ImageModel imageModel) {
        return imageModel.getPath().contains(cacheDirectory());
    }

    private void editImage(BufferedImage edited, File file, String resamplePath) {
        String format = FilenameUtils.getExtension(file.getPath());
        try {
            file.createNewFile();
            ImageIO.write(edited, format, file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        setSelectedImage(new ImageModel(file.getPath(), resamplePath));
    }

    private static class LoadDirectory extends Service<List<ImageModel>> {

        private final String directoryPath;
        private final AtomicInteger fileCount = new AtomicInteger(0);
        private final List<ImageModel> images = new ArrayList<>();

        public LoadDirectory(String directoryPath) {
            this.directoryPath = directoryPath;
        }

        public Integer getFileCount() {
            return fileCount.get();
        }

        @Override
        protected Task<List<ImageModel>> createTask() {
            return new Task<>() {
                @Override
                protected List<ImageModel> call() {
                    Iterator<File> iterator = FileUtils.iterateFiles(
                            new File(directoryPath), new String[]{"jpg", "jpeg", "png", "gif"}, false);
                    while (iterator.hasNext()) {
                        ImageModel image = new ImageModel(iterator.next().getPath());
                        images.add(image);
                        fileCount.addAndGet(1);
                        updateMessage("Scanning " + directoryPath + "... " + image.getShortName());
                    }
                    updateMessage("Found " + getFileCount() + " files.");
                    return images;
                }
            };
        }
    }

    private void setSelectedImage(ImageModel imageModel) {
        if (imageModel == null) return;

        selectedImageModelWrapper.set(imageModel);
        status.unbind();
        status.set((getCurrentIndex() + 1) + " / " + imageModels.size()
                + "  |  Resolution: " + imageModel.getResolution() + ", " + imageModel.getColorDepth() + "-bit"
                + "  |  Format: " + imageModel.getFormat()
                + "  |  Size: " + imageModel.getFormattedFileSize()
                + "  |  Created: " + formatTime(imageModel.getDateCreated())
                + "  |  Last Modified: " + formatTime(imageModel.getDateModified()));
    }

    private void unloadInvisibleImages() {
        if (getCurrentIndex() > 0) {
            imageModels.get(getCurrentIndex() - 1).unsetImage();
        }
        if (getCurrentIndex() < imageModels.size() - 1) {
            imageModels.get(getCurrentIndex() + 1).unsetImage();
        }
    }

    private void zoomImage(ImageModel imageModel, double percentage) {
        double newWidth = imageModel.getWidth() + imageModel.getWidth() * percentage;
        double newHeight = imageModel.getHeight() + imageModel.getHeight() * percentage;
        resizeImage(imageModel, (int) newWidth, (int) newHeight);
    }

    private void rotateImage(ImageModel imageModel, Scalr.Rotation rotation) {
        var file = new File(Paths.get(cacheDirectory(), imageModel.getShortName()).toString());
        var rotated = Scalr.rotate(SwingFXUtils.fromFXImage(imageModel.getImage(), null), rotation);
        editImage(rotated, file, imageModel.getResamplePath());
    }

    private int getCurrentIndex() {
        if (imageModels != null && getSelectedImageModel() != null) {
            if (getSelectedImageModel().getResamplePath() == null ||
                    getSelectedImageModel().getResamplePath().equals("")) {
                return imageModels.indexOf(imageModels.stream()
                        .filter(item -> getSelectedImageModel().getPath().equals(item.getPath()))
                        .findAny()
                        .orElse(null));

            } else {
                return imageModels.indexOf(imageModels.stream()
                        .filter(item -> getSelectedImageModel().getResamplePath().equals(item.getPath()))
                        .findAny()
                        .orElse(null));
            }
        }
        return 0;
    }
}
