package view

import io.kvision.form.check.checkBox
import io.kvision.form.select.simpleSelectInput
import io.kvision.html.table
import io.kvision.html.td
import io.kvision.html.tr
import io.kvision.panel.SimplePanel
import model.Filter

class FilterDialogContent(val current: Filter) : SimplePanel(){
    init {
        table {
            tr { // by tag
                td {
                    checkBox(label = "By Tag") {
                        inline = true
                    }
                }
                td {
                    simpleSelectInput {

                    }
                }
            }
            tr { // by page
                td {
                    checkBox(label = "By Page") {
                        inline = true
                    }
                }
                td {
                    simpleSelectInput {

                    }
                }
            }
            tr { // by dataview
                td {
                    checkBox(label = "By Dataview") {
                        inline = true
                    }
                }
                td {
                    simpleSelectInput {

                    }
                }
            }
            tr { // hide future
                td {
                    checkBox(label = "Hide Future") {
                        inline = true
                    }
                }
            }
        }
    }
}