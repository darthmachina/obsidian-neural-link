//const isTest = typeof config.plugins.includes('kotlin-test-js-runner/karma-kotlin-reporter.js')
//if (isTest) {
//    config.resolve = {
//        alias: {
//            obsidian: '../../../../../test-resources/obsidian.js',
//        }
//    };
//} else {
    config.externals = {
        obsidian: "obsidian"
    }
//}
