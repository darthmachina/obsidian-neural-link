if (config.loggers !== undefined) {
    config.resolve = {
        alias: {
            obsidian: '../../../../../test-resources/obsidian.js',
        }
    };
} else {
    config.externals = {
        obsidian: "obsidian"
    }
}
