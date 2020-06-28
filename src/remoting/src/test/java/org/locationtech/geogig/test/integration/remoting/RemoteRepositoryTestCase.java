/* Copyright (c) 2013-2017 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Johnathan Garrett (LMN Solutions) - initial implementation
 */
package org.locationtech.geogig.test.integration.remoting;

import static java.util.Optional.ofNullable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.eclipse.jdt.annotation.Nullable;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.locationtech.geogig.dsl.Geogig;
import org.locationtech.geogig.feature.Feature;
import org.locationtech.geogig.feature.FeatureType;
import org.locationtech.geogig.feature.FeatureTypes;
import org.locationtech.geogig.feature.Name;
import org.locationtech.geogig.model.NodeRef;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.Ref;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.model.RevFeature;
import org.locationtech.geogig.model.RevFeatureType;
import org.locationtech.geogig.plumbing.RefParse;
import org.locationtech.geogig.plumbing.remotes.RemoteAddOp;
import org.locationtech.geogig.porcelain.AddOp;
import org.locationtech.geogig.porcelain.BranchCreateOp;
import org.locationtech.geogig.porcelain.CheckoutOp;
import org.locationtech.geogig.porcelain.CommitOp;
import org.locationtech.geogig.porcelain.LogOp;
import org.locationtech.geogig.porcelain.MergeOp;
import org.locationtech.geogig.porcelain.MergeOp.MergeReport;
import org.locationtech.geogig.remotes.CloneOp;
import org.locationtech.geogig.remotes.FetchOp;
import org.locationtech.geogig.remotes.LsRemoteOp;
import org.locationtech.geogig.remotes.PullOp;
import org.locationtech.geogig.remotes.PushOp;
import org.locationtech.geogig.remotes.RefDiff;
import org.locationtech.geogig.remotes.TransferSummary;
import org.locationtech.geogig.repository.DefaultProgressListener;
import org.locationtech.geogig.repository.FeatureInfo;
import org.locationtech.geogig.repository.ProgressListener;
import org.locationtech.geogig.repository.Repository;
import org.locationtech.geogig.repository.RepositoryConnectionException;
import org.locationtech.geogig.repository.WorkingTree;
import org.locationtech.geogig.test.TestRepository;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public abstract class RemoteRepositoryTestCase {

    protected static final ProgressListener SIMPLE_PROGRESS = new DefaultProgressListener() {
        public @Override void setDescription(String msg, Object... args) {
            System.err.printf(msg + "\n", args);
        }
    };

    protected static final String REMOTE_NAME = "origin";

    protected static final String idL1 = "Lines.1";

    protected static final String idL2 = "Lines.2";

    protected static final String idL3 = "Lines.3";

    protected static final String idP1 = "Points.1";

    protected static final String idP2 = "Points.2";

    protected static final String idP3 = "Points.3";

    protected static final String pointsNs = "http://geogig.points";

    protected static final String pointsName = "Points";

    protected static final String pointsTypeSpec = "sp:String,ip:Integer,pp:Point:srid=4326";

    protected static final Name pointsTypeName = Name.valueOf("http://geogig.points", pointsName);

    protected FeatureType pointsType;

    protected Feature points1;

    protected Feature points1_modified;

    protected Feature points2;

    protected Feature points2_modified;

    protected Feature points3;

    protected Feature points3_modified;

    protected static final String linesNs = "http://geogig.lines";

    protected static final String linesName = "Lines";

    protected static final String linesTypeSpec = "sp:String,ip:Integer,pp:LineString:srid=4326";

    protected static final Name linesTypeName = Name.valueOf("http://geogig.lines", linesName);

    protected FeatureType linesType;

    protected Feature lines1;

    protected Feature lines1_modified;

    protected Feature lines2;

    protected Feature lines2_modified;

    protected Feature lines3;

    protected Feature lines3_modified;

    public @Rule TestRepository repositorySupport = new TestRepository();

    public Repository localRepo;

    public Repository originRepo;

    public Repository upstreamRepo;

    // prevent recursion
    private boolean setup = false;

    @Before
    public final void setUp() throws Exception {
        if (setup) {
            throw new IllegalStateException("Are you calling super.setUp()!?");
        }

        setup = true;
        doSetUp();
    }

    protected final void doSetUp() throws Exception {
        localRepo = repositorySupport.repository();
        originRepo = repositorySupport.createAndInitRepository("remotetestrepository");
        upstreamRepo = repositorySupport.createAndInitRepository("upstream");
        {
            String remoteURI = originRepo.getLocation().toString();
            localRepo.command(RemoteAddOp.class).setName(REMOTE_NAME).setURL(remoteURI).call();
            upstreamRepo.command(RemoteAddOp.class).setName(REMOTE_NAME).setURL(remoteURI).call();
        }

        pointsType = FeatureTypes.createType(pointsTypeName.toString(), pointsTypeSpec.split(","));

        points1 = feature(pointsType, idP1, "StringProp1_1", Integer.valueOf(1000), "POINT(1 1)");
        points1_modified = feature(pointsType, idP1, "StringProp1_1a", Integer.valueOf(1001),
                "POINT(1 2)");
        points2 = feature(pointsType, idP2, "StringProp1_2", Integer.valueOf(2000), "POINT(2 2)");
        points2_modified = feature(pointsType, idP2, "StringProp1_2a", Integer.valueOf(2001),
                "POINT(2 3)");
        points3 = feature(pointsType, idP3, "StringProp1_3", Integer.valueOf(3000), "POINT(3 3)");
        points3_modified = feature(pointsType, idP3, "StringProp1_3a", Integer.valueOf(3001),
                "POINT(3 4)");

        linesType = FeatureTypes.createType(linesTypeName.toString(), linesTypeSpec.split(","));

        lines1 = feature(linesType, idL1, "StringProp2_1", Integer.valueOf(1000),
                "LINESTRING (1 1, 2 2)");
        lines1_modified = feature(linesType, idL1, "StringProp2_1a", Integer.valueOf(1001),
                "LINESTRING (1 2, 2 2)");
        lines2 = feature(linesType, idL2, "StringProp2_2", Integer.valueOf(2000),
                "LINESTRING (3 3, 4 4)");
        lines2_modified = feature(linesType, idL2, "StringProp2_2a", Integer.valueOf(2001),
                "LINESTRING (3 4, 4 4)");
        lines3 = feature(linesType, idL3, "StringProp2_3", Integer.valueOf(3000),
                "LINESTRING (5 5, 6 6)");
        lines3_modified = feature(linesType, idL3, "StringProp2_3a", Integer.valueOf(3001),
                "LINESTRING (5 6, 6 6)");

        setUpInternal();
    }

    protected LsRemoteOp lsremoteOp() {
        return localRepo.command(LsRemoteOp.class);
    }

    protected FetchOp fetchOp() throws RepositoryConnectionException {
        return localRepo.command(FetchOp.class);
    }

    protected CloneOp cloneOp() {
        return localRepo.command(CloneOp.class).setRemoteName(REMOTE_NAME);
    }

    protected PullOp pullOp() {
        return localRepo.command(PullOp.class);
    }

    protected PushOp pushOp() throws RepositoryConnectionException {
        return localRepo.command(PushOp.class);
    }

    @After
    public final void tearDown() throws Exception {
        setup = false;
        tearDownInternal();
    }

    /**
     * Called as the last step in {@link #setUp()}
     */
    protected abstract void setUpInternal() throws Exception;

    /**
     * Called before {@link #tearDown()}, subclasses may override as appropriate
     */
    protected void tearDownInternal() throws Exception {
        //
    }

    protected Feature feature(FeatureType type, String id, Object... values) {
        Feature feature = Feature.build(id, type);
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (type.getDescriptor(i).isGeometryDescriptor()) {
                if (value instanceof String) {
                    value = geom((String) value);
                }
            }
            feature.setAttribute(i, value);
        }
        return feature;
    }

    protected List<RevCommit> populate(Geogig geogig, boolean oneCommitPerFeature,
            Feature... features) throws Exception {
        return populate(geogig, oneCommitPerFeature, Arrays.asList(features));
    }

    protected List<RevCommit> populate(Geogig geogig, boolean oneCommitPerFeature,
            List<Feature> features) throws Exception {

        List<RevCommit> commits = new ArrayList<RevCommit>();

        for (Feature f : features) {
            insertAndAdd(geogig, f);
            if (oneCommitPerFeature) {
                RevCommit commit = geogig.command(CommitOp.class).call();
                commits.add(commit);
            }
        }

        if (!oneCommitPerFeature) {
            RevCommit commit = geogig.command(CommitOp.class).call();
            commits.add(commit);
        }

        return commits;
    }

    /**
     * Inserts the Feature to the index and stages it to be committed.
     */
    protected ObjectId insertAndAdd(Geogig geogig, Feature f) throws Exception {
        ObjectId objectId = insert(geogig, f);

        add(geogig);
        return objectId;
    }

    protected ObjectId insertAndAdd(Repository geogig, Feature f) throws Exception {
        ObjectId objectId = insert(geogig, f);

        add(geogig);
        return objectId;
    }

    protected void insertAndAdd(Repository geogig, Feature... f) throws Exception {
        insert(geogig, f);
        add(geogig);
    }

    /**
     * Inserts the feature to the index but does not stages it to be committed
     */
    protected ObjectId insert(Geogig geogig, Feature f) throws Exception {
        return insert(geogig.getRepository(), f);
    }

    protected ObjectId insert(Repository repo, Feature f) throws Exception {
        final WorkingTree workTree = repo.context().workingTree();
        Name name = f.getType().getName();
        String parentPath = name.getLocalPart();
        RevFeatureType type = RevFeatureType.builder().type(f.getType()).build();
        repo.context().objectDatabase().put(type);
        String path = NodeRef.appendChild(parentPath, f.getId());
        FeatureInfo fi = FeatureInfo.insert(RevFeature.builder().build(f), type.getId(), path);
        workTree.insert(fi);
        return fi.getFeature().getId();
    }

    protected void insertAndAdd(Geogig geogig, Feature... features) throws Exception {
        insert(geogig, features);
        add(geogig);
    }

    protected void add(Geogig geogig) {
        add(geogig.getRepository());
    }

    protected void add(Repository repo) {
        repo.command(AddOp.class).call();
    }

    protected Geometry geom(String wkt) {
        try {
            return new WKTReader().read(wkt);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    protected void insert(Repository repo, Iterable<? extends Feature> features) throws Exception {
        WorkingTree workingTree = repo.context().workingTree();

        FeatureType type = features.iterator().next().getType();

        repo.context().objectDatabase().put(RevFeatureType.builder().type(type).build());

        final String treePath = type.getName().getLocalPart();

        Iterable<FeatureInfo> featureInfos = Iterables.transform(features,
                (f) -> featureInfo(treePath, f));

        workingTree.insert(featureInfos.iterator(), new DefaultProgressListener());
    }

    protected void insert(Geogig geogig, Feature... features) throws Exception {
        for (Feature f : features) {
            insert(geogig, f);
        }
    }

    protected void insert(Repository geogig, Feature... features) throws Exception {
        for (Feature f : features) {
            insert(geogig, f);
        }
    }

    public FeatureInfo featureInfo(String treePath, Feature f) {
        final String path = NodeRef.appendChild(treePath, f.getId());
        RevFeature feature = RevFeature.builder().build(f);
        FeatureType type = f.getType();
        RevFeatureType ftype = RevFeatureType.builder().type(type).build();
        return FeatureInfo.insert(feature, ftype.getId(), path);
    }

    /**
     * Deletes a feature from the index
     * 
     * @param f
     * @return
     * @throws Exception
     */
    protected boolean deleteAndAdd(Geogig geogig, Feature f) throws Exception {
        boolean existed = delete(geogig, f);
        if (existed) {
            add(geogig);
        }

        return existed;
    }

    protected boolean delete(Geogig geogig, Feature f) throws Exception {
        final WorkingTree workTree = geogig.getRepository().context().workingTree();
        Name name = f.getType().getName();
        String localPart = name.getLocalPart();
        String id = f.getId();
        boolean existed = workTree.delete(localPart, id);
        return existed;
    }

    protected void delete(Repository repo, Iterable<? extends Feature> features) throws Exception {
        final WorkingTree workTree = repo.context().workingTree();

        Iterator<String> featurePaths = Iterators.transform(features.iterator(),
                (f) -> f.getType().getName().getLocalPart() + "/" + f.getId());
        workTree.delete(featurePaths, new DefaultProgressListener());
    }

    protected <E> List<E> toList(Iterator<E> logs) {
        List<E> logged = new ArrayList<E>();
        Iterators.addAll(logged, logs);
        return logged;
    }

    protected <E> List<E> toList(Iterable<E> logs) {
        List<E> logged = new ArrayList<E>();
        Iterables.addAll(logged, logs);
        return logged;
    }

    protected void createBranch(Repository repo, String branch) {
        repo.command(BranchCreateOp.class).setAutoCheckout(true).setName(branch)
                .setProgressListener(SIMPLE_PROGRESS).call();
    }

    protected void checkout(Repository repo, String branch) {
        repo.command(CheckoutOp.class).setSource(branch).call();
    }

    protected MergeReport mergeNoFF(Repository repo, String branch, String mergeMessage,
            boolean mergeOurs) {
        Ref branchRef = repo.command(RefParse.class).setName(branch).call().get();
        ObjectId updatesBranchTip = branchRef.getObjectId();
        MergeReport mergeReport = repo.command(MergeOp.class)//
                .setMessage(mergeMessage)//
                .setNoFastForward(true)//
                .addCommit(updatesBranchTip)//
                .setOurs(mergeOurs)//
                .setTheirs(!mergeOurs)//
                .setProgressListener(SIMPLE_PROGRESS)//
                .call();
        return mergeReport;
    }

    /**
     * Computes the aggregated bounds of {@code features}, assuming all of them are in the same CRS
     */
    protected Envelope boundsOf(Feature... features) {
        Envelope bounds = null;
        for (int i = 0; i < features.length; i++) {
            Feature f = features[i];
            if (bounds == null) {
                bounds = f.getDefaultGeometryBounds();
            } else {
                bounds.expandToInclude(f.getDefaultGeometryBounds());
            }
        }
        return bounds;
    }

    /**
     * Computes the aggregated bounds of {@code features} in the {@code targetCrs}
     */
    // protected Envelope boundsOf(CoordinateReferenceSystem targetCrs, Feature... features)
    // throws Exception {
    // ReferencedEnvelope bounds = new ReferencedEnvelope(targetCrs);
    //
    // for (int i = 0; i < features.length; i++) {
    // Feature f = features[i];
    // BoundingBox fbounds = f.getBounds();
    // if (!CRS.equalsIgnoreMetadata(targetCrs, fbounds)) {
    // fbounds = fbounds.toBounds(targetCrs);
    // }
    // bounds.include(fbounds);
    // }
    // return bounds;
    // }

    public RevCommit commit(Repository repo, String msg) {
        return repo.command(CommitOp.class).setMessage(msg).call();
    }

    protected Optional<Ref> getRef(Repository repo, String refspec) {
        return repo.command(RefParse.class).setName(refspec).call();
    }

    protected void assertSummary(TransferSummary result, String remoteURL, @Nullable Ref before,
            @Nullable Ref after) {
        assertSummary(result, remoteURL, ofNullable(before), ofNullable(after));
    }

    protected void assertSummary(TransferSummary result, String remoteURL, Optional<Ref> before,
            Optional<Ref> after) {
        assertNotNull(result);
        Collection<RefDiff> diffs = result.getRefDiffs().get(remoteURL);
        assertNotNull(diffs);
        String name = before.orElseGet(after::get).getName();
        RefDiff diff = Maps.uniqueIndex(diffs, (d) -> d.oldRef().orElse(d.getNewRef()).getName())
                .get(name);
        assertNotNull(diff);
        assertEquals(before, diff.oldRef());
        assertEquals(after, diff.newRef());
    }

    protected List<RevCommit> log(Repository repo) {
        return Lists.newArrayList(repo.command(LogOp.class).call());
    }
}
