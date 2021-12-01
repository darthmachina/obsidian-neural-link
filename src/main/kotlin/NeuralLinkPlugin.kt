import events.FileModifiedEvent

@OptIn(ExperimentalJsExport::class)
@JsExport
@JsName("default")
class NeuralLinkPlugin(override var app: App, override var manifest: PluginManifest) : Plugin(app, manifest) {
    var settings = NeuralLinkPluginSettings.default()
        // Can't call loadSettings() until the plugin is loaded (not constructed) so can't use val
        // here. Make the setter private so the value can only be changed within this class.
        private set

    private val fileModifiedEvent = FileModifiedEvent(this)

    override fun onload() {
        loadSettings()

        this.registerEvent(this.app.metadataCache.on("changed") {
            file -> fileModifiedEvent.processEvent(file)
        })

        // Add Settings tab
        addSettingTab(NeuralLinkPluginSettingsTab(app, this))
        console.log("NeuralLinkPlugin onload()")
    }

    override fun onunload() {
        console.log("NeuralLinkPlugin onunload()")
    }

//			let modified = false;
//			const fileContents = await (await this.app.vault.read(file)).split('\n');
//			const fileListItems  = await this.app.metadataCache.getFileCache(file).listItems;
//			console.log('------------------------')
//			fileListItems.forEach((item, index) => {
//				const lineContents = fileContents[item.position.start.line];
//				console.log(`listItem data: [position: ${item.position.start.line}/${item.position.end.line}, task: ${item.task}, parent: ${item.parent}]`);
//				console.log(`  text for listItem: ${lineContents}`)
//
//				// Check for recurrence
//				if (lineContents.contains('[repeat::')) {
//					const repeatRegex = /\[repeat::[\s]*([\w\s]+)(!)?\]/;
//					const dueRegex = /\[due::[\s]*([\w\s:-]+)\]/;
//					const dueFormat = 'YYYY-MM-DD HH:mm:SS';
//
//					const repeatString = new RegExp(repeatRegex, 'g').exec(lineContents);
//					const dueString = new RegExp(dueRegex, 'g').exec(lineContents);
//					const fromCompletionDate = repeatString[2] === '!';
//					console.log(`  repeatString: ${repeatString[1]}, dueString: ${dueString[1]}, from: ${repeatString[2]}`);
//
//					const nldatesPlugin = this.app.plugins.getPlugin("nldates-obsidian");
//					const parsedResult = nldatesPlugin.parseDate(repeatString[1]);
//					console.log(`  parsedResult: ${parsedResult.date}`);
//					if (fromCompletionDate) {
//						console.log(`  dateFromCompletion: ${parsedResult.moment.from('2021-11-20')}`);
//					}
//				}
//			});
//

    private fun loadSettings() {
        // TODO: implement exmaple of versioned settings
        loadData().then {result ->
            if (result != null) {
                // TODO ClassCastException here if there are no settings available? Maybe "result as String"?
                val loadedSettings = NeuralLinkPluginSettings.fromJson(result as String)
                console.log("loadedSettings: ", loadedSettings)
                // TODO Replace with a version check
                // Right now if fromJson fails the default settings will be used
                if (loadedSettings.taskRemoveRegex != "") {
                    console.log("Saving loaded settings")
                    settings = loadedSettings
                }
            }
        }
    }
}
