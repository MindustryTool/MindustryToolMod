# reactive-map-signal Specification

## Purpose
Provides a reactive map primitive (`MapSignal<K, V>`) implementing `Readable<Map<K, V>>` with fine-grained key-level reactivity, targeted observer invalidation, and complete isolation from ambient UI component lifecycles.

## Requirements

### Requirement: Whole-Map Reactive Observation
The `MapSignal<K, V>` primitive SHALL implement `Readable<Map<K, V>>` and provide reactive access to the entire underlying map state via `get()` and non-reactive access via `peek()`. Calling `get()` within an active reactive context SHALL register the caller as an observer for any structural or entry change to the map.

#### Scenario: Observing entire map contents
- **WHEN** an observer reads `mapSignal.get()` inside an active reactive context
- **THEN** the observer receives an unmodifiable view of the current map entries and is notified whenever any entry is added, updated, or removed

#### Scenario: Non-reactive map inspection
- **WHEN** `mapSignal.peek()` is called outside or inside a reactive context
- **THEN** the current map contents are returned without registering the current context as an observer

### Requirement: Fine-Grained Key-Level Reactive Observation
The `MapSignal<K, V>` primitive SHALL provide a `readable(K key)` method returning a lightweight `Readable<V>` that reads the value associated with `key`. Calling `get()` on this `Readable<V>` inside a reactive context SHALL register the caller as an observer exclusively for changes associated with `key`.

#### Scenario: Reading a specific key reactively
- **WHEN** an observer reads `mapSignal.readable(key).get()` inside a reactive context
- **THEN** the observer receives the current value for `key` (or null if absent) and is notified when `key`'s value changes

#### Scenario: Reading a missing key reactively
- **WHEN** an observer reads `mapSignal.readable(key).get()` for a key not present in the map
- **THEN** null is returned and the observer is registered so that when `key` is subsequently inserted, the observer is notified

### Requirement: Targeted Invalidation and Mutation Equality Suppression
Mutations to a `MapSignal<K, V>` SHALL only notify observers of modified keys and whole-map observers. Observers of unchanged keys SHALL NOT be notified. If a mutated key's new value equals the existing value according to `Objects.equals()`, the mutation SHALL be suppressed and no observers notified.

#### Scenario: Mutating a single key does not invalidate other keys
- **WHEN** `mapSignal.put(keyA, newValue)` is called and `newValue` is not equal to the old value
- **THEN** observers of `keyA` and observers of the whole map are invalidated, but observers of `keyB` are not notified

#### Scenario: Setting equal value suppresses notification
- **WHEN** `mapSignal.put(key, currentValue)` is called with a value equal to the existing value
- **THEN** no observers (neither key observers nor whole-map observers) are invalidated

### Requirement: Batch Mutations and Removal
The `MapSignal<K, V>` primitive SHALL support batch insertion via `putAll(Map<K, V>)`, key removal via `remove(K)`, and clearing via `clear()`. Batch mutations SHALL compute the exact set of changed keys and dispatch notifications only once per batch.

#### Scenario: Batch updating entries
- **WHEN** `mapSignal.putAll(newEntries)` is called
- **THEN** only keys whose values actually changed are invalidated, and whole-map observers are notified once

#### Scenario: Removing an existing key
- **WHEN** `mapSignal.remove(key)` is called for an existing key
- **THEN** the key is removed, observers of that key are invalidated with null, and whole-map observers are notified

#### Scenario: Clearing the map
- **WHEN** `mapSignal.clear()` is called on a non-empty map
- **THEN** all existing keys are invalidated and whole-map observers are notified

### Requirement: Lifecycle Independence
`MapSignal<K, V>` instances and their key readables (`readable(K key)`) SHALL NOT automatically register with ambient `ComponentContext` lifecycles during instantiation or retrieval. Transient UI component mount and unmount lifecycles SHALL NOT dispose or disconnect store-held `MapSignal` or key readables.

#### Scenario: Component unmount does not dispose MapSignal
- **WHEN** a UI component that reads `mapSignal.readable(key)` is unmounted and disposed
- **THEN** the `MapSignal` and its `readable(key)` continue to function and provide reactive updates to subsequent or existing subscribers
