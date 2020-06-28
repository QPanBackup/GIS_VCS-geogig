/* Copyright (c) 2013-2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Victor Olaya (Boundless) - initial implementation
 */
package org.locationtech.geogig.cli.plumbing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.locationtech.geogig.cli.AbstractCommand;
import org.locationtech.geogig.cli.CLICommand;
import org.locationtech.geogig.cli.Console;
import org.locationtech.geogig.cli.GeogigCLI;
import org.locationtech.geogig.cli.annotation.ReadOnly;
import org.locationtech.geogig.dsl.Geogig;
import org.locationtech.geogig.model.ObjectId;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.model.RevObject;
import org.locationtech.geogig.plumbing.FindCommonAncestor;
import org.locationtech.geogig.plumbing.RevObjectParse;

import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

/**
 * Outputs the common ancestor of 2 commits
 * 
 */
@ReadOnly
@Command(name = "merge-base", description = "Outputs the common ancestor of 2 commits")
public class MergeBase extends AbstractCommand implements CLICommand {

    /**
     * The commits to use for computing the common ancestor
     * 
     */
    @Parameters(description = "<commit> <commit>")
    private List<String> commits = new ArrayList<String>();

    public @Override void runInternal(GeogigCLI cli) throws IOException {
        checkParameter(commits.size() == 2, "Two commit references must be provided");

        Console console = cli.getConsole();
        Geogig geogig = cli.getGeogig();

        Optional<RevObject> left = geogig.command(RevObjectParse.class).setRefSpec(commits.get(0))
                .call();
        checkParameter(left.isPresent(), commits.get(0) + " does not resolve to any object.");
        checkParameter(left.get() instanceof RevCommit,
                commits.get(0) + " does not resolve to a commit");
        Optional<RevObject> right = geogig.command(RevObjectParse.class).setRefSpec(commits.get(1))
                .call();
        checkParameter(right.isPresent(), commits.get(1) + " does not resolve to any object.");
        checkParameter(right.get() instanceof RevCommit,
                commits.get(1) + " does not resolve to a commit");
        Optional<ObjectId> ancestor = geogig.command(FindCommonAncestor.class)
                .setLeft((RevCommit) left.get()).setRight((RevCommit) right.get()).call();
        checkParameter(ancestor.isPresent(), "No common ancestor was found.");

        console.print(ancestor.get().toString());
    }

}
