# Testing

Main testing goals of this project:

- When we want to support a new release of Bitwig, we can run a test suite to confirm that the
  bitwig internals still behave as we expect them to. If not, then we learn to what extent.

## Testing Plan:

For each supported version of Bitwig:

- Attempt to port our code to the new version of bitwig.jar
- Run tests against the new JAR
  - Does the Device object still behave as expected?
  - Do GUI elements still behave as expected?
  - Do MouseButtonPressedEvents still behave as expected?
  - ... and so on.
  - Have any of the methods/classes that we've hooked changed in their implementation?
    - What about in the context in which they're used?
      - Would it be possible to identify all references to our method, and test if those references
        have changed implementation?
        - How do we connect an event processing routine, to the routine that fired the event? If we
          can't then we may not know if an event has changed in the number of times it's called.
- Load up our new agent in Bitwig.
  - Run tests for each of our features:
    - Does the dock toggle still work?
    - Does the plugin window tracking still work?
      - Are the windows the right dimensions?
      - Do they occlude properly?
      - ...
  - Run tests for each of our regression scenarios:
    - Will our program boot after dealing with JAR deobfuscation?
    - Will it reintroduce X bug?