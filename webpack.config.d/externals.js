// const isTest = process.env.NODE_ENV === 'test';
//
// if (isTest) {
//     console.log("isTest is true")
//     config.resolve = {
//         alias: {
//             obsidian: '../../../../../test-resources/obsidian.js'
//         }
//     };
// } else {
//     console.log(`isTest is false: ${process.env.NODE_ENV}`)
//     config.externals = {
//         obsidian: 'obsidian',
//     };
// }

// TODO: Set var in Karma config and check here for whether it's the normal externals or the test one
config.externals = {
    obsidian: '../../../../../test-resources/obsidian.js'
};
