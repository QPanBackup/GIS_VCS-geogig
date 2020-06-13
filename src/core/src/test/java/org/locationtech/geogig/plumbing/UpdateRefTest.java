/* Copyright (c) 2013-2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Johnathan Garrett (LMN Solutions) - initial implementation
 */
package org.locationtech.geogig.plumbing;

import java.util.Optional;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.porcelain.BranchCreateOp;
import org.locationtech.geogig.porcelain.CommitOp;
import org.locationtech.geogig.test.integration.RepositoryTestCase;

public class UpdateRefTest extends RepositoryTestCase {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    protected @Override void setUpInternal() throws Exception {
        repo.context().configDatabase().put("user.name", "groldan");
        repo.context().configDatabase().put("user.email", "groldan@test.com");
    }

    @Test
    public void testConstructorAndMutators() throws Exception {
        insertAndAdd(points1);
        RevCommit commit1 = repo.command(CommitOp.class).call();
        repo.command(BranchCreateOp.class).setName("branch1").call();

        insertAndAdd(points2);
        RevCommit commit2 = repo.command(CommitOp.class).call();
        Optional<Ref> newBranch = repo.command(UpdateRef.class).setName("refs/heads/branch1")
                .setNewValue(commit2.getId()).setOldValue(commit1.getId()).setReason("Testing")
                .call();

        assertTrue(newBranch.get().getObjectId().equals(commit2.getId()));
        assertFalse(newBranch.get().getObjectId().equals(commit1.getId()));
    }

    @Test
    public void testNoName() {
        exception.expect(IllegalStateException.class);
        repo.command(UpdateRef.class).call();
    }

    @Test
    public void testNoValue() {
        exception.expect(IllegalStateException.class);
        repo.command(UpdateRef.class).setName(Ref.MASTER).call();
    }

    @Test
    public void testDeleteRefThatWasASymRef() throws Exception {
        insertAndAdd(points1);
        RevCommit commit1 = repo.command(CommitOp.class).call();
        repo.command(BranchCreateOp.class).setName("branch1").call();

        insertAndAdd(points2);
        RevCommit commit2 = repo.command(CommitOp.class).call();

        repo.command(UpdateSymRef.class).setName("refs/heads/branch1")
                .setOldValue(commit1.getId().toString()).setNewValue(Ref.MASTER).setReason("test")
                .call();

        repo.command(UpdateRef.class).setName("refs/heads/branch1").setNewValue(commit2.getId())
                .setOldValue(Ref.MASTER).setReason("test").call();

        Optional<Ref> branchId = repo.command(RefParse.class).setName("refs/heads/branch1").call();

        assertTrue(branchId.get().getObjectId().equals(commit2.getId()));

        repo.command(UpdateRef.class).setDelete(true).setName("refs/heads/branch1")
                .setReason("test setup").call();
    }

    @Test
    public void testDeleteWithNonexistentName() {
        Optional<Ref> ref = repo.command(UpdateRef.class).setDelete(true).setName("NoRef")
                .setReason("test").call();
        assertFalse(ref.isPresent());
    }
}
