# TODO Items
- [ ] Pass in indented items to each `TaskProcessor`, not just the main task
- [ ] Replace `JSON.parse()` with `kotlinx.serialization` 
  - https://github.com/Kotlin/kotlinx.serialization
  - For versioning can follow https://stackoverflow.com/questions/63826230/how-to-partially-decode-a-json-string-using-kotlinx-serialization to load just the version. Can then use a factory method to load that version into an object and merge it with the defaults to create an object at the latest version