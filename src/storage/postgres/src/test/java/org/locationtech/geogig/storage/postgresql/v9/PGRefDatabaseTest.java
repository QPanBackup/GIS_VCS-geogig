/* Copyright (c) 2015-2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Gabriel Roldan (Boundless) - initial implementation
 */
package org.locationtech.geogig.storage.postgresql.v9;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.locationtech.geogig.repository.Platform;
import org.locationtech.geogig.storage.RefDatabase;
import org.locationtech.geogig.storage.postgresql.config.Environment;
import org.locationtech.geogig.storage.postgresql.config.PGStorage;
import org.locationtech.geogig.storage.postgresql.config.PGTemporaryTestConfig;
import org.locationtech.geogig.storage.postgresql.config.PGTestDataSourceProvider;
import org.locationtech.geogig.test.integration.repository.RefDatabaseTest;

public class PGRefDatabaseTest extends RefDatabaseTest {

    public static @ClassRule PGTestDataSourceProvider ds = new PGTestDataSourceProvider();

    public @Rule PGTemporaryTestConfig testConfig = new PGTemporaryTestConfig(
            getClass().getSimpleName(), ds);

    Environment mainEnvironment;

    protected @Override RefDatabase createDatabase(Platform platform) throws Exception {
        mainEnvironment = testConfig.getEnvironment();
        PGStorage.createNewRepo(mainEnvironment);
        return new PGRefDatabase(mainEnvironment);
    }

    @Test
    public void testLockSingleRepo() throws Exception {
        PGRefDatabase pgRefDb = PGRefDatabase.class.cast(refDb);
        Callable<Long> lockTask = new Callable<Long>() {
            public @Override Long call() throws TimeoutException {
                pgRefDb.lockWithTimeout(5);
                return 0L;
            }
        };
        Callable<Long> unlockTask = new Callable<Long>() {
            public @Override Long call() throws TimeoutException {
                pgRefDb.unlock();
                return 0L;
            }
        };
        ExecutorService firstThread = Executors.newSingleThreadExecutor();
        ExecutorService secondThread = Executors.newSingleThreadExecutor();
        // Lock on first thread
        firstThread.submit(lockTask).get();

        // Second thread should time out
        try {
            secondThread.submit(lockTask).get();
        } catch (ExecutionException e) {
            assertTrue(e.getCause() instanceof TimeoutException);
        }

        // Unlock first thread
        firstThread.submit(unlockTask).get();

        // Second thread should be able to lock
        secondThread.submit(lockTask).get();

        // Unlock second thread
        secondThread.submit(unlockTask).get();

        firstThread.shutdown();
        secondThread.shutdown();
    }

    @Test
    public void testLockMultiRepo() throws Exception {
        // Create a second repository in the same database.
        Environment secondEnvironment = mainEnvironment
                .withRepository(getClass().getSimpleName() + "2");
        PGStorage.createNewRepo(secondEnvironment);

        PGRefDatabase secondRefDb = new PGRefDatabase(secondEnvironment);
        secondRefDb.open();
        PGRefDatabase firstRefDb = PGRefDatabase.class.cast(refDb);
        Callable<Long> lockFirstRepo = new Callable<Long>() {
            public @Override Long call() throws TimeoutException {
                firstRefDb.lockWithTimeout(5);
                return 0L;
            }
        };
        Callable<Long> unlockFirstRepo = new Callable<Long>() {
            public @Override Long call() throws TimeoutException {
                firstRefDb.unlock();
                return 0L;
            }
        };
        Callable<Long> lockSecondRepo = new Callable<Long>() {
            public @Override Long call() throws TimeoutException {
                secondRefDb.lockWithTimeout(5);
                return 0L;
            }
        };
        Callable<Long> unlockSecondRepo = new Callable<Long>() {
            public @Override Long call() throws TimeoutException {
                secondRefDb.unlock();
                return 0L;
            }
        };
        ExecutorService firstThread = Executors.newSingleThreadExecutor();
        ExecutorService secondThread = Executors.newSingleThreadExecutor();
        // Lock first repo
        firstThread.submit(lockFirstRepo).get();

        // Second thread should be able to lock second repo
        secondThread.submit(lockSecondRepo).get();

        // Unlock first repo
        firstThread.submit(unlockFirstRepo).get();

        // Unlock second repo
        secondThread.submit(unlockSecondRepo).get();

        firstThread.shutdown();
        secondThread.shutdown();

        secondRefDb.close();

    }

}
