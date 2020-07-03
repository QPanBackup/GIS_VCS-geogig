/* Copyright (c) 2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Johnathan Garrett (Prominent Edge) - initial implementation
 */
package org.locationtech.geogig.geotools.cli.geopkg;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.locationtech.geogig.cli.CommandFailedException;
import org.locationtech.geogig.cli.Console;
import org.locationtech.geogig.cli.GeogigCLI;
import org.locationtech.geogig.geotools.TestHelper;
import org.locationtech.geogig.repository.Platform;
import org.mockito.exceptions.base.MockitoException;

/**
 *
 */
public class GeoPkgDescribeTest extends Assert {
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private GeogigCLI cli;

    private Console consoleReader;

    private GeoPackageTestSupport support;

    GeopkgDescribe describeCommand;

    @Before
    public void setUp() throws Exception {
        describeCommand = new GeopkgDescribe();
        describeCommand.commonArgs = new GeopkgCommandProxy();
        consoleReader = spy(new Console().disableAnsi());
        cli = spy(new GeogigCLI(consoleReader));
        support = new GeoPackageTestSupport();
        setUpGeogig(cli);
    }

    @After
    public void tearDown() throws Exception {
        cli.close();
    }

    @Test
    public void testDescribe() throws Exception {
        describeCommand.commonArgs.database = support.createDefaultTestData().getAbsolutePath();
        describeCommand.table = "table1";
        describeCommand.support.dataStoreFactory = TestHelper.createTestFactory();
        describeCommand.run(cli);
    }

    @Test
    public void testInvalidDatabaseParams() throws Exception {
        describeCommand.commonArgs.database = "nonexistent.gpkg";
        describeCommand.table = "table1";
        assertThrows(IllegalArgumentException.class, () -> describeCommand.run(cli));
    }

    @Test
    public void testDescribeNonexistentTable() throws Exception {
        describeCommand.commonArgs.database = support.createDefaultTestData().getAbsolutePath();
        describeCommand.table = "nonexistent";
        describeCommand.support.dataStoreFactory = TestHelper.createTestFactory();
        assertThrows(CommandFailedException.class, () -> describeCommand.run(cli));
    }

    @Test
    public void testNoTable() throws Exception {
        describeCommand.commonArgs.database = support.newFile().getAbsolutePath();
        describeCommand.table = "";
        describeCommand.support.dataStoreFactory = TestHelper.createTestFactory();
        assertThrows(CommandFailedException.class, () -> describeCommand.run(cli));
    }

    @Test
    public void testNullDataStore() throws Exception {
        describeCommand.commonArgs.database = support.newFile().getAbsolutePath();
        describeCommand.table = "table1";
        describeCommand.support.dataStoreFactory = TestHelper.createNullTestFactory();
        assertThrows(CommandFailedException.class, () -> describeCommand.run(cli));
    }

    @Test
    public void testDescribeException() throws Exception {
        when(cli.getConsole()).thenThrow(new MockitoException("Exception"));
        describeCommand.commonArgs.database = support.newFile().getAbsolutePath();
        describeCommand.table = "table1";
        describeCommand.support.dataStoreFactory = TestHelper.createTestFactory();
        assertThrows(MockitoException.class, () -> describeCommand.run(cli));
    }

    @Test
    public void testFlushException() throws Exception {
        doThrow(new IOException("Exception")).when(consoleReader).flush();
        describeCommand.commonArgs.database = support.newFile().getAbsolutePath();
        describeCommand.table = "table1";
        describeCommand.support.dataStoreFactory = TestHelper.createTestFactory();
        assertThrows(RuntimeException.class, () -> describeCommand.run(cli));
    }

    private void setUpGeogig(GeogigCLI cli) throws Exception {
        final File userhome = tempFolder.newFolder("mockUserHomeDir");
        final File workingDir = tempFolder.newFolder("mockWorkingDir");
        tempFolder.newFolder("mockWorkingDir", ".geogig");

        final Platform platform = mock(Platform.class);
        when(platform.pwd()).thenReturn(workingDir);
        when(platform.getUserHome()).thenReturn(userhome);

        cli.setPlatform(platform);
    }
}
