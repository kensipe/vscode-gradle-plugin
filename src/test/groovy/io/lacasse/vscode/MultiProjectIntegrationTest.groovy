package io.lacasse.vscode

import io.lacasse.integtest.FunctionalTest
import io.lacasse.integtest.TestFile
import io.lacasse.vscode.fixtures.VisualStudioCodeTaskNames
import io.lacasse.vscode.fixtures.VisualStudioCodeWorkspaceFixture
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class MultiProjectIntegrationTest extends Specification implements FunctionalTest, VisualStudioCodeTaskNames {
    @Rule TemporaryFolder testProjectDir = new TemporaryFolder()
    
    @Override
    File getRootDir() {
        return testProjectDir.root.canonicalFile
    }

    @Override
    File createBuildFile() {
        return testProjectDir.newFile("build.gradle")
    }

    @Override
    File createSettingsFile() {
        return testProjectDir.newFile("settings.gradle")
    }

    def setup() {
        buildFile << """
            plugins {
                id "io.lacasse.vscode"
            }
        """
        settingsFile << """
            rootProject.name = "root"
        """
    }

    def "can generate vscode project and workspace for multiple projects"() {
        settingsFile << """
            include "project1", "project2", "project3"
        """
        buildFile << """
            allprojects {
                apply plugin: "io.lacasse.vscode"
            }
        """
        
        when:
        succeed "vscode"
        
        then:
        assertTasksExecuted([vscodeTasks(), vscodeTasks(":project1"), vscodeTasks(":project2"), vscodeTasks(":project3")].flatten())
        assertTasksNotSkipped([vscodeTasks(), vscodeTasks(":project1"), vscodeTasks(":project2"), vscodeTasks(":project3")].flatten())

        def workspace = vscodeWorkspace()
        workspace.location.assertIsFile()
        workspace.content.folders*.path as Set == [rootDir, file("project1"), file("project2"), file("project3")]*.absolutePath as Set
    }

    VisualStudioCodeWorkspaceFixture vscodeWorkspace(TestFile workspaceFile = file("root.code-workspace")) {
        new VisualStudioCodeWorkspaceFixture(workspaceFile)
    }
}