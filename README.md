# obsidian-neural-link
Collection of tools to automate Obsidian workflows.

Why *Neural Link*? Because this connects my brain to Obsidian in a better way.

## Usage
This plugin is mainly meant for personal use, at least for now. There are things that I would like to automate in terms of my personal workflow and how I work with Obsidian and other plugins. So I'm using my programming background to create this. It's the first Obsidian plugin I've developed, so I may eventually release it publicly or spin off some features into their own plugins. But for now this is just really meant for my use.

### Why a public repository?
I don't want to use my development versions in my regular vault, and having to manually copy over new versions would be a PITA. So I will be regularly releasing new versions which I can then use [BRAT](https://github.com/TfTHacker/obsidian42-brat) to manage. And, if other people discover it and find some benefit out of it, that works for me.

### What's included?

#### Remove tags matching a RegEx from a completed task
I use [CardBoard](https://github.com/roovo/obsidian-card-board) for managing my tasks, but I create a LOT of tasks. This can lead to performance problems as the tasks that CardBaord has to process constantly increases. I use this to remove the tags related to the status columns when the task is completed, to keep CardBoard snappy.

This uses a RegEx to define which tags to remove, and it is based on the actual tag text. This lets me define a full tag hierarchy to remove (like everything under `#kanban/`).

#### Repeating Tasks
Tasks can include some metadata that defines a repeating task. This will watch for completed tasks that include the metadata and create a new task that is a copy with all indented list items included, marking any subtasks as incomplete to get a fresh task.

## Contributing
Please see the [contributing doc](CONTRIBUTING.md) for more information on helping out if you would like to.
