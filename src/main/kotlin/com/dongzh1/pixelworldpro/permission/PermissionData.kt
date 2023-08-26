package com.dongzh1.pixelworldpro.permission

import java.util.UUID

data class PermissionData(
    var name: String,
    var uuid: UUID,
    var group: String,
    var permission: List<String>
)