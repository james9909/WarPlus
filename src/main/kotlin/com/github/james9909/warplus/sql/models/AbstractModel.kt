package com.github.james9909.warplus.sql.models

import java.sql.Connection

abstract class AbstractModel {
    abstract fun write(conn: Connection)
}
