package com.corgibalance.components;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.io.IOException;

public class PaginationBar extends HBox {

    public static final int DEFAULT_PAGE_SIZE = 50;

    private static final Integer[] PAGE_SIZES = {25, 50, 100, 200, 500};

    @FXML
    private Button firstButton;
    @FXML
    private Button prevButton;
    @FXML
    private Button nextButton;
    @FXML
    private Button lastButton;
    @FXML
    private Label infoLabel;
    @FXML
    private Label pageLabel;
    @FXML
    private ComboBox<Integer> pageSizeCombo;

    private Runnable onPageChange = () -> {
    };

    private boolean updating;

    private int totalItems;
    private int pageSize = DEFAULT_PAGE_SIZE;
    private int pageCount = 1;
    private int currentPage = 1;

    public PaginationBar() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/components/PaginationBar.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load pagination bar component", e);
        }

        pageSizeCombo.getItems().setAll(PAGE_SIZES);
        pageSizeCombo.valueProperty().addListener((_, _, value) -> {
            if (!updating && value != null) {
                pageSize = value;
                onPageChange.run();
            }
        });
        firstButton.setGraphic(new HeroIcon(HeroIcon.Icon.SKIP_LEFT));
        lastButton.setGraphic(new HeroIcon(HeroIcon.Icon.SKIP_RIGHT));
        firstButton.setOnAction(_ -> {
            if (currentPage > 1) {
                currentPage = 1;
                onPageChange.run();
            }
        });
        lastButton.setOnAction(_ -> {
            if (currentPage < pageCount) {
                currentPage = pageCount;
                onPageChange.run();
            }
        });
        prevButton.setOnAction(_ -> {
            if (currentPage > 1) {
                currentPage = currentPage - 1;
                onPageChange.run();
            }
        });
        nextButton.setOnAction(_ -> {
            if (currentPage < pageCount) {
                currentPage = currentPage + 1;
                onPageChange.run();
            }
        });
    }

    public void setOnPageChange(Runnable callback) {
        this.onPageChange = callback == null ? () -> {
        } : callback;
    }

    public void update(int totalItems, int pageSize, int currentPage) {
        updating = true;
        try {
            this.totalItems = totalItems;
            this.pageSize = pageSize;
            this.pageCount = Math.max(1, (int) Math.ceil((double) totalItems / pageSize));
            this.currentPage = Math.max(1, Math.min(currentPage, pageCount));
            pageSizeCombo.setValue(pageSize);
            if (totalItems == 0) {
                infoLabel.setText("No transactions");
            } else {
                int from = (this.currentPage - 1) * pageSize + 1;
                int to = Math.min(this.currentPage * pageSize, totalItems);
                infoLabel.setText("Showing " + from + "\u2013" + to + " of " + totalItems);
            }
            pageLabel.setText("Page " + this.currentPage + " of " + pageCount);
            boolean jumpable = pageCount > 3;
            firstButton.setVisible(jumpable);
            lastButton.setVisible(jumpable);
            firstButton.setDisable(this.currentPage <= 1);
            lastButton.setDisable(this.currentPage >= pageCount);
            prevButton.setDisable(this.currentPage <= 1);
            nextButton.setDisable(this.currentPage >= pageCount);
        } finally {
            updating = false;
        }
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getPageCount() {
        return pageCount;
    }

    public int getTotalItems() {
        return totalItems;
    }
}
