package com.corgibalance.components;

import com.corgibalance.models.Account;
import com.corgibalance.models.AccountFolder;
import com.corgibalance.services.AccountFolderService;
import com.corgibalance.services.CurrencyConverter;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class AccountListComponent {

    private static final DataFormat ACCOUNT_ID_DATA = new DataFormat("application/x-corgibalance-account-id");
    private static final DataFormat FOLDER_ID_DATA = new DataFormat("application/x-corgibalance-folder-id");

    private final AccountFolderService service;
    private final CurrencyConverter converter;
    private final Runnable onChanged;
    private final HBox unassignedHeader;

    public AccountListComponent(AccountFolderService service, CurrencyConverter converter, Runnable onChanged) {
        this.service = service;
        this.converter = converter;
        this.onChanged = onChanged;
        this.unassignedHeader = createUnassignedHeader();
    }

    public void render(VBox accountList, Long baseCurrencyId) {
        accountList.getChildren().clear();
        List<Account> accounts = service.accounts().stream()
                .filter(account -> !account.isHidden())
                .toList();
        List<AccountFolder> folders = service.folders();
        Set<Long> folderIds = folders.stream().map(AccountFolder::getId).collect(Collectors.toSet());

        Map<Long, List<AccountFolder>> childrenByParent = new HashMap<>();
        for (AccountFolder f : folders) {
            Long pid = f.getParentId();
            childrenByParent.computeIfAbsent(pid, _ -> new ArrayList<>()).add(f);
        }

        List<Account> unassigned = accounts.stream()
                .filter(account -> account.getFolderId() == null || !folderIds.contains(account.getFolderId()))
                .toList();
        for (Account account : unassigned) {
            accountList.getChildren().add(accountRow(account));
        }
        renderFolderTree(childrenByParent, accounts, null, 0, accountList, baseCurrencyId);
        accountList.getChildren().add(unassignedHeader);
        unassignedHeader.setVisible(false);
        unassignedHeader.setManaged(false);
    }

    public void addFolder() {
        addSubfolder(null);
    }

    private void renderFolderTree(Map<Long, List<AccountFolder>> childrenByParent,
                                  List<Account> accounts, Long parentId, int depth, VBox accountList,
                                  Long baseCurrencyId) {
        List<AccountFolder> children = childrenByParent.getOrDefault(parentId, List.of());
        for (AccountFolder folder : children) {
            List<Account> inFolder = accounts.stream()
                    .filter(account -> folder.getId().equals(account.getFolderId()))
                    .toList();
            accountList.getChildren().add(folderHeader(folder, inFolder, depth, baseCurrencyId));
            if (folder.isExpanded()) {
                for (Account account : inFolder) {
                    accountList.getChildren().add(accountRow(account, depth + 1));
                }
                renderFolderTree(childrenByParent, accounts, folder.getId(), depth + 1, accountList, baseCurrencyId);
            }
        }
    }

    private Node folderHeader(AccountFolder folder, List<Account> accounts, int depth, Long baseCurrencyId) {
        long total = 0;
        for (Account account : accounts) {
            long balance = service.balance(account.getId());
            total += converter.convert(balance, account.getCurrencyId(), baseCurrencyId);
        }

        HeroIcon icon = new HeroIcon(folder.isExpanded() ? HeroIcon.Icon.FOLDER : HeroIcon.Icon.FOLDER_PLUS);
        icon.getStyleClass().add("account-folder__icon");
        Label name = new Label(folder.getName());
        name.getStyleClass().add("account-folder__name");
        Label amount = new Label(converter.format(total, baseCurrencyId));
        amount.getStyleClass().add("account-folder__amount");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(icon, name, spacer, amount);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(6);
        header.getStyleClass().add("account-folder");
        if (depth > 0) {
            header.setStyle("-fx-padding: 6 10 6 " + (12 + depth * 16) + ";");
        }
        header.setOnMouseClicked(_ -> toggleFolder(folder));
        dropTarget(header, folder.getId());
        folderDragSource(header, folder);

        MenuItem addSubfolder = new MenuItem("Add subfolder");
        addSubfolder.setOnAction(_ -> addSubfolder(folder));
        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(_ -> deleteFolder(folder));
        ContextMenu menu = new ContextMenu(addSubfolder, deleteItem);
        header.setOnContextMenuRequested(e -> {
            menu.show(header, e.getScreenX(), e.getScreenY());
            e.consume();
        });

        return header;
    }

    private void toggleFolder(AccountFolder folder) {
        Dialogs.runSafely(() -> service.toggle(folder), onChanged);
    }

    private HBox createUnassignedHeader() {
        Label name = new Label("No folder");
        name.getStyleClass().add("account-folder__name");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(name, spacer);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setSpacing(6);
        header.getStyleClass().addAll("account-folder", "account-folder--muted");
        dropTarget(header, null);
        return header;
    }

    private Node accountRow(Account account) {
        return accountRow(account, 0);
    }

    private Node accountRow(Account account, int depth) {
        long balance = service.balance(account.getId());
        Label name = new Label(account.getName());
        name.getStyleClass().add("card__text");
        Label amount = new Label(converter.format(balance, account.getCurrencyId()));
        amount.getStyleClass().add("card__text");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(name, spacer, amount);
        row.getStyleClass().add("account-row");
        if (depth > 0) {
            row.setStyle("-fx-padding: 2 0 2 " + (12 + depth * 16) + ";");
        }
        dragSource(row, account);
        return row;
    }

    private void dragSource(Node node, Account account) {
        node.setOnDragDetected(event -> {
            Dragboard dragboard = node.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.put(ACCOUNT_ID_DATA, String.valueOf(account.getId()));
            dragboard.setContent(content);
            dragboard.setDragView(node.snapshot(null, null));
            unassignedHeader.setVisible(true);
            unassignedHeader.setManaged(true);
            event.consume();
        });
        node.setOnDragDone(event -> {
            unassignedHeader.setVisible(false);
            unassignedHeader.setManaged(false);
        });
    }

    private void folderDragSource(Node node, AccountFolder folder) {
        node.setOnDragDetected(event -> {
            Dragboard dragboard = node.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.put(FOLDER_ID_DATA, String.valueOf(folder.getId()));
            dragboard.setContent(content);
            dragboard.setDragView(node.snapshot(null, null));
            event.consume();
        });
    }

    private void dropTarget(Node node, Long folderId) {
        node.setOnDragOver(event -> {
            if (event.getGestureSource() != node
                    && (event.getDragboard().hasContent(ACCOUNT_ID_DATA) || event.getDragboard().hasContent(FOLDER_ID_DATA))) {
                event.acceptTransferModes(TransferMode.MOVE);
                node.getStyleClass().add("account-folder--drag-over");
            }
            event.consume();
        });
        node.setOnDragExited(event -> node.getStyleClass().remove("account-folder--drag-over"));
        node.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;
            try {
                if (dragboard.hasContent(ACCOUNT_ID_DATA)) {
                    long accountId = Long.parseLong((String) dragboard.getContent(ACCOUNT_ID_DATA));
                    service.moveAccount(accountId, folderId);
                    success = true;
                } else if (dragboard.hasContent(FOLDER_ID_DATA)) {
                    long draggedFolderId = Long.parseLong((String) dragboard.getContent(FOLDER_ID_DATA));
                    if (folderId != null && draggedFolderId != folderId && !service.isDescendant(draggedFolderId, folderId)) {
                        service.moveFolder(draggedFolderId, folderId);
                        success = true;
                    } else if (folderId == null && draggedFolderId != 0) {
                        service.moveFolder(draggedFolderId, null);
                        success = true;
                    }
                }
                if (success) {
                    onChanged.run();
                }
            } catch (RuntimeException e) {
                Dialogs.showError(e);
            }
            node.getStyleClass().remove("account-folder--drag-over");
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private void addSubfolder(AccountFolder parent) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(parent == null ? "New folder" : "New subfolder");
        dialog.setHeaderText(null);
        dialog.setContentText("Folder name:");
        Dialogs.style(dialog, "btn--primary");
        Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) {
            return;
        }
        String name = result.get().trim();
        if (name.isEmpty()) {
            return;
        }
        Dialogs.runSafely(() -> service.create(name, parent != null ? parent.getId() : null), onChanged);
    }

    private void deleteFolder(AccountFolder folder) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete folder");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete folder \"" + folder.getName() + "\"? Accounts and subfolders will become unassigned.");
        Dialogs.style(confirm, "btn--danger");
        if (confirm.showAndWait().filter(r -> r == ButtonType.OK).isEmpty()) {
            return;
        }
        Dialogs.runSafely(() -> service.delete(folder), onChanged);
    }
}
