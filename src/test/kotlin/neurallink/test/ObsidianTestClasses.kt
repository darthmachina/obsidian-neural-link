package neurallink.test

import ListItemCache
import Loc
import Pos

class TestLoc(
    override var line: Number,
    override var col: Number,
    override var offset: Number
) : Loc {
    companion object {
        val EMPTY = TestLoc(0, 0, 0)
    }
}

class TestPos(
    override var start: Loc,
    override var end: Loc
) : Pos {
    companion object {
        val EMPTY = TestPos(TestLoc.EMPTY, TestLoc.EMPTY)
    }
}

class TestListItemCache(
    override var parent: Number,
    override var position: Pos,
    override var task: String? = null
) : ListItemCache {
    companion object {
        val EMPTY = TestListItemCache(-1, TestPos.EMPTY)
    }
}