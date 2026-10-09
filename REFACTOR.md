# REFACTOR.md

One section per milestone. Fill each one in as you go, in order.

Milestone 1 is written in two sittings, the pin before the refactor and the
rest after. A pin written afterwards is worth nothing, and a TA will ask.

Keep it short and specific. Point at methods, call sites, and test names.

---

## Milestone 1: Direct a refactor, characterization first

### The pin (write this section before you direct the refactor)

**The pin.**
`src/test/java/edu/cmu/cs214/scheduling/workflow/BookingWorkflowTest.java`:
`recurringSubmitSkipsASlotThatStartsWhenAnotherEnds` pins that
`BookingWorkflow.submit()` skips a recurring slot starting exactly when an
existing booking ends, while still booking the following conflict-free week.
The test checks the skipped slot, the booked slot and its original occurrence
index, stored bookings, and notification count. It records current behavior,
not a claim that the boundary rule is correct. No existing test method is edited
or deleted.

Validation before refactoring: `mvn -B test` against the unchanged shipped
implementation reports `Tests run: 36, Failures: 0, Errors: 0, Skipped: 0`.

**Why that one, and does a shipped test already cover it?**
The recurring branch uses inclusive comparisons (`<=`) for room conflicts,
whereas the regular branch uses strict comparisons (`<`). Consolidating those
checks during a refactor could silently change the recurring behavior. The
shipped `regularSubmitAcceptsASlotThatStartsWhenAnotherEnds` covers adjacency
only for regular bookings; `recurringSubmitBooksEveryWeekOfAnOpenSeries` covers
only a conflict-free series. Reading all four shipped test classes found no
assertion on recurring adjacency or the skipped-slot list. This pin adds that
boundary case with a second, available week, checking that the series is
partially accepted rather than wholly rejected.

**What a regeneration would do differently here.**
A regeneration from a short description would have to choose the overlap
boundary again. It would likely follow `TimeSlot`'s documented inclusive-start,
exclusive-end convention and reuse the regular booking overlap check, accepting
both weeks. That is a prediction, not a measured regeneration result; this test
would detect that change.

### The directive

**The refactor and the exact directive.** Extract a class per booking type,
using composition and a package-private `BookingHandler` interface. The user
selected option A after comparing independent handler classes with an enum
whose constants implement the behavior. Independent classes give the more
complex recurring rules their own file, at the cost of more files and an extra
call to follow. Both options use polymorphism.

The agent proposed the following structure and scope. All conversation
quotations below are English translations. After discussing alternatives,
the user authorized option A by replying "Choose A", after the pin commit
`b72f58e`.

Proposed structure:

> Consolidate each booking type's `submit/cancel/priceOf/describe` logic in its
> corresponding class. Keep the common checks and entry points in
> `BookingWorkflow`, with a single type mapping for dispatch. Keep the handler
> classes and interface package-private to avoid expanding the public API.

Proposed scope directive:

> Modify production code only within `workflow/`. Preserve existing public
> method signatures, validation order, return values, exceptions, storage
> changes, and notification behavior. Preserve the recurring booking boundary
> rule. Do not modify existing tests or the pin, or change domain, notify,
> pricing, or reporting. Run the full test suite and show the diff afterward.

The user's standing process instruction was:

> Proceed step by step. Stop for my review and judgment before committing.

The boundary stays within workflow/ because the repeated type branches are
there; storage, data types, notification rendering, and pricing rules do not
need to change. `REFACTOR.md` is the accompanying process record.

### The result

**Review status.** On October 8, 2026, after the refactor and verification
results were presented for review, the user approved the implementation and
asked to record the approval: "Review approved. Please record it."

**The diff and the suite.** The user approved the refactor and then authorized
a final re-check and commit. The refactor is recorded in commit `db4f683`,
after pin commit `b72f58e`. For the TA, use
`git diff b72f58e db4f683 -- src/main/java/edu/cmu/cs214/scheduling/workflow/` to show
the workflow change and all four added handler/interface files.
`mvn -B test` after extraction and `mvn -B clean test` during the final re-check
both report
`Tests run: 36, Failures: 0, Errors: 0, Skipped: 0` and `BUILD SUCCESS`.

**What did NOT change: behavior and files.** Agent verification presented during
review: the pin still skips the adjacent recurring slot, books the second week
with occurrence index 2, and keeps the expected stored bookings and notification
count. Comparing the 12 extracted operation bodies with the original branches
found the same Java tokens and string literals; only indentation changed.
The final re-check also confirmed unchanged public constructor/method signatures
and the same common entry-check sequences in all four operations.
This includes recurring cancellation from the selected occurrence onward and
pricing all uncancelled occurrences in a series. The common entry checks and
room-name fallback remain in `BookingWorkflow`; its map covers all three
current `BookingType` values. The recipient fallback was moved unchanged into
`BookingHandler`, and the maximum series length remains 26.

