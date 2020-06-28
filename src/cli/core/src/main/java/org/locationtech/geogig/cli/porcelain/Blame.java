/* Copyright (c) 2013-2016 Boundless and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Victor Olaya (Boundless) - initial implementation
 */
package org.locationtech.geogig.cli.porcelain;

import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.YELLOW;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.fusesource.jansi.Ansi;
import org.locationtech.geogig.cli.AbstractCommand;
import org.locationtech.geogig.cli.Console;
import org.locationtech.geogig.cli.GeogigCLI;
import org.locationtech.geogig.cli.InvalidParameterException;
import org.locationtech.geogig.cli.annotation.ReadOnly;
import org.locationtech.geogig.dsl.Geogig;
import org.locationtech.geogig.model.RevCommit;
import org.locationtech.geogig.porcelain.BlameException;
import org.locationtech.geogig.porcelain.BlameOp;
import org.locationtech.geogig.porcelain.BlameReport;
import org.locationtech.geogig.porcelain.ValueAndCommit;
import org.locationtech.geogig.storage.text.TextValueSerializer;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Shows information about the commits and authors that have modified the current attributes of a
 * given feature
 * 
 */
@ReadOnly
@Command(name = "blame", description = "Shows information about authors of modifications for a single feature")
public class Blame extends AbstractCommand {

    /**
     * The path to the element to analyze.
     */
    @Parameters(description = "Path to the feature to bleam (e.g. roads/1)")
    private List<String> featurePath = new ArrayList<String>();

    @Option(names = { "--porcelain" }, description = "Use porcelain output format")
    private boolean porcelain = false;

    @Option(names = { "--no-values" }, description = "Do not show values, only attribute names")
    private boolean noValues = false;

    public @Override void runInternal(GeogigCLI cli) throws IOException {
        checkParameter(featurePath.size() < 2, "Only one path allowed");
        checkParameter(!featurePath.isEmpty(), "A path must be specified");

        Console console = cli.getConsole();
        Geogig geogig = cli.getGeogig();

        String path = featurePath.get(0);

        try {
            BlameReport report = geogig.command(BlameOp.class).setPath(path).call();

            Map<String, ValueAndCommit> changes = report.getChanges();
            Iterator<String> iter = changes.keySet().iterator();
            while (iter.hasNext()) {
                String attrib = iter.next();
                ValueAndCommit valueAndCommit = changes.get(attrib);
                RevCommit commit = valueAndCommit.commit;
                Optional<?> value = valueAndCommit.value;
                if (porcelain) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(attrib).append(' ');
                    sb.append(commit.getId().toString()).append(' ');
                    sb.append(commit.getAuthor().getName().orElse("")).append(' ');
                    sb.append(commit.getAuthor().getEmail().orElse("")).append(' ');
                    sb.append(Long.toString(commit.getAuthor().getTimestamp())).append(' ');
                    sb.append(Integer.toString(commit.getAuthor().getTimeZoneOffset()));
                    if (!noValues) {
                        sb.append(" ").append(TextValueSerializer
                                .asString(Optional.of((Object) value.orElse(null))));
                    }
                    console.println(sb.toString());
                } else {
                    Ansi ansi = newAnsi(console);
                    ansi.fg(GREEN).a(attrib + ": ").reset();
                    if (!noValues) {
                        String s = value.isPresent() ? value.get().toString() : "NULL";
                        ansi.fg(YELLOW).a(s).a(" ").reset();
                    }
                    ansi.a(commit.getId().toString().substring(0, 8)).a(" ");
                    ansi.a(commit.getAuthor().getName().orElse("")).a(" ");
                    ansi.a(commit.getAuthor().getEmail().orElse("")).a(" ");
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                    String date = formatter.format(new Date(commit.getAuthor().getTimestamp()
                            + commit.getAuthor().getTimeZoneOffset()));
                    ansi.a(date);
                    console.println(ansi.toString());
                }
            }
        } catch (BlameException e) {
            switch (e.statusCode) {
            case FEATURE_NOT_FOUND:
                throw new InvalidParameterException("The supplied path does not exist", e);
            case PATH_NOT_FEATURE:
                throw new InvalidParameterException(
                        "The supplied path does not resolve to a feature", e);

            }
        }
    }
}
