/* Copyright (c) 2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Gabriel Roldan (Boundless) - initial implementation
 */
package org.locationtech.geogig.ql.cli;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Ignore;
import org.junit.Test;
import org.locationtech.geogig.porcelain.CommitOp;
import org.locationtech.geogig.ql.porcelain.QLUpdate;
import org.locationtech.geogig.repository.DiffObjectCount;
import org.locationtech.geogig.test.integration.RepositoryTestCase;

public class QLUpdatetIntegrationTest extends RepositoryTestCase {

    @Override
    public void setUpInternal() throws Exception {
        insertAndAdd(points1);
        insertAndAdd(points2);
        insertAndAdd(lines1);
        insertAndAdd(lines2);
        insertAndAdd(points3);
        insertAndAdd(lines3);

        repo.command(CommitOp.class).call();
    }

    private long update(String statement) {
        DiffObjectCount count = repo.command(QLUpdate.class).setStatement(statement).call().get();
        return count.getFeaturesChanged();
    }

    @Test
    public void simpleUpdate() {
        assertEquals(3, update("update Points set ip = 7"));
    }

    @Test
    public void simpleUpdate2() {
        String statement = "update Points set ip = " + points1.getAttribute("ip");
        assertEquals(2, update(statement));
    }

    @Test
    public void simpleFidFilterUpdate() {
        assertEquals(1, update("update \"HEAD:Points\" set ip = 7 where @id = 'Points.3'"));
        assertEquals(0, update("update \"HEAD:Points\" set ip = 7 where @id = 'nonexistentId'"));
    }

    @Test
    public void updateGeometry() {
        String st = "update Points set pp = 'POINT(-1 -2)' where @id = 'Points.1'";
        assertEquals(1, update(st));
    }

    @Test
    public void updateGeometryFieldWithNonGeometryValue() {
        String st = "update Points set pp = 1200 where @id = 'Points.1'";
        Exception e = assertThrows(IllegalArgumentException.class, () -> update(st));
        assertThat(e.getMessage(), containsString("Unable to convert value"));
    }

    @Test
    @Ignore // REVISIT
    public void updateGeometryWrongType() {
        String st = "update Points set pp = 'LINESTRING(1 1, 2 2)' where @id = 'Points.1'";
        Exception e = assertThrows(IllegalArgumentException.class, () -> update(st));
        assertThat(e.getMessage(), containsString("is not assignable to"));
    }
}
