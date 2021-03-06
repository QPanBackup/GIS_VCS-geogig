/* Copyright (c) 2020 Gabriel Roldan
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Distribution License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/edl-v10.html
 *
 * Contributors:
 * Gabriel Roldan - factored out of IniFileConfigDatabase
 */
package org.locationtech.geogig.storage.fs;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

import org.locationtech.geogig.storage.AbstractStore;
import org.locationtech.geogig.storage.ConfigException;
import org.locationtech.geogig.storage.ConfigException.StatusCode;
import org.locationtech.geogig.storage.ConfigStore;

public class IniFileConfigStore extends AbstractStore implements ConfigStore {

    /**
     * Access it through {@link #iniFile()}, not directly.
     */
    private INIFile local;

    private final Supplier<File> configFile;

    private BooleanSupplier createIfMissing;

    public IniFileConfigStore(final Supplier<File> file, BooleanSupplier readOnly,
            BooleanSupplier createIfMissing) {

        super(readOnly.getAsBoolean());
        this.configFile = file;
        this.createIfMissing = createIfMissing;
    }

    private INIFile iniFile() {
        if (local == null) {
            File configFile = this.configFile.get();
            if (!configFile.exists()) {
                if (createIfMissing.getAsBoolean()) {
                    File parent = configFile.getParentFile();
                    if (!parent.exists() && !parent.mkdirs()) {
                        throw new ConfigException(StatusCode.CANNOT_WRITE);
                    }
                    try {
                        configFile.createNewFile();
                    } catch (IOException e) {
                        throw new ConfigException(e, StatusCode.CANNOT_WRITE);
                    }
                } else {
                    throw new ConfigException(StatusCode.INVALID_LOCATION);
                }
            }
            this.local = INIFile.forFile(configFile);
        }
        return this.local;
    }

    public Optional<String> get(String key) {
        try {
            String[] parsed = parse(key);
            Optional<String> valueOpt = iniFile().get(parsed[0], parsed[1]);
            if (valueOpt.isPresent() && valueOpt.get().length() > 0) {
                return valueOpt;
            } else {
                return Optional.empty();
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new ConfigException(e, StatusCode.SECTION_OR_KEY_INVALID);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e, null);
        } catch (IOException e) {
            throw new ConfigException(e, null);
        }
    }

    public <T> Optional<T> get(String key, Class<T> c) {
        Optional<String> text = get(key);
        if (text.isPresent()) {
            return Optional.of(cast(c, text.get()));
        } else {
            return Optional.empty();
        }
    }

    public Map<String, String> getAll() {
        try {
            return iniFile().getAll();
        } catch (StringIndexOutOfBoundsException e) {
            throw new ConfigException(e, StatusCode.SECTION_OR_KEY_INVALID);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e, null);
        } catch (IOException e) {
            throw new ConfigException(e, null);
        }
    }

    public Map<String, String> getAllSection(String section) {
        try {
            return iniFile().getSection(section);
        } catch (StringIndexOutOfBoundsException e) {
            throw new ConfigException(e, StatusCode.SECTION_OR_KEY_INVALID);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e, null);
        } catch (IOException e) {
            throw new ConfigException(e, null);
        }
    }

    public List<String> getAllSubsections(String section) {
        try {
            return iniFile().listSubsections(section);
        } catch (StringIndexOutOfBoundsException e) {
            throw new ConfigException(e, StatusCode.SECTION_OR_KEY_INVALID);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e, null);
        } catch (IOException e) {
            throw new ConfigException(e, null);
        }
    }

    public void put(String key, Object value) {
        String[] parsed = parse(key);
        try {
            iniFile().set(parsed[0], parsed[1], stringify(value));
        } catch (StringIndexOutOfBoundsException e) {
            throw new ConfigException(e, StatusCode.SECTION_OR_KEY_INVALID);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e, null);
        } catch (IOException e) {
            throw new ConfigException(e, null);
        }
    }

    public void remove(String key) {
        String[] parsed = parse(key);
        try {
            iniFile().remove(parsed[0], parsed[1]);
        } catch (StringIndexOutOfBoundsException e) {
            throw new ConfigException(e, StatusCode.SECTION_OR_KEY_INVALID);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e, null);
        } catch (IOException e) {
            throw new ConfigException(e, null);
        }
    }

    public void removeSection(String key) {
        try {
            iniFile().removeSection(key);
        } catch (NoSuchElementException e) {
            throw new ConfigException(e, StatusCode.MISSING_SECTION);
        } catch (StringIndexOutOfBoundsException e) {
            throw new ConfigException(e, StatusCode.SECTION_OR_KEY_INVALID);
        } catch (IllegalArgumentException e) {
            throw new ConfigException(e, null);
        } catch (IOException e) {
            throw new ConfigException(e, null);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T cast(Class<T> c, String s) {
        if (String.class.equals(c)) {
            return c.cast(s);
        }
        if (int.class.equals(c) || Integer.class.equals(c)) {
            return (T) Integer.valueOf(s);
        }
        if (Boolean.class.equals(c)) {
            return c.cast(Boolean.valueOf(s));
        }
        throw new IllegalArgumentException("Unsupported type: " + c);
    }

    private String stringify(Object o) {
        return o == null ? "" : o.toString();
    }

    private String[] parse(String qualifiedKey) {
        if (qualifiedKey == null) {
            throw new IllegalArgumentException("Config key may not be null.");
        }
        int splitAt = qualifiedKey.lastIndexOf(".");
        return new String[] { qualifiedKey.substring(0, splitAt),
                qualifiedKey.substring(splitAt + 1) };
    }
}
