/* Copyright (c) 2013-2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Johnathan Garrett (LMN Solutions) - initial implementation
 */
package org.locationtech.geogig.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

import org.junit.Test;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.plumbing.FindCommonAncestor;
import org.locationtech.geogig.porcelain.BranchCreateOp;
import org.locationtech.geogig.porcelain.CheckoutOp;
import org.locationtech.geogig.porcelain.CommitOp;
import org.locationtech.geogig.porcelain.ConfigOp;
import org.locationtech.geogig.porcelain.ConfigOp.ConfigAction;
import org.locationtech.geogig.porcelain.MergeOp;
import org.locationtech.geogig.porcelain.MergeOp.MergeReport;

public class FindCommonAncestorTest extends RepositoryTestCase {

    protected @Override void setUpInternal() throws Exception {
        // These values should be used during a commit to set author/committer
        // TODO: author/committer roles need to be defined better, but for
        // now they are the same thing.
        repo.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setName("user.name")
                .setValue("groldan").call();
        repo.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setName("user.email")
                .setValue("groldan@test.com").call();
    }

    @Test
    public void testFindCommonAncestorCase1() throws Exception {
        // Create the following revision graph
        // o
        // |
        // o - Points 1 added
        // |\
        // | o - branch1 - Points 2 added
        // |
        // o - Points 3 added
        // |
        // o - master - HEAD - Lines 1 added
        insertAndAdd(points1);
        final RevCommit c1 = repo.command(CommitOp.class).setMessage("commit for " + idP1).call();

        // create branch1 and checkout
        repo.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch1").call();
        insertAndAdd(points2);
        final RevCommit left = repo.command(CommitOp.class).setMessage("commit for " + idP2).call();

        // checkout master
        repo.command(CheckoutOp.class).setSource("master").call();
        insertAndAdd(points3);
        repo.command(CommitOp.class).setMessage("commit for " + idP3).call();
        insertAndAdd(lines1);
        final RevCommit right = repo.command(CommitOp.class).setMessage("commit for " + idL1)
                .call();

        Optional<ObjectId> commonAncestor = repo.command(FindCommonAncestor.class).setLeft(left)
                .setRight(right).call();

        assertTrue(commonAncestor.isPresent());
        assertEquals(commonAncestor.get(), c1.getId());

    }

    @Test
    public void testFindCommonAncestorCase2() throws Exception {
        // Create the following revision graph
        // o
        // |
        // o - Points 1 added
        // |\
        // | o - Points 2 added
        // | |
        // | o - Points 3 added
        // | |
        // | o - Lines 2 added - branch1
        // |
        // o - master - HEAD - Lines 1 added
        insertAndAdd(points1);
        final RevCommit c1 = repo.command(CommitOp.class).setMessage("commit for " + idP1).call();

        // create branch1 and checkout
        repo.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch1").call();
        insertAndAdd(points2);
        repo.command(CommitOp.class).setMessage("commit for " + idP2).call();
        insertAndAdd(points3);
        repo.command(CommitOp.class).setMessage("commit for " + idP3).call();
        insertAndAdd(lines2);
        final RevCommit left = repo.command(CommitOp.class).setMessage("commit for " + idL2).call();

        // checkout master
        repo.command(CheckoutOp.class).setSource("master").call();
        insertAndAdd(lines1);
        final RevCommit right = repo.command(CommitOp.class).setMessage("commit for " + idL1)
                .call();

        Optional<ObjectId> commonAncestor = repo.command(FindCommonAncestor.class).setLeft(left)
                .setRight(right).call();

        assertTrue(commonAncestor.isPresent());
        assertEquals(commonAncestor.get(), c1.getId());

    }

    @Test
    public void testFindCommonAncestorCase3() throws Exception {
        // Create the following revision graph
        // o
        // |
        // o - Points 1 added
        // |\
        // | o - Points 2 added
        // | |
        // | o - Points 3 added
        // | |\
        // | | o - Lines 1 added - branch2
        // | |
        // o | - Lines 2 added
        // | |
        // | o - Lines 3 added - branch1
        // |/
        // o - master - HEAD - Merge Commit
        insertAndAdd(points1);
        repo.command(CommitOp.class).setMessage("commit for " + idP1).call();

        // create branch1 and checkout
        repo.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch1").call();
        insertAndAdd(points2);
        repo.command(CommitOp.class).setMessage("commit for " + idP2).call();
        insertAndAdd(points3);
        final RevCommit ancestor = repo.command(CommitOp.class).setMessage("commit for " + idP3)
                .call();
        repo.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch2").call();
        insertAndAdd(lines1);
        final RevCommit branch2 = repo.command(CommitOp.class).setMessage("commit for " + idL1)
                .call();
        repo.command(CheckoutOp.class).setSource("branch1").call();
        insertAndAdd(lines3);
        final RevCommit left = repo.command(CommitOp.class).setMessage("commit for " + idL3).call();

        // checkout master
        repo.command(CheckoutOp.class).setSource("master").call();
        insertAndAdd(lines2);
        repo.command(CommitOp.class).setMessage("commit for " + idL2).call();

        final MergeReport mergeReport = repo.command(MergeOp.class).addCommit(left.getId()).call();

        Optional<ObjectId> commonAncestor = repo.command(FindCommonAncestor.class)
                .setLeft(mergeReport.getMergeCommit()).setRight(branch2).call();

        assertTrue(commonAncestor.isPresent());
        assertEquals(commonAncestor.get(), ancestor.getId());

    }

