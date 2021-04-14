package com.github.taven.logminer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransactionalBuffer {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionalBuffer.class);
    private final Map<String, Transaction> transactions;
    private BigInteger lastCommittedScn;

    public TransactionalBuffer() {
        this.transactions = new HashMap<>();
        this.lastCommittedScn = BigInteger.ZERO;
    }

    public interface CommitCallback {
        void execute(BigInteger smallestScn, BigInteger commitScn, int callbackNumber) throws InterruptedException;
    }

    private static final class Transaction {
        private final BigInteger firstScn;
        private BigInteger lastScn;
        private final List<CommitCallback> commitCallbacks;

        private Transaction(BigInteger firstScn) {
            this.firstScn = firstScn;
            this.commitCallbacks = new ArrayList<>();
            this.lastScn = firstScn;
        }

        @Override
        public String toString() {
            return "Transaction{" +
                    "firstScn=" + firstScn +
                    ", lastScn=" + lastScn +
                    '}';
        }
    }

    public boolean isEmpty() {
        return this.transactions.isEmpty();
    }

    private BigInteger calculateSmallestScn() {
        return transactions.isEmpty() ? null
                : transactions.values()
                .stream()
                .map(transaction -> transaction.firstScn)
                .min(BigInteger::compareTo)
                .orElseThrow(() -> new RuntimeException("Cannot calculate smallest SCN"));
    }

    public void registerCommitCallback(String transactionId, BigInteger scn, Instant changeTime, CommitCallback callback) {
        transactions.computeIfAbsent(transactionId, s -> new Transaction(scn));

        Transaction transaction = transactions.get(transactionId);
        if (transaction != null) {
            transaction.commitCallbacks.add(callback);
        }
    }

    public boolean commit(String txId, BigInteger commitScn, OffsetContext offsetScn) {
        Transaction transaction = transactions.remove(txId);
        if (transaction == null) {
            return false;
        }

        BigInteger smallestScn = calculateSmallestScn();

        if (offsetScn.committedScn > commitScn.longValue() || lastCommittedScn.longValue() > commitScn.longValue()) {
            LOGGER.warn("txId {} already commit, ignore.", txId);
            return false;
        }

        int counter = transaction.commitCallbacks.size();
        for (CommitCallback callback : transaction.commitCallbacks) {
            try {
                callback.execute(smallestScn, commitScn, --counter);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }

        lastCommittedScn = commitScn;
        return true;
    }

    public boolean rollback(String txId) {
        return transactions.remove(txId) != null;
    }

}
