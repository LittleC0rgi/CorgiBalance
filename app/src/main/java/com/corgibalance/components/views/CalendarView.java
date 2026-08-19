package com.corgibalance.components.views;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class CalendarView extends View implements Refreshable {

    private static final DateTimeFormatter MONTH_FORMAT =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    @FXML
    private Label monthLabel;
    @FXML
    private GridPane grid;

    private YearMonth currentMonth;

    public CalendarView() {
        super("Calendar", "/fxml/views/calendar.fxml");
    }

    @FXML
    private void initialize() {
        currentMonth = YearMonth.now();
        render();
    }

    @Override
    public void onShow() {
        render();
    }

    @FXML
    private void prevMonth() {
        currentMonth = currentMonth.minusMonths(1);
        render();
    }

    @FXML
    private void nextMonth() {
        currentMonth = currentMonth.plusMonths(1);
        render();
    }

    private void render() {
        monthLabel.setText(currentMonth.format(MONTH_FORMAT));
        grid.getChildren().clear();
        LocalDate today = LocalDate.now();
        int offset = firstColumn(currentMonth);
        int days = currentMonth.lengthOfMonth();
        for (int day = 1; day <= days; day++) {
            int position = offset + day - 1;
            VBox cell = dayCell(currentMonth.atDay(day), today);
            grid.add(cell, position % 7, position / 7);
            GridPane.setHgrow(cell, Priority.ALWAYS);
            GridPane.setVgrow(cell, Priority.ALWAYS);
        }
    }

    private VBox dayCell(LocalDate date, LocalDate today) {
        VBox cell = new VBox(4);
        cell.getStyleClass().add("calendar__day");
        if (date.equals(today)) {
            cell.getStyleClass().add("calendar__day--today");
        }
        cell.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        Label number = new Label(String.valueOf(date.getDayOfMonth()));
        number.getStyleClass().add("calendar__day__number");

        VBox plans = new VBox(2);
        plans.getStyleClass().add("calendar__day__plans");
        VBox.setVgrow(plans, Priority.ALWAYS);
        plans.setMaxHeight(Double.MAX_VALUE);

        cell.getChildren().addAll(number, plans);
        return cell;
    }

    public static int firstColumn(YearMonth month) {
        return month.atDay(1).getDayOfWeek().getValue() - 1;
    }
}
