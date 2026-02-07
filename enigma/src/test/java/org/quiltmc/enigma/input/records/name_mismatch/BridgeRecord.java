package org.quiltmc.enigma.input.records.name_mismatch;

import org.quiltmc.enigma.impl.plugin.RecordIndexingService;

import java.util.function.Supplier;

/**
 * {@link #get()} should be found by {@link RecordIndexingService} because it's the only getter candidate: the
 * {@code Object get()} bridge method should not be a candidate because it has the wrong return type and wrong access.
 */
public record BridgeRecord(Double get) implements Supplier<Double> { }