`git diff --exit-code b72f58e --` over `src/test`, the four production packages
`domain/`, `notify/`, `pricing/`, `reporting/`, and build/setup files returned no
changes. All four new Java files are within `workflow/`. Production changes
stayed within the directive; documentation changes are confined to this file.
Passing tests and branch comparison support preservation, but do not prove it
for every possible input.

**One thing the agent changed that you had to look at twice.**
I looked twice at the recurring submission loop after it moved into
`RecurringBookingHandler.submit()`. I checked that a conflicting week is skipped
without rejecting the whole series, and that later bookings retain their
original occurrence numbers through `week + 1`. If the first week conflicts,
the second week's booking must still have occurrence index 2. This detail was
easy to overlook during extraction, so I compared the original branch with the
new handler and checked the characterization test's assertions.

### The closing explanation

The user reviewed and approved the following explanation on October 8, 2026.

**Refactor or regenerate?** Refactoring is the better fit for this change:

- **Test coverage:** The 35 shipped tests cover the main operations, but did
  not pin recurring adjacency. The added pin brings the suite to 36, without
  making it a complete specification. Regeneration could still change untested
  behavior; extraction permits comparison with the existing branch bodies.
- **Code age:** The available history starts with one starter commit, followed
  by the pin and this refactor. There is no long history of accumulated fixes
  to preserve, which makes regeneration more plausible. That advantage alone
  does not resolve the missing behavioral specification.
- **Spec quality:** The README and method comments are not enough to rebuild
  every decision. For example, `TimeSlot` documents an exclusive end, while
  recurring conflict checks treat touching endpoints as a conflict. A rewrite
  from the high-level description could silently choose the regular rule.
- **Reach:** Workflow callers depend on the public operations and their
  results; notification tests check exact rendered text. `ReportService` reads
  the resulting store state, so changed writes or cancellation semantics could
  affect reports even though it does not call the workflow directly.

The existing separation of storage, pricing, notifications, and reporting is
usable. The specific problem is repeated type dispatch, which the three
handlers remove without replacing those boundaries. The cost is four extra
types and an additional call to follow. Regeneration would require reviewing
fresh implementations of the same rules with no clear benefit for this task.

**What would flip your answer.** Regenerating the workflow would become more
attractive if a new requirement invalidated the per-type organization, and a
precise specification plus public-behavior tests covered the decisions a
rewrite would otherwise guess: conflict boundaries, partial series acceptance,
cancellation range, series pricing, and storage/notification effects. With
those checks available and little historical repair knowledge to lose, replacing
the workflow could cost less to review than reorganizing most of its handlers.

---

## Milestone 2: The pattern critique

The user reviewed and accepted the recommended direct-append design below,
including its tradeoffs and compatibility limits. This is a design critique,
not an implemented change to `notify/` or its tests.

### The patterns present

| Pattern or role | Classes and methods |
| --- | --- |
| Strategy | `NotificationStrategy.render()` and `EmailNotificationStrategy`, held by `NotificationHub`. |
| Simple Factory | `NotifierFactory.createStrategy()`. It directly constructs the renderer; this is not the subclass-based GoF Factory Method structure. |
| Singleton | `NotifierFactory`'s private constructor, static instance, and synchronized `getInstance()`. |
| Observer | `NotificationHub.subscribe()/publish()`, `NotificationSubscriber`, and its `OutboxSubscriber` implementation. |
| Object Adapter | `OutboxSubscriber` wraps an `Outbox`, translating `onNotification(rendered)` into `append(rendered)`. |
| Facade-like entry point | `NotificationHub.publish()` hides formatting and delivery from workflow callers. This is the hub's role, not another separate layer. |

### The problem each one solves

| Pattern or role | Requirement that would justify it |
| --- | --- |
| Strategy | Several rendering algorithms must be selectable independently of the booking workflow. |
| Simple Factory | Callers need a central rule for selecting or constructing a renderer from configuration or user preferences. |
| Singleton | Exactly one instance must own a process-wide resource or coordinate shared state. |
| Observer | Several independently registered consumers must react to each notification without editing its publisher. |
| Object Adapter | A required subscriber interface must work with an existing sink whose interface cannot be changed. |
| Facade-like entry point | Booking operations should publish a message without knowing formatting and storage details. |

### Which of those problems exist here

- **Strategy:** No current choice of renderer. `EmailNotificationStrategy` is
  the only implementation, and `NotificationHub(Outbox)` obtains it from the
  factory rather than accepting a renderer. Workflow handlers always publish
  the same message type.
