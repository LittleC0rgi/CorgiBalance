package com.corgibalance.services;

import com.corgibalance.models.Account;
import com.corgibalance.models.AccountFolder;
import com.corgibalance.repositories.AccountFolderRepository;
import com.corgibalance.repositories.AccountRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountFolderService {

    private final AccountRepository accountRepository;
    private final AccountFolderRepository accountFolderRepository;

    public AccountFolderService() {
        this(new AccountRepository(), new AccountFolderRepository());
    }

    public AccountFolderService(AccountRepository accountRepository, AccountFolderRepository accountFolderRepository) {
        this.accountRepository = accountRepository;
        this.accountFolderRepository = accountFolderRepository;
    }

    public List<Account> accounts() {
        return accountRepository.findAll();
    }

    public List<AccountFolder> folders() {
        return accountFolderRepository.findAll();
    }

    public long balance(long accountId) {
        return accountRepository.currentBalance(accountId);
    }

    public void toggle(AccountFolder folder) {
        folder.setExpanded(!folder.isExpanded());
        accountFolderRepository.update(folder);
    }

    public void moveAccount(long accountId, Long folderId) {
        Account account = accountRepository.findById(accountId);
        if (account != null) {
            account.setFolderId(folderId);
            accountRepository.update(account);
        }
    }

    public void moveFolder(long folderId, Long newParentId) {
        AccountFolder folder = accountFolderRepository.findById(folderId);
        if (folder != null) {
            folder.setParentId(newParentId);
            accountFolderRepository.update(folder);
        }
    }

    public AccountFolder create(String name, Long parentId) {
        AccountFolder folder = new AccountFolder();
        folder.setName(name);
        folder.setParentId(parentId);
        return accountFolderRepository.create(folder);
    }

    public void delete(AccountFolder folder) {
        accountFolderRepository.delete(folder);
    }

    public boolean isDescendant(long ancestorId, long folderId) {
        Map<Long, AccountFolder> byId = new HashMap<>();
        for (AccountFolder f : accountFolderRepository.findAll()) {
            byId.put(f.getId(), f);
        }
        Long current = folderId;
        while (current != null) {
            if (current == ancestorId) {
                return true;
            }
            AccountFolder f = byId.get(current);
            current = f == null ? null : f.getParentId();
        }
        return false;
    }
}
