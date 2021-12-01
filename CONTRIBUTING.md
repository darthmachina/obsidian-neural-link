# Contributing
I am definitely willing to accept help in developing this project (issues, pull requests, etc).

## Development
The Gradle task `browserWebpack` is the main task to create a build suitable for use in Obsidian.

### My Workflow
1. `./gradlew browserWebpack` will create the distribution files for use in Obsidian
2. symlink the `build/distributions` to the Obsidian plugins folder.
    - e.g. `ln -s ~/projects/obsidian-kotlin-plugin/build/distrubitions /vault_location/.obsidian/plugins/kotlin-plugin`
3. Safe Mode needs to be off in Obsidian
4. Refresh `Installed plugins` under `Community plugins`
5. Enable `Kotlin Sample Plugin`

When creating a new build, disable and re-enable the plugin under `Installed plugins` to pull in any changes.
