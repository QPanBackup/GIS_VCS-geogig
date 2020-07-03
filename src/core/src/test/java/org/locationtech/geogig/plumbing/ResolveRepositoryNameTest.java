/* Copyright (c) 2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Johnathan Garrett (Prominent Edge) - initial implementation
 */
package org.locationtech.geogig.plumbing;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.locationtech.geogig.test.integration.RepositoryTestCase;

/**
 *
 */
public class ResolveRepositoryNameTest extends RepositoryTestCase {

    protected @Override void setUpInternal() throws Exception {
    }

    @Test
    public void testDefault() throws Exception {
        String expected = super.testRepository.getTestMethodName();
        String resolved = repo.command(ResolveRepositoryName.class).call();
        assertEquals(expected, resolved);
    }

    @Test
    public void testConfiguredName() throws Exception {
        final String configRepoName = "myConfiguredRepoName";
        getRepository().context().configDatabase().put("repo.name", configRepoName);
        String repoName = repo.command(ResolveRepositoryName.class).call();
        assertEquals(configRepoName, repoName);
    }

}
