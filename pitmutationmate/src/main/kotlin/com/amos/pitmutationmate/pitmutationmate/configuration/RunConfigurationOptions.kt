// SPDX-License-Identifier: MIT
// SPDX-FileCopyrightText: 2023 Lennart Heimbs <lennart@heimbs.me>, Brianne Oberson

package com.amos.pitmutationmate.pitmutationmate.configuration

import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.openapi.components.StoredProperty

class RunConfigurationOptions : RunConfigurationOptions() {
    private var taskNameOption: StoredProperty<String?> = string("").provideDelegate(this, "taskName")
    var taskName: String?
        get() = taskNameOption.getValue(this)
        set(value) {
            taskNameOption.setValue(this, value)
        }

    private var classFQNOption: StoredProperty<String?> = string("").provideDelegate(this, "classFQN")
    var classFQN: String?
        get() = classFQNOption.getValue(this)
        set(value) {
            classFQNOption.setValue(this, value)
        }

    private val gradleExecutableOption: StoredProperty<String?> = string("").provideDelegate(this, "gradleExecutable")
    var gradleExecutable: String?
        get() = gradleExecutableOption.getValue(this)
        set(value) {
            gradleExecutableOption.setValue(this, value)
        }
}