- **Factory:** No selection rule. `createStrategy()` always returns
  `new EmailNotificationStrategy()`, and the hub constructor is its only
  production caller.
- **Singleton:** No resource that needs unique ownership. The factory has no
  business state or external connection; its only state is the singleton
  reference. `factoryHandsBackTheSameInstance` checks identity, but does not
  establish a business need for it.
- **Observer:** Registration exists, but repository call sites register only
  `new OutboxSubscriber(outbox)` in the hub constructor. No other subscriber
  implementation or production `subscribe()` call was found. The ability to
  add listeners is real; a current need for multiple reactions is not shown.
- **Adapter:** It resolves a real interface mismatch introduced by the current
  Observer structure. Its entire callback is `outbox.append(rendered)`. If that
  subscriber boundary goes away, the mismatch goes away too.
- **Facade-like entry point:** This boundary is useful now. The three workflow
  handlers call `hub.publish(...)`; none needs to know the rendered string
  format or how the outbox stores it.

### The simpler structure

**Your proposal.** Keep a concrete `NotificationHub`, `NotificationMessage`,
and `Outbox`. The hub keeps its default and injected-outbox constructors, input
validation, and `getOutbox()`. Its one important method becomes:

```java
public void publish(NotificationMessage message) {
    outbox.append("To: " + message.recipient()
            + " | Subject: " + message.subject()
            + " | " + message.body());
}
```

This removes the renderer interface/implementation, factory/singleton, and
subscriber interface/adapter/list. It keeps formatting outside the workflow
and preserves the existing outbox's ordered storage and access methods.
`occurredAt` remains in the message but, as today, is not included in the text.

An alternative is to remove only the renderer and factory layers while keeping
Observer and its adapter. That preserves the public subscription facility but
retains the callback/list indirection for a single known sink. For the current
repository, the direct append is the smaller design. The alternative becomes
preferable if independent listeners are an actual supported requirement.

**What stays the same.** The proposal must preserve the exact
`To: ... | Subject: ... | ...` text and one outbox append per publication,
including confirmations sent through the workflow. These are checked by
`publishedMessageLandsInTheOutboxFullyRendered` and
`aConfirmationFromTheWorkflowReachesTheOutbox`; workflow tests also check
notification counts after booking, cancellation, rejection, and the pinned
partial series. The proposal continues to use the supplied outbox rather than
silently creating a different one.

There is a compatibility limit: `hubDeliversToItsOneSubscriber` explicitly
asserts `subscriberCount() == 1`, and `factoryHandsBackTheSameInstance` requires
the singleton accessor. Removing those APIs would break those tests; removing
`subscribe()` would also remove a public capability, even though repository
callers do not use it. This proposal therefore preserves notification outcomes,
not every existing structural/API contract. Preserving every current test in a
future implementation would require retaining compatibility APIs/layers. No
test is changed or deleted for this lab, and no claim is made that the proposed
replacement has been implemented or passes the unchanged suite.

**What you would keep, if anything.** No notification interface is necessary
for the current in-memory sink: injecting a concrete `Outbox` already makes
its contents observable. Keep the hub as the entry point. If a real external
delivery provider arrives, a narrow injectable delivery interface could isolate
network access for tests; the current renderer interface does not do that.

### What would bring each layer back

- **Strategy:** Different tenants require plain-text and HTML renderers,
  selected at construction and tested independently. Accept a rendering
  strategy in the hub instead of fixing it internally.
- **Factory:** Tenant configuration and user preferences determine which
  renderer is constructed, and several entry points must use the same choice
  rules. Centralize those rules in the factory.
- **Observer:** Each publication must reach both the outbox and an audit
  recorder, with consumers registered independently at startup.
- **Adapter:** Those observers include a third-party archive exposing only
  `append(String)`. Adapt that API to the subscriber callback.
- **Singleton:** A future in-process coordinator must uniquely own a shared
  resource. That would justify investigating singleton lifetime for the
  coordinator, not automatically for this stateless factory; constructing one
  owner at startup and injecting it may still avoid global access.

**Misuse or anti-pattern?** The main issue is pattern misuse: Strategy, Factory,
and Observer solve valid problems, but their variants, selection rules, and
multiple consumers are absent here. Together the layers form the lecture's
"pattern soup", an instance of speculative over-abstraction. Removing them
improves traceability from `publish()` to the outbox at the cost of ready-made
extension points. The critique is about their fit here, not a claim that the
patterns are inherently bad. Adapter supports an unnecessary boundary, while
the hub's facade-like separation still solves a current problem.

---

## Milestone 3: The missing pattern

Read `pricing/`. Not coded, one sentence.

**The pattern.** Which one fits `PriceCalculator`, and the problem that makes
it fit. Name the problem.

**Would you apply it today?** Yes or no, one line, with the reason.
