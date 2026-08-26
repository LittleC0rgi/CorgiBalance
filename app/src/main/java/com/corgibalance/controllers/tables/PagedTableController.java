package com.corgibalance.controllers.tables;

import com.corgibalance.components.PaginationBar;
import com.corgibalance.models.BaseModel;
import com.corgibalance.repositories.CrudRepository;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public abstract class PagedTableController<T extends BaseModel, R extends CrudRepository<T>>
        extends BaseTableController<T, R> {

    @FXML
    protected PaginationBar paginationBar;

    private final List<T> baseList = new ArrayList<>();
    private final List<T> displayList = new ArrayList<>();
    private int pageSize = PaginationBar.DEFAULT_PAGE_SIZE;
    private int currentPage = 1;
    private Comparator<T> activeSort;
    private Predicate<T> filter = item -> true;

    protected PagedTableController(R repository) {
        super(repository);
    }

    @Override
    public void initialize() {
        if (paginationBar != null) {
            paginationBar.setOnPageChange(this::onPaginationChanged);
        }
        super.initialize();
        if (paginationBar != null) {
            configureSorting();
        }
    }

    @Override
    protected void loadData() {
        if (paginationBar == null) {
            super.loadData();
            return;
        }
        baseList.clear();
        baseList.addAll(repository.findAll());
        currentPage = lastPage();
        rebuildDisplayList();
        applyPage();
    }

    @Override
    protected void commit(T item, Consumer<T> apply, boolean createOnPlaceholder) {
        apply.accept(item);
        if (isPlaceholder(item)) {
            if (!createOnPlaceholder) {
                table.refresh();
                return;
            }
            try {
                repository.create(item);
            } catch (RuntimeException e) {
                showError(e);
                return;
            }
            if (paginationBar == null) {
                table.getItems().add(newPlaceholder());
                table.refresh();
                return;
            }
            showPageContaining(item);
            return;
        }
        try {
            repository.update(item);
        } catch (RuntimeException e) {
            showError(e);
        }
        table.refresh();
    }

    @Override
    protected void onItemDeleted(T item) {
        if (paginationBar == null) {
            return;
        }
        baseList.removeIf(candidate -> Objects.equals(candidate.getId(), item.getId()));
        rebuildDisplayList();
        applyPage();
    }

    private void showPageContaining(T item) {
        baseList.clear();
        baseList.addAll(repository.findAll());
        rebuildDisplayList();
        int index = indexOfId(item.getId());
        if (index >= 0) {
            currentPage = index / pageSize + 1;
        }
        applyPage();
    }

    private void onPaginationChanged() {
        currentPage = paginationBar.getCurrentPage();
        pageSize = paginationBar.getPageSize();
        applyPage();
    }

    private void applyPage() {
        int total = displayList.size();
        int pages = Math.max(1, (int) Math.ceil((double) total / pageSize));
        if (currentPage > pages) {
            currentPage = pages;
        }
        int fromIndex = (currentPage - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        List<T> page = new ArrayList<>();
        if (fromIndex < toIndex) {
            page.addAll(displayList.subList(fromIndex, toIndex));
        }
        if (currentPage == pages) {
            page.add(newPlaceholder());
        }
        setItems(FXCollections.observableArrayList(page));
        paginationBar.update(total, pageSize, currentPage);
    }

    private int lastPage() {
        return Math.max(1, (int) Math.ceil((double) baseList.size() / pageSize));
    }

    private int indexOfId(Long id) {
        for (int i = 0; i < displayList.size(); i++) {
            if (displayList.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private void rebuildDisplayList() {
        displayList.clear();
        displayList.addAll(baseList.stream().filter(filter).toList());
        if (activeSort != null) {
            displayList.sort(activeSort);
        }
    }

    protected void setFilter(Predicate<T> predicate) {
        this.filter = predicate == null ? item -> true : predicate;
        currentPage = 1;
        rebuildDisplayList();
        applyPage();
    }

    private void configureSorting() {
        table.sortPolicyProperty().set(_ -> true);
        table.getSortOrder().addListener((ListChangeListener<TableColumn<T, ?>>) _ -> onSortChanged());
        for (TableColumn<T, ?> column : table.getColumns()) {
            column.sortTypeProperty().addListener((obs, oldType, newType) -> onSortChanged());
        }
    }

    private void onSortChanged() {
        activeSort = buildComparator();
        rebuildDisplayList();
        applyPage();
    }

    private Comparator<T> buildComparator() {
        Comparator<T> comparator = null;
        for (TableColumn<T, ?> column : table.getSortOrder()) {
            if (!column.isSortable()) {
                continue;
            }
            Comparator<T> byColumn = (a, b) -> compareValues(column.getCellData(a), column.getCellData(b));
            if (column.getSortType() == TableColumn.SortType.DESCENDING) {
                byColumn = byColumn.reversed();
            }
            comparator = comparator == null ? byColumn : comparator.thenComparing(byColumn);
        }
        return comparator;
    }

    @SuppressWarnings("unchecked")
    private int compareValues(Object a, Object b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        if (a instanceof Comparable && a.getClass().isInstance(b)) {
            return ((Comparable<Object>) a).compareTo(b);
        }
        return 0;
    }
}
