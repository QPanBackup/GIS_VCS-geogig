/* Copyright (c) 2013-2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Johnathan Garrett (LMN Solutions) - initial implementation
 */
package org.locationtech.geogig.model.impl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.locationtech.geogig.model.RevFeatureType;
import org.locationtech.geogig.model.RevObject;
import org.locationtech.geogig.model.RevObjectTestUtil;
import org.locationtech.geogig.model.RevObjects;
import org.locationtech.geogig.test.integration.RepositoryTestCase;

public class RevFeatureTypeTest extends RepositoryTestCase {

    protected @Override void setUpInternal() throws Exception {
        repo.context().configDatabase().put("user.name", "groldan");
        repo.context().configDatabase().put("user.email", "groldan@test.com");
    }

    @Test
    public void testConstructorAndAccessors() {
        RevFeatureType featureType = RevFeatureType.builder().type(linesType).build();

        assertEquals(RevObject.TYPE.FEATURETYPE, featureType.getType());

        RevObjectTestUtil.deepEquals(RevFeatureType.builder().type(linesType).build(), featureType);
    }

    @Test
    public void testToString() {
        RevFeatureType featureType = RevFeatureType.builder().type(linesType).build();

        String actual = featureType.toString();
        String expected = RevObjects.toString(featureType);
        assertEquals(expected, actual);
    }
}
