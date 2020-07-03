/* Copyright (c) 2012-2017 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Johnathan Garrett (LMN Solutions) - initial implementation
 */
package org.locationtech.geogig.test.integration.remoting;

import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.model.RevTag;
import org.locationtech.geogig.plumbing.MapRef;
import org.locationtech.geogig.plumbing.RefParse;
import org.locationtech.geogig.plumbing.remotes.RemoteAddOp;
import org.locationtech.geogig.plumbing.remotes.RemoteRemoveOp;
import org.locationtech.geogig.plumbing.remotes.RemoteResolve;
import org.locationtech.geogig.porcelain.BranchCreateOp;
import org.locationtech.geogig.porcelain.BranchDeleteOp;
import org.locationtech.geogig.porcelain.CheckoutOp;
import org.locationtech.geogig.porcelain.CommitOp;
import org.locationtech.geogig.porcelain.LogOp;
import org.locationtech.geogig.porcelain.TagCreateOp;
import org.locationtech.geogig.porcelain.TagListOp;
import org.locationtech.geogig.remotes.CloneOp;
import org.locationtech.geogig.remotes.FetchOp;
import org.locationtech.geogig.remotes.TransferSummary;
import org.locationtech.geogig.repository.Remote;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.test.TestSupport;

import com.google.common.collect.Lists;

/**
 * {@link FetchOp} integration test suite for full clones (for shallow and sparse clones see
 * {@link ShallowCloneTest} and {@link SparseCloneTest})
 *
 */
public class FetchOpTest extends RemoteRepositoryTestCase {

    LinkedList<RevCommit> expectedMaster;

    LinkedList<RevCommit> expectedBranch;

    protected Remote origin, upstream;

    private Optional<Ref> originMaster, originBranch1, originTag;

    private Optional<Ref> upstreamMaster, upstreamBranch1, upstreamTag;

    private Optional<Ref> localOriginMaster;

    protected @Override void setUpInternal() throws Exception {
        // clone the repository
        CloneOp clone = cloneOp();
        // clone.setRepositoryURL(remoteGeogig.envHome.toURI().toString()).call();
        Repository cloned = clone.setRemoteURI(originRepo.getLocation())
                .setCloneURI(localRepo.getLocation()).call();

        // Commit several features to the remote

        expectedMaster = new LinkedList<RevCommit>();
        expectedBranch = new LinkedList<RevCommit>();

        insertAndAdd(originRepo, points1);
        RevCommit commit = originRepo.command(CommitOp.class).call();
        expectedMaster.addFirst(commit);
        expectedBranch.addFirst(commit);

        // Create and checkout branch1
        originRepo.command(BranchCreateOp.class).setAutoCheckout(true).setName("Branch1").call();

        // Commit some changes to branch1
        insertAndAdd(originRepo, points2);
        commit = originRepo.command(CommitOp.class).call();
        expectedBranch.addFirst(commit);

        insertAndAdd(originRepo, points3);
        commit = originRepo.command(CommitOp.class).call();
        expectedBranch.addFirst(commit);

        // Make sure Branch1 has all of the commits
        Iterator<RevCommit> logs = originRepo.command(LogOp.class).call();
        List<RevCommit> logged = Lists.newArrayList(logs);
        assertEquals(expectedBranch, logged);

        // Checkout master and commit some changes
        originRepo.command(CheckoutOp.class).setSource("master").call();

        insertAndAdd(originRepo, lines1);
        commit = originRepo.command(CommitOp.class).call();
        expectedMaster.addFirst(commit);

        insertAndAdd(originRepo, lines2);
        commit = originRepo.command(CommitOp.class).call();
        expectedMaster.addFirst(commit);

        originRepo.command(TagCreateOp.class) //
                .setMessage("TestTag") //
                .setCommitId(commit.getId()) //
                .setName("test") //
                .call();

        // Make sure master has all of the commits
        logs = originRepo.command(LogOp.class).call();
        logged = Lists.newArrayList(logs);
        assertEquals(expectedMaster, logged);

        upstreamRepo.command(CloneOp.class).setRemoteURI(originRepo.getLocation())
                .setRemoteName("origin").call();

        upstream = localRepo.command(RemoteAddOp.class).setName("upstream")
                .setURL(upstreamRepo.getLocation().toString()).call();

        origin = localRepo.command(RemoteResolve.class).setName(REMOTE_NAME).call().get();

        originMaster = Optional.of(toRemote(origin, getRef(originRepo, "master").get()));
        originBranch1 = Optional.of(toRemote(origin, getRef(originRepo, "Branch1").get()));
        originTag = Optional.of(toRemote(origin, getRef(originRepo, "test").get()));

        upstreamMaster = Optional.of(toRemote(upstream, getRef(upstreamRepo, "master").get()));
        upstreamBranch1 = Optional.of(toRemote(upstream, getRef(upstreamRepo, "Branch1").get()));
        upstreamTag = Optional.of(toRemote(upstream, getRef(upstreamRepo, "test").get()));

        localOriginMaster = getRef(localRepo, "refs/remotes/origin/master");
    }

