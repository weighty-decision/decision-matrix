## Coding standards
### Testing
- Use kotest matchers for assertions
- Use junit for tests
- don't use mockk; use interfaces as dependencies, with NotImplementedError as the default implementation in the interface