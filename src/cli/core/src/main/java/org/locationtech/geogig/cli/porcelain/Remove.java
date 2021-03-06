/* Copyright (c) 2012-2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Victor Olaya (Boundless) - initial implementation
 */
package org.locationtech.geogig.cli.porcelain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.locationtech.geogig.cli.AbstractCommand;
import org.locationtech.geogig.cli.CLICommand;
import org.locationtech.geogig.cli.CommandFailedException;
import org.locationtech.geogig.cli.Console;
import org.locationtech.geogig.cli.GeogigCLI;
import org.locationtech.geogig.cli.InvalidParameterException;
import org.locationtech.geogig.porcelain.RemoveOp;
import org.locationtech.geogig.repository.DiffObjectCount;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 *
 */
@Command(name = "rm", description = "Remove features or trees")
public class Remove extends AbstractCommand implements CLICommand {

    /**
     * True if the remove operation should delete the contents of a path in case it resolves to a
     * tree, and tree itself. If a path resolving to a tree is used and this flag is set to false,
     * the path will not be deleted, nor its contents
     */
    @Option(names = { "-r",
            "--recursive" }, description = "Recursively remove trees, including the tree nodes themselves")
    private boolean recursive;

    @Option(names = { "-t", "--truncate" }, description = "Truncate trees, leaving them empty")
    private boolean truncate;

    @Parameters(description = "<path_to_remove>  [<path_to_remove>]...", arity = "1..*")
    private List<String> paths = new ArrayList<String>();

    public @Override void runInternal(GeogigCLI cli) throws IOException {

        Console console = cli.getConsole();

        /* Perform the remove operation */
        RemoveOp op = cli.getGeogig().command(RemoveOp.class).setRecursive(recursive)
                .setTruncate(truncate);

        for (String pathToRemove : paths) {
            op.addPathToRemove(pathToRemove);
        }

        DiffObjectCount result;
        try {
            result = op.setProgressListener(cli.getProgressListener()).call();
        } catch (IllegalArgumentException e) {
            throw new InvalidParameterException(e.getMessage());
        }
        /* And inform about it */
        console.println(String.format("Deleted %,d feature(s)", result.getFeaturesRemoved()));
        if (result.getTreesRemoved() > 0) {
            console.println(String.format("Deleted %,d trees", result.getTreesRemoved()));
        }
        if (result.getFeaturesRemoved() == 0 && result.getTreesRemoved() == 0) {
            throw new CommandFailedException();
        }
    }

}
