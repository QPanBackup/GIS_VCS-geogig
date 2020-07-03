/* Copyright (c) 2013-2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Juan Marin (Boundless) - initial implementation
 */
package org.locationtech.geogig.geotools.plumbing;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.locationtech.geogig.geotools.TestHelper;

public class ListOpTest {

    @Test
    public void testNullDataStore() throws Exception {
        ListOp list = new ListOp();
        assertThrows(GeoToolsOpException.class, list::call);
    }

    @Test
    public void testEmptyDataStore() throws Exception {
        ListOp list = new ListOp();
        list.setDataStore(
                TestHelper.createEmptyTestFactory().createDataStore(Collections.emptyMap()));
        Optional<List<String>> features = list.call();
        assertFalse(features.isPresent());
    }

    @Test
    public void testTypeNameException() throws Exception {
        ListOp list = new ListOp();
        list.setDataStore(TestHelper.createFactoryWithGetNamesException()
                .createDataStore(Collections.emptyMap()));

        assertThrows(GeoToolsOpException.class, list::call);
    }

    @Test
    public void testList() throws Exception {
        ListOp list = new ListOp();
        list.setDataStore(TestHelper.createTestFactory().createDataStore(Collections.emptyMap()));
        Optional<List<String>> features = list.call();
        assertTrue(features.isPresent());

        assertTrue(features.get().contains("table1"));
        assertTrue(features.get().contains("table2"));
    }
}