    @Test
    public void testFindCommonAncestorCase4() throws Exception {
        // Create the following revision graph
        // o
        // |
        // o - Points 1 added
        // |\
        // | o - Points 2 added
        // | |
        // | o - Points 3 added (Ancestor of branch2 and master)
        // | |\
        // | | o - Lines 1 added
        // | | |\
        // | | | o - Polygon 1 added - branch3
        // | | | |
        // | | o | - Polygon 2 added
        // | | |/
        // | | o - Merge Commit - branch2
        // | |
        // o | - Lines 2 added
        // | |
        // | o - Lines 3 added - branch1
        // |/
        // o - master - HEAD - Merge Commit
        insertAndAdd(points1);
        repo.command(CommitOp.class).setMessage("commit for " + idP1).call();

        // create branch1 and checkout
        repo.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch1").call();
        insertAndAdd(points2);
        repo.command(CommitOp.class).setMessage("commit for " + idP2).call();
        insertAndAdd(points3);
        final RevCommit ancestor = repo.command(CommitOp.class).setMessage("commit for " + idP3)
                .call();
        repo.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch2").call();
        insertAndAdd(lines1);
        repo.command(CommitOp.class).setMessage("commit for " + idL1).call();
        repo.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch3").call();
        insertAndAdd(poly1);
        RevCommit branch3 = repo.command(CommitOp.class).setMessage("commit for " + idPG1).call();
        repo.command(CheckoutOp.class).setSource("branch2").call();
        insertAndAdd(poly2);
        repo.command(CommitOp.class).setMessage("commit for " + idPG2).call();

        MergeReport mergeReport = repo.command(MergeOp.class).addCommit(branch3.getId()).call();

        RevCommit branch2 = mergeReport.getMergeCommit();

        repo.command(CheckoutOp.class).setSource("branch1").call();
        insertAndAdd(lines3);
        final RevCommit left = repo.command(CommitOp.class).setMessage("commit for " + idL3).call();

        // checkout master
        repo.command(CheckoutOp.class).setSource("master").call();
        insertAndAdd(lines2);
        repo.command(CommitOp.class).setMessage("commit for " + idL2).call();

        mergeReport = repo.command(MergeOp.class).addCommit(left.getId()).call();

        Optional<ObjectId> commonAncestor = repo.command(FindCommonAncestor.class)
                .setLeft(mergeReport.getMergeCommit()).setRight(branch2).call();

        assertTrue(commonAncestor.isPresent());
        assertEquals(commonAncestor.get(), ancestor.getId());

    }

    @Test
    public void testFindCommonAncestorCase5() throws Exception {
        // Create the following revision graph
        // o - root commit Add Points 1
        // |\
        // | o - commit1 Add Points 2
        // | |\
        // o | | - commit2 Add Points 3
        // | | |
        // | | o - commit3 Modify Points 1
        // | | |
        // | o | - commit4 Add Lines 1
        // | |\|
        // | | o - commit5 Merge commit
        // | | |
        // | o | - commit6 Add Lines 2
        // | | |
        // | o | - commit7 Add Lines 3
        // | | |
        // | o | - commit8 Add Polygon 1
        // | | |
        // | o | - commit9 Add Polygon 2
        // | | |
        // | | o - commit10 Add Polygon 3
        // |/
        // o - commit11 Merge commit

        // root commit
        insertAndAdd(points1);
        repo.command(CommitOp.class).setMessage("root commit").call();

        // commit1
        repo.command(BranchCreateOp.class).setAutoCheckout(true).setName("branch1").call();
        insertAndAdd(points2);
        repo.command(CommitOp.class).setMessage("commit1").call();
        repo.command(BranchCreateOp.class).setAutoCheckout(false).setName("branch2").call();

        // commit2
        repo.command(CheckoutOp.class).setSource("master").call();
        insertAndAdd(points3);
        repo.command(CommitOp.class).setMessage("commit2").call();

        // commit3
        repo.command(CheckoutOp.class).setSource("branch2").call();
        insertAndAdd(points1_modified);
        repo.command(CommitOp.class).setMessage("commit3").call();

        // commit4
        repo.command(CheckoutOp.class).setSource("branch1").call();
        insertAndAdd(lines1);
        ObjectId commit4 = repo.command(CommitOp.class).setMessage("commit4").call().getId();

        // commit5
        repo.command(CheckoutOp.class).setSource("branch2").call();
        repo.command(MergeOp.class).setMessage("commit3").addCommit(commit4).call();

        // commit6
        repo.command(CheckoutOp.class).setSource("branch1").call();
        insertAndAdd(lines2);
        repo.command(CommitOp.class).setMessage("commit6").call();

        // commit7
        insertAndAdd(lines3);
        repo.command(CommitOp.class).setMessage("commit7").call();

        // commit8
        insertAndAdd(poly1);
        repo.command(CommitOp.class).setMessage("commit8").call();

        // commit9
        insertAndAdd(poly2);
        ObjectId commit9 = repo.command(CommitOp.class).setMessage("commit9").call().getId();

        // commit10
        repo.command(CheckoutOp.class).setSource("branch2").call();
        insertAndAdd(poly3);
        RevCommit commit10 = repo.command(CommitOp.class).setMessage("commit10").call();

        // commit11
        repo.command(CheckoutOp.class).setSource("master").call();
        MergeReport report = repo.command(MergeOp.class).setMessage("commit11").addCommit(commit9)
                .call();

        Optional<ObjectId> commonAncestor = repo.command(FindCommonAncestor.class)
                .setLeft(report.getMergeCommit()).setRight(commit10).call();

        assertTrue(commonAncestor.isPresent());
        assertEquals(commonAncestor.get(), commit4);
    }
}
