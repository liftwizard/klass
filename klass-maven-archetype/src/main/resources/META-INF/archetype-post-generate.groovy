/*
 * Copyright 2025 Craig Motlin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Post-generation script to strip .vm extension from generated files.
 *
 * Template files in the archetype use .vm extension to prevent tools like
 * OpenRewrite and IDE formatters from modifying them. This script removes
 * the .vm extension after generation so the output files have correct names.
 */

def projectDir = new File(request.outputDirectory, request.artifactId)

def stripVmExtension(File dir) {
    dir.eachFileRecurse { file ->
        if (file.isFile() && file.name.endsWith('.vm')) {
            def newName = file.name[0..-4]  // Remove last 3 characters (.vm)
            def newFile = new File(file.parentFile, newName)
            assert file.renameTo(newFile) : "Failed to rename ${file} to ${newFile}"
        }
    }
}

stripVmExtension(projectDir)
