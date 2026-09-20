# query-view Specification

## Purpose
Provides declarative reactive component rendering for `Query<T>` in Solim UI, supporting loading, error, and data states with stale-while-revalidate (SWR) awareness, built-in defaults, and lifecycle management.

## Requirements

### Requirement: QueryView Component
`QueryView<T>` SHALL be a declarative reactive component in `solim-core` extending `BaseComponent` that binds to a `Query<T>` and dynamically renders the appropriate UI for each query lifecycle state: loading, error, and data.

#### Scenario: QueryView creation
- **WHEN** `QueryView.of(query)` or `UI.query(query)` is invoked
- **THEN** it SHALL return a `QueryView<T>` instance bound to the provided query

#### Scenario: Layout modifiers support
- **WHEN** cell or table modifiers (`growX()`, `minWidth()`, `padding()`, `align()`) are called on `QueryView`
- **THEN** they SHALL be applied to the underlying container table and cell

### Requirement: State Rendering and Transitions
`QueryView<T>` SHALL observe `query.loading()`, `query.error()`, `query.data()`, and `query.fetching()` to display the active state across all lifecycle transitions.

#### Scenario: Initial state
- **WHEN** `QueryView` is created before the query completes its initial fetch
- **THEN** it SHALL render the loading state

#### Scenario: Loading state
- **WHEN** `query.loading().get()` is true
- **THEN** `QueryView` SHALL render the component provided by `.loading(Supplier<Component>)`, or the default centered spinner if none is provided

#### Scenario: Success state
- **WHEN** `query.data().get()` is non-null and no error is present
- **THEN** `QueryView` SHALL render the component produced by `.data(Function<T, Component>)` or `.data(BiFunction<T, Boolean, Component>)`

#### Scenario: Error state
- **WHEN** `query.error().get()` is non-null and `query.data().get()` is null
- **THEN** `QueryView` SHALL render the component produced by `.error(Function<Throwable, Component>)`, or the default error view if none is provided

#### Scenario: Transition loading to success
- **WHEN** `query` successfully completes its initial load
- **THEN** `QueryView` SHALL unmount the loading component and mount the data component

#### Scenario: Transition loading to error
- **WHEN** `query` fails its initial load
- **THEN** `QueryView` SHALL unmount the loading component and mount the error component

#### Scenario: Transition success to refetch to fetching
- **WHEN** `query.refetch()` is called after a successful initial load
- **THEN** `QueryView` SHALL keep the data component visible on screen, and pass `isFetching = true` to `.data((data, isFetching) -> ...)`

#### Scenario: Transition success to refetch to success
- **WHEN** a background refetch completes successfully with updated data
- **THEN** `QueryView` SHALL update the data component with the new data and `isFetching = false`

#### Scenario: Transition success to refetch to error
- **WHEN** a background refetch fails while previous data is already displayed
- **THEN** `QueryView` SHALL retain the previous data component on screen without flashing to an error screen

#### Scenario: Transition error to retry to success
- **WHEN** an initial error occurs, and `query.refetch()` is triggered via the retry action
- **THEN** `QueryView` SHALL show loading or fetching, and on success, mount the data component

### Requirement: Data and Collection Reconciliation
`QueryView<T>` SHALL handle data updates, empty states, and collection reconciliation cleanly.

#### Scenario: Data changes
- **WHEN** the query data updates to a new value
- **THEN** `QueryView` SHALL re-render the data component with the updated value

#### Scenario: Empty data handling
- **WHEN** query data resolves to an empty collection or null
- **THEN** `QueryView` SHALL pass the empty value to `.data()` without throwing or breaking layout

#### Scenario: Keyed forEach reconciliation
- **WHEN** the data component contains a `forEach` or `reactiveGrid` with keyed items
- **THEN** structural keyed reconciliation SHALL preserve existing child elements for unchanged keys

#### Scenario: Duplicate keys in cache
- **WHEN** two `Query` instances use the same `QueryKey`
- **THEN** both `QueryView` instances SHALL observe the shared cache entry without conflicting or duplicating network calls

### Requirement: Concurrency and Edge Cases
`QueryView<T>` and `Query<T>` SHALL be resilient to asynchronous race conditions and callback failures.

#### Scenario: Stale or late requests
- **WHEN** a slow fetch completes after a faster refetch has already finished
- **THEN** the slow stale result SHALL be discarded via generation counter and NOT displayed by `QueryView`

#### Scenario: Callback exceptions
- **WHEN** a user-provided `.data()`, `.loading()`, or `.error()` callback throws an exception
- **THEN** the error SHALL be logged safely and SHALL NOT corrupt the reactive context or crash the game loop

#### Scenario: Query shared by multiple components
- **WHEN** two distinct `QueryView` components observe the same `Query` instance
- **THEN** both components SHALL update synchronously when the query state transitions

#### Scenario: Query outliving a component
- **WHEN** a `QueryView` component unmounts and disposes while the underlying `Query` or its cache entry remains active
- **THEN** subsequent fetches on the query SHALL NOT invoke unmounted component callbacks or leak memory

#### Scenario: No unnecessary rebuilding
- **WHEN** a signal update occurs that produces the same data value (`equals()`)
- **THEN** `QueryView` SHALL skip rebuilding the data subtree

### Requirement: Default UI and Auto-Wired Retry
`QueryView<T>` SHALL provide built-in defaults for loading and error states when custom handlers are omitted.

#### Scenario: Default loading UI
- **WHEN** `.loading(...)` is not specified on `QueryView` and the query is loading
- **THEN** it SHALL render a centered loading spinner

#### Scenario: Default error UI with retry
- **WHEN** `.error(...)` is not specified on `QueryView` and the query has an initial error
- **THEN** it SHALL render scarlet error text and a "Retry" button that invokes `query.refetch()` when clicked

### Requirement: Disposal and Cleanup
`QueryView<T>` SHALL isolate child component builds and properly dispose unmounted children and bindings when transitioning between states or when the `QueryView` itself is disposed.

#### Scenario: Child disposal on state transition
- **WHEN** `QueryView` transitions from loading to data
- **THEN** the loading component and its owned bindings SHALL be disposed before mounting the data component

#### Scenario: QueryView disposal
- **WHEN** the parent component or `QueryView` is disposed
- **THEN** all active child components and bindings SHALL be disposed, and no further query updates SHALL be processed