    private Ref toRemote(Remote remote, Ref local) {
        return localRepo.command(MapRef.class).setRemote(remote).add(local).convertToRemote().call()
                .get(0);
    }

    private void verifyFetch() throws Exception {
        // Make sure the local repository got all of the commits from master
        localRepo.command(CheckoutOp.class).setSource("refs/remotes/origin/master").call();
        Iterator<RevCommit> logs = localRepo.command(LogOp.class).call();
        List<RevCommit> logged = Lists.newArrayList(logs);

        assertEquals(expectedMaster, logged);

        // Make sure the local repository got all of the commits from Branch1
        localRepo.command(CheckoutOp.class).setSource("refs/remotes/origin/Branch1").call();
        logs = localRepo.command(LogOp.class).call();
        logged = Lists.newArrayList(logs);
        assertEquals(expectedBranch, logged);

        List<RevTag> tags = localRepo.command(TagListOp.class).call();
        assertEquals(1, tags.size());

        TestSupport.verifyRepositoryContents(localRepo);
    }

    private void verifyPrune() throws Exception {
        // Make sure the local repository got all of the commits from master
        localRepo.command(CheckoutOp.class).setForce(true).setSource("refs/remotes/origin/master")
                .call();
        Iterator<RevCommit> logs = localRepo.command(LogOp.class).call();
        List<RevCommit> logged = Lists.newArrayList(logs);

        assertEquals(expectedMaster, logged);

        // Make sure the local repository no longer has Branch1
        Optional<Ref> missing = localRepo.command(RefParse.class)
                .setName("refs/remotes/origin/Branch1").call();

        assertFalse(missing.isPresent());
    }

    @Test
    public void testFetchNoArgsDefaultsToOrigin() throws Exception {
        // fetch from the remote
        FetchOp fetch = fetchOp();

        TransferSummary summary = fetch.call();
        assertNotNull(summary);
        assertEquals(1, summary.getRefDiffs().size());
        assertTrue(summary.getRefDiffs().containsKey(origin.getFetchURL()));
        assertSummary(summary, origin.getFetchURL(), localOriginMaster, originMaster);
        assertSummary(summary, origin.getFetchURL(), empty(), originBranch1);
        assertSummary(summary, origin.getFetchURL(), empty(), originTag);
        verifyFetch();
    }

    @Test
    public void testFetchAll() throws Exception {
        // fetch from the remote
        FetchOp fetch = fetchOp();
        TransferSummary summary = fetch.setAllRemotes(true).call();
        assertEquals(2, summary.getRefDiffs().size());
        assertTrue(summary.getRefDiffs().containsKey(origin.getFetchURL()));
        assertTrue(summary.getRefDiffs().containsKey(upstream.getFetchURL()));

        assertSummary(summary, origin.getFetchURL(), localOriginMaster, originMaster);
        assertSummary(summary, origin.getFetchURL(), empty(), originBranch1);
        assertSummary(summary, origin.getFetchURL(), empty(), originTag);

        assertSummary(summary, upstream.getFetchURL(), empty(), upstreamMaster);
        assertSummary(summary, upstream.getFetchURL(), empty(), upstreamBranch1);

        verifyFetch();
    }

