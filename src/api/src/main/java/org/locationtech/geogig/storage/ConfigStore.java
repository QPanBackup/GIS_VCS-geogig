/* Copyright (c) 2020 Gabriel Roldan
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Gabriel Roldan - initial implementation
 */
package org.locationtech.geogig.storage;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import lombok.NonNull;

/**
 * Provides an interface for implementations of config databases, which manage GeoGig config files.
 * 
 * @since 2.0
 */
public interface ConfigStore extends Store {

    /**
     * Queries the repository config file for a particular name.
     * 
     * @param key String in "section.key" format to query for
     * @return The value of the key if found, otherwise an empty Optional
     * @throws ConfigException if an error is encountered
     */
    public Optional<String> get(String key);

    /**
     * Queries the repository config file for a particular name.
     * 
     * @param key String in "section.key" format to query for
     * @param c The type to return the value as
     * @return The value of the key if found, otherwise an empty Optional
     * @throws IllegalArgumentException if unable to return value as type c
     * @throws ConfigException if an error is encountered
     */
    public <T> Optional<T> get(String key, Class<T> c);

    /**
     * Builds and returns a map with all of the values from the repository config file.
     * 
     * @return A map which contains all of the contents of the config file.
     * @throws ConfigException if an error is encountered
     */
    public Map<String, String> getAll();

    /**
     * Builds and returns a map with all of the values from the section in the repository config
     * file.
     * 
     * @return A map which contains all of the contents of the given section.
     * @throws ConfigException if an error is encountered
     */
    public Map<String, String> getAllSection(String section);

    /**
     * @return A list which contains all of the subsections for a given section in the repository
     *         config file.
     * @throws ConfigException if an error is encountered
     */
    public List<String> getAllSubsections(String section);

    /**
     * Sets a value in the repository config file
     * 
     * @param key String in "section.key" format to set
     * @param value The value to set
     * @throws ConfigException if an error is encountered
     */
    public void put(String key, Object value);

    public default void putSection(@NonNull String section,
            final @NonNull Map<String, String> kvp) {
        kvp.forEach((k, v) -> {
            Objects.requireNonNull(k);
            Objects.requireNonNull(v);
            put(String.format("%s.%s", section, k), v);
        });
    }

    /**
     * Removes a value from the repository config file
     * 
     * @param key String in "section.key" format to set
     * @throws ConfigException if an error is encountered
     */
    public void remove(String key);

    /**
     * Removes a section from the repository config file
     * 
     * @param key String in "section" format to set
     * @throws ConfigException if an error is encountered
     */
    public void removeSection(String key);
}
