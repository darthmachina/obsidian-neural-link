import event.FileModifiedEvent
import service.SettingsService

@OptIn(ExperimentalJsExport::class)
@JsExport
@JsName("default")
class NeuralLinkPlugin(override var app: App, override var manifest: PluginManifest) : Plugin(app, manifest) {
    private val state = NeuralLinkState(NeuralLinkPluginSettings.default())

    // Dependent classes are constructed here and passed into the classes that need them. Poor man's DI.
    // SERVICES
    private val settingsService = SettingsService(state)

    // EVENTS
    private val fileModifiedEvent = FileModifiedEvent(this, state)

    override fun onload() {
        loadSettings()

        this.registerEvent(this.app.metadataCache.on("changed") {
            file -> fileModifiedEvent.processEvent(file)
        })

        // Add Settings tab
        addSettingTab(NeuralLinkPluginSettingsTab(app, this, settingsService, state))
        console.log("NeuralLinkPlugin onload()")
    }

    override fun onunload() {
        console.log("NeuralLinkPlugin onunload()")
    }

    private fun loadSettings() {
        loadData().then {result ->
            settingsService.loadFromJson(result)
        }
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
}
