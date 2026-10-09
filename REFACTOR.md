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
a final re-check and commit. The refactor is recorded in the commit containing
this section, after pin commit `b72f58e`. For the TA, use
`git diff b72f58e -- src/main/java/edu/cmu/cs214/scheduling/workflow/` to show
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

**One thing the agent changed that you had to look at twice.** Something you
checked line by line before accepting. If there was nothing, say how carefully
you read the diff.

The user approved the implementation. The specific point revisited, or depth
of review if none required a second look, still needs the user's description.
The approval alone does not supply that detail.

### The closing explanation

**Refactor or regenerate?** Argue whether regenerating `BookingWorkflow` from scratch
would have been the better call, using the lecture's four questions (test
coverage, code age, spec quality, and reach). Be concrete about this codebase.

**What would flip your answer.** A condition about the artifact, not a feeling.

---

## Milestone 2: The pattern critique

Read `notify/`. It works and the outbox tests pass.

### The patterns present

List every design pattern you can name in that package. For each one, the class
or classes that carry it.

### The problem each one solves

For each pattern you listed, what would have to be true about the requirements
for that pattern to be the right call? One sentence each, not in terms of
"flexibility".

### Which of those problems exist here

For each pattern, does the problem it solves exist in this codebase? Point at
the code that settles it.

### The simpler structure

**Your proposal.** What replaces `notify/`. Sketch the classes and the one
method that matters.

**What stays the same.** The tested behavior it must still produce, named
precisely enough that a reader can check it against the shipped tests.

**What you would keep, if anything.** If you would keep one interface, say
which and why. "None of it" is a fine answer if you can defend it.

### What would bring each layer back

For at least two of the layers you would remove, what requirement, if it
arrived next sprint, would make that layer the right structure? Be specific
about the requirement, not about the pattern.

**Misuse or anti-pattern?** Say which this is and why the distinction matters.

---

## Milestone 3: The missing pattern

Read `pricing/`. Not coded, one sentence.

**The pattern.** Which one fits `PriceCalculator`, and the problem that makes
it fit. Name the problem.

**Would you apply it today?** Yes or no, one line, with the reason.
