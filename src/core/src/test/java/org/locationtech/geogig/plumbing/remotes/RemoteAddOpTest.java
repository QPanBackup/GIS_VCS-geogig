/* Copyright (c) 2012-2017 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Johnathan Garrett (LMN Solutions) - initial implementation
 */
package org.locationtech.geogig.plumbing.remotes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Before;
import org.junit.Test;
import org.locationtech.geogig.repository.Remote;
import org.locationtech.geogig.test.integration.RepositoryTestCase;

public class RemoteAddOpTest extends RepositoryTestCase {

    @Before
    public final void setUpInternal() {
    }

    @Test
    public void testNullName() {
        final RemoteAddOp remoteAdd = repo.command(RemoteAddOp.class);

        assertThrows(RemoteException.class,
                remoteAdd.setName(null).setURL("http://test.com")::call);
    }

    @Test
    public void testEmptyName() {
        final RemoteAddOp remoteAdd = repo.command(RemoteAddOp.class);

        assertThrows(RemoteException.class, remoteAdd.setName("").setURL("http://test.com")::call);
    }

    @Test
    public void testInvalidName() {
        final RemoteAddOp remoteAdd = repo.command(RemoteAddOp.class);

        Exception e = assertThrows(IllegalArgumentException.class,
                remoteAdd.setName("ma..er").setURL("http://test.com")::call);
        assertThat(e.getMessage(),
                containsString("Component of ref cannot have two consecutive dots (..) anywhere."));
    }

    @Test
    public void testNullURL() {
        final RemoteAddOp remoteAdd = repo.command(RemoteAddOp.class);

        assertThrows(RemoteException.class, remoteAdd.setName("myremote").setURL(null)::call);
    }

    @Test
    public void testEmptyURL() {
        final RemoteAddOp remoteAdd = repo.command(RemoteAddOp.class);

        assertThrows(RemoteException.class, remoteAdd.setName("myremote").setURL("")::call);
    }

    @Test
    public void testAddRemoteNullBranch() {
        final RemoteAddOp remoteAdd = repo.command(RemoteAddOp.class);

        String remoteName = "myremote";
        String remoteURL = "http://test.com";

        Remote remote = remoteAdd.setName(remoteName).setURL(remoteURL).setBranch(null).call();

        assertEquals(remoteName, remote.getName());
        assertEquals(remoteURL, remote.getFetchURL());
        assertEquals(remoteURL, remote.getPushURL());
        assertEquals(Remote.defaultRemoteRefSpec(remoteName), remote.getFetchSpec());
    }

    @Test
    public void testAddRemoteEmptyBranch() {
        final RemoteAddOp remoteAdd = repo.command(RemoteAddOp.class);

        String remoteName = "myremote";
        String remoteURL = "http://test.com";

        Remote remote = remoteAdd.setName(remoteName).setURL(remoteURL).setBranch("").call();

        assertEquals(remoteName, remote.getName());
        assertEquals(remoteURL, remote.getFetchURL());
        assertEquals(remoteURL, remote.getPushURL());

        assertEquals(Remote.defaultRemoteRefSpec(remoteName), remote.getFetchSpec());
    }

    @Test
    public void testAddRemoteWithBranch() {
        final RemoteAddOp remoteAdd = repo.command(RemoteAddOp.class);

        String remoteName = "myremote";
        String remoteURL = "http://test.com";
        String branch = "mybranch";

        Remote remote = remoteAdd.setName(remoteName).setURL(remoteURL).setBranch(branch).call();

        assertEquals(remoteName, remote.getName());
        assertEquals(remoteURL, remote.getFetchURL());
        assertEquals(remoteURL, remote.getPushURL());
        assertEquals("+refs/heads/" + branch + ":refs/remotes/" + remoteName + "/" + branch,
                remote.getFetchSpec());
    }

    @Test
    public void testAddRemoteThatExists() {
        final RemoteAddOp remoteAdd = repo.command(RemoteAddOp.class);

        String remoteName = "myremote";
        String remoteURL = "http://test.com";

        Remote remote = remoteAdd.setName(remoteName).setURL(remoteURL).call();

        assertEquals(remoteName, remote.getName());
        assertEquals(remoteURL, remote.getFetchURL());
        assertEquals(remoteURL, remote.getPushURL());
        assertEquals(Remote.defaultRemoteRefSpec(remoteName), remote.getFetchSpec());

        assertThrows(RemoteException.class,
                remoteAdd.setName(remoteName).setURL("someotherurl.com")::call);
    }

    @Test
    public void testAddMultipleRemotes() {
        final RemoteAddOp remoteAdd = repo.command(RemoteAddOp.class);

        String remoteName1 = "myremote";
        String remoteURL1 = "http://test.com";

        String remoteName2 = "myremote2";
        String remoteURL2 = "http://test2.org";

        Remote remote = remoteAdd.setName(remoteName1).setURL(remoteURL1).call();

        assertEquals(remoteName1, remote.getName());
        assertEquals(remoteURL1, remote.getFetchURL());
        assertEquals(remoteURL1, remote.getPushURL());
        assertEquals(Remote.defaultRemoteRefSpec(remoteName1), remote.getFetchSpec());

        remote = remoteAdd.setName(remoteName2).setURL(remoteURL2).call();

        assertEquals(remoteName2, remote.getName());
        assertEquals(remoteURL2, remote.getFetchURL());
        assertEquals(remoteURL2, remote.getPushURL());
    }

}
