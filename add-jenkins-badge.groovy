#!/usr/bin/jenkins-cli groovy

/*
 * A simple script to be called using
 *
 *	jenkins-cli groovy add-jenkins-badge.groovy \
 *		"$JOB_NAME" "$BUILD_NUMBER" "<text>"
 *
 * to attach a little text to the given build (stealing Groovy Postbuild's
 * addShortText(String) functionality).
 */

removeBadge = false
if (this.args.length > 3 && "remove".equals(this.args[0])) {
	removeBadge = true
	this.args = this.args[1..-1].toArray()
}

if (this.args.length != 3) {
	throw new IllegalArgumentException("Usage: add-jenkins-badge <job> <build-number> <text>")
}

projectName = this.args[0]
buildNumber = Integer.parseInt(this.args[1])
version = this.args[2]

if (projectName != null && version != null) {
	map = jenkins.model.Jenkins.instance.getItemMap()
	project = map.get(projectName)
	build = project.getBuildByNumber(buildNumber)

	if (removeBadge) {
		actionsList = [ build.getActions() ]
		if (build instanceof hudson.matrix.MatrixBuild) {
			build.getRuns().each() {
				run -> actionsList.add(run.getActions())
			}
		}
		removeCount = 0
		actionsList.each() {
			actions ->
			for (i = actions.size() - 1; i >= 0; i--) {
				action = actions.get(i)
				if (action.getClass().getName().endsWith(".GroovyPostbuildAction") &&
						version.equals(action.getText())) {
					actions.remove(i)
					removeCount++
				}
			}
		}
		println("Removed " + removeCount + " badges")
		return
	}

	loader = jenkins.model.Jenkins.getInstance().getPluginManager().uberClassLoader
	clazz = loader.loadClass("org.jvnet.hudson.plugins.groovypostbuild.GroovyPostbuildAction")
	background = (version.matches("^[^A-Za-z]+$")?"#00FF00":version.matches("^.+?\\d$")?"#FFAA00":"#FFFF00");
	action = clazz.createShortText(version, "#000000", background,"1px", "#000000")

	build.getActions().add(action)

	if (build instanceof hudson.matrix.MatrixBuild) {
		build.getRuns().each() {
			run -> run.getActions().add(action)
		}
	}
}
