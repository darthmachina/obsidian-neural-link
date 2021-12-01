import events.RemoveRegexFromTask
import events.TaskProcessor
import kotlinx.coroutines.await

@OptIn(ExperimentalJsExport::class)
@JsExport
@JsName("default")
class NeuralLinkPlugin(override var app: App, override var manifest: PluginManifest) : Plugin(app, manifest) {
    var settings : NeuralLinkPluginSettings = NeuralLinkPluginSettings.default()

    val taskProcessors : List<TaskProcessor>

    init {
        taskProcessors = mutableListOf(RemoveRegexFromTask(this))
    }

    override fun onload() {
        loadSettings()

        this.registerEvent(this.app.metadataCache.on("changed") {
            file: TFile -> handleFileModified(file)
        })

        // Add Settings tab
        addSettingTab(NeuralLinkPluginSettingsTab(app, this))
        console.log("KotlinPlugin onload()")
    }

    override fun onunload() {
        console.log("KotlinPlugin onunload()")
    }

    private fun handleFileModified(file: TFile) {
        var modified = false;
        val fileContents = app.vault.read(file).then { it.split('\n') }
        val fileListItems = app.metadataCache.getFileCache(file)?.listItems ?: arrayOf()
        fileListItems.forEach { listItem ->
            if (listItem.task?.toUpperCase() == "X") {
                val lineContents = fileContents
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
//				const newLine = lineContents.replace(new RegExp(this.settings.textToRemove, 'g'), '');
//				console.log(`  new task line: ${newLine}`);
//				if (item.task === 'x' || item.task === 'X') {
//					if (newLine !== lineContents) {
//						console.log('Replacing task line with new one without text');
//						fileContents[index] = newLine;
//						modified = true;
//					}
//				}
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
//			if (modified) {
//				this.app.vault.modify(file, fileContents.join('\n'));
//			}
    }

    private fun loadSettings() {
        // TODO: implement exmaple of versioned settings
        loadData().then {result ->
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