    @Test
    public void testFetchSingleRef() throws Exception {
        final Repository remote = this.originRepo;
        final Repository local = this.localRepo;

        remote.command(BranchCreateOp.class).setName("branch2").setAutoCheckout(true).call();
        insertAndAdd(remote, lines1_modified, lines2_modified, lines3_modified);
        RevCommit branch2Tip = commit(remote, "modified lines on branch2");
        checkout(remote, "master");

        remote.command(BranchCreateOp.class).setName("branch3").setAutoCheckout(true).call();
        insertAndAdd(remote, points1_modified, points2_modified, points3_modified);
        RevCommit branch3Tip = commit(remote, "modified points on branch3");
        checkout(remote, "master");

        Optional<Ref> originBranch2 = Optional
                .of(toRemote(origin, getRef(remote, "branch2").get()));

        // fetch from the remote
        FetchOp fetch = fetchOp().setAutofetchTags(false);

        Remote singleRefOrigin = origin.fetch("refs/heads/branch2");

        TransferSummary summary = fetch.addRemote(singleRefOrigin).call();

        assertFalse(getRef(local, "refs/heads/branch2").isPresent());
        assertTrue(getRef(local, "refs/remotes/origin/branch2").isPresent());
        assertFalse(getRef(local, "branch3").isPresent());
        assertEquals(branch2Tip.getId(),
                getRef(local, "refs/remotes/origin/branch2").get().getObjectId());

        assertEquals(1, summary.getRefDiffs().size());
        assertTrue(summary.getRefDiffs().containsKey(origin.getFetchURL()));
        assertEquals(1, summary.getRefDiffs().get(origin.getFetchURL()).size());
        assertSummary(summary, origin.getFetchURL(), empty(), originBranch2);
        TestSupport.verifyRepositoryContents(local, "refs/remotes/origin/branch2");

    }

    public @Test void testFetchRespectsTargeRef() throws Exception {
        final Repository remote = this.originRepo;
        final Repository local = this.localRepo;

        remote.command(BranchCreateOp.class).setName("branch2").setAutoCheckout(true).call();
        insertAndAdd(remote, lines1_modified, lines2_modified, lines3_modified);
        RevCommit branch2Tip = commit(remote, "modified lines on branch2");
        checkout(remote, "master");

        remote.command(BranchCreateOp.class).setName("branch3").setAutoCheckout(true).call();
        insertAndAdd(remote, points1_modified, points2_modified, points3_modified);
        RevCommit branch3Tip = commit(remote, "modified points on branch3");
        checkout(remote, "master");

        // fetch from the remote, fetching remote's branch2 to local's refs/custom/branch2 and
        // branch3 to refs/custom/branch3 explicitly
        FetchOp fetch = fetchOp().setAutofetchTags(false);
        final String refSpec = "refs/heads/branch2:refs/custom/branch2;refs/heads/branch3:refs/custom/branch3;";
        Remote singleRefOrigin = origin.fetch(refSpec);

        TransferSummary summary = fetch.addRemote(singleRefOrigin).call();

        assertFalse(getRef(local, "refs/heads/branch2").isPresent());
        assertFalse(getRef(local, "refs/heads/branch3").isPresent());
        assertFalse(getRef(local, "refs/remotes/origin/branch2").isPresent());
        assertFalse(getRef(local, "refs/remotes/origin/branch3").isPresent());

        assertTrue(getRef(local, "refs/custom/branch2").isPresent());
        assertTrue(getRef(local, "refs/custom/branch2").isPresent());
        assertEquals(branch2Tip.getId(), getRef(local, "refs/custom/branch2").get().getObjectId());
        assertEquals(branch3Tip.getId(), getRef(local, "refs/custom/branch3").get().getObjectId());

        assertEquals(1, summary.getRefDiffs().size());
        assertTrue(summary.getRefDiffs().containsKey(origin.getFetchURL()));
        assertEquals(2, summary.getRefDiffs().get(origin.getFetchURL()).size());
        TestSupport.verifyRepositoryContents(local, "refs/custom/branch2", "refs/custom/branch3");
    }

    @Test
    public void testFetchSpecificRemote() throws Exception {
        // fetch from the remote
        FetchOp fetch = fetchOp();
        TransferSummary summary = fetch.addRemote("upstream").call();
        assertEquals(1, summary.getRefDiffs().size());
        assertTrue(summary.getRefDiffs().containsKey(upstream.getFetchURL()));

        assertSummary(summary, upstream.getFetchURL(), empty(), upstreamMaster);
        assertSummary(summary, upstream.getFetchURL(), empty(), upstreamTag);
        assertSummary(summary, upstream.getFetchURL(), empty(), upstreamBranch1);

        TestSupport.verifySameContents(upstreamRepo, localRepo);
    }

