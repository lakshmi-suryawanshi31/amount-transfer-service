package com.dws.challenge.util;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.ConcurrentHashMap;

public class AccountLockManager {
    private static final ConcurrentHashMap<String, Lock> accountLocks = new ConcurrentHashMap<>();

    // Get lock for a given account
    public static Lock getLock(String accountId) {
        accountLocks.putIfAbsent(accountId, new ReentrantLock());
        return accountLocks.get(accountId);
    }

    // Clean up the lock if no longer needed (optional)
    public static void cleanUpLock(String accountId) {
        accountLocks.remove(accountId);
    }

}
