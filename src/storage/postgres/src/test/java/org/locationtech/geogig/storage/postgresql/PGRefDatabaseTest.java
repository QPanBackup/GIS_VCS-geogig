/* Copyright (c) 2015 Boundless.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Gabriel Roldan (Boundless) - initial implementation
 */
package org.locationtech.geogig.storage.postgresql;

import java.io.IOException;

import org.junit.After;
import org.junit.Rule;
import org.locationtech.geogig.api.Platform;
import org.locationtech.geogig.storage.ConfigDatabase;
import org.locationtech.geogig.storage.RefDatabase;
import org.locationtech.geogig.test.integration.repository.RefDatabaseTest;

import com.google.common.base.Throwables;

public class PGRefDatabaseTest extends RefDatabaseTest {

    @Rule
    public PGTemporaryTestConfig testConfig = new PGTemporaryTestConfig(getClass().getSimpleName());

    ConfigDatabase configdb;

    @Override
    protected RefDatabase createDatabase(Platform platform) throws Exception {
        Environment config = testConfig.getEnvironment();
        PGStorage.createNewRepo(config);
        closeConfigDb();
        configdb = new PGConfigDatabase(config);
        return new PGRefDatabase(configdb, config);
    }

    @After
    public void closeConfigDb() {
        if (configdb != null) {
            try {
                configdb.close();
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
            configdb = null;
        }
    }

}