    @Test
    public void testFetchSpecificRemoteAndAll() throws Exception {
        // fetch from the remote
        FetchOp fetch = fetchOp();
        TransferSummary summary = fetch.addRemote("upstream").setAllRemotes(true).call();

        assertEquals(2, summary.getRefDiffs().size());
        assertTrue(summary.getRefDiffs().containsKey(origin.getFetchURL()));
        assertTrue(summary.getRefDiffs().containsKey(upstream.getFetchURL()));

        assertSummary(summary, origin.getFetchURL(), localOriginMaster, originMaster);
        assertSummary(summary, origin.getFetchURL(), empty(), originBranch1);
        assertSummary(summary, origin.getFetchURL(), empty(), originTag);

        assertSummary(summary, upstream.getFetchURL(), empty(), upstreamMaster);
        assertSummary(summary, upstream.getFetchURL(), empty(), upstreamBranch1);

        verifyFetch();
    }

    @Test
    public void testFetchNoRemotes() throws Exception {
        localRepo.command(RemoteRemoveOp.class).setName(REMOTE_NAME).call();
        FetchOp fetch = fetchOp();
        Exception e = assertThrows(IllegalArgumentException.class, fetch::call);
        assertThat(e.getMessage(), containsString("Remote could not be resolved"));
    }

    @Test
    public void testFetchNoChanges() throws Exception {
        // fetch from the remote
        FetchOp fetch = fetchOp();
        TransferSummary summary = fetch.addRemote("origin").call();
        assertEquals(1, summary.getRefDiffs().size());
        assertTrue(summary.getRefDiffs().containsKey(origin.getFetchURL()));
        assertSummary(summary, origin.getFetchURL(), localOriginMaster, originMaster);
        assertSummary(summary, origin.getFetchURL(), empty(), originBranch1);
        assertSummary(summary, origin.getFetchURL(), empty(), originTag);
        verifyFetch();

        // fetch again
        summary = fetch.call();
        assertTrue(summary.toString(), summary.isEmpty());
    }

    @Test
    public void testFetchWithPrune() throws Exception {
        // fetch from the remote
        FetchOp fetch = fetchOp();
        fetch.addRemote("origin").setAllRemotes(true).call();

        verifyFetch();
        Optional<Ref> localOriginBranch1 = getRef(localRepo, "refs/remotes/origin/Branch1");
        assertTrue(localOriginBranch1.isPresent());

        // Remove a branch from the remote
        originRepo.command(BranchDeleteOp.class).setName("Branch1").call();

        // fetch again
        fetch = fetchOp();
        TransferSummary summary = fetch.setPrune(true).call();
        assertSummary(summary, origin.getFetchURL(), localOriginBranch1, empty());

        verifyPrune();
    }

    @Test
    public void testFetchWithPruneAndBranchAdded() throws Exception {
        // fetch from the remote
        FetchOp fetch = fetchOp();
        fetch.addRemote("origin").setAllRemotes(true).call();

        verifyFetch();

        Optional<Ref> localOriginBranch1 = getRef(localRepo, "refs/remotes/origin/Branch1");
        assertTrue(localOriginBranch1.isPresent());

        // Remove a branch from the remote
        originRepo.command(BranchDeleteOp.class).setName("Branch1").call();

        // Add another branch
        Ref branch2 = originRepo.command(BranchCreateOp.class).setName("Branch2").call();

        // fetch again
        fetch = fetchOp();
        TransferSummary summary = fetch.setPrune(true).call();
        assertEquals(1, summary.getRefDiffs().size());
        assertTrue(summary.getRefDiffs().containsKey(origin.getFetchURL()));
        assertSummary(summary, origin.getFetchURL(), localOriginBranch1, empty());

        Ref expectedNew = toRemote(origin, branch2);
        assertSummary(summary, origin.getFetchURL(), null, expectedNew);

        verifyPrune();

        Optional<Ref> pruned = getRef(localRepo, "refs/remotes/origin/Branch1");
        assertFalse(pruned.isPresent());
        Optional<Ref> missing = getRef(localRepo, "refs/remotes/origin/Branch2");
        assertTrue(missing.isPresent());
    }
}
