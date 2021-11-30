#Release Notes#

* Release Notes starting with version 0.1.0.2014-08-11
* Missing version numbers between 0.4.7 and 0.4.20 inclusive indicate intermediate testing releases

##Version 1.3.0.2021-11-30##
* webapps is now on top of the classpath
* The extension importer does not offer projects anymore that are no HisInOne extension projects
* Some bugfixes in git lfs autoprune

##Version 1.2.0.2021-07-12##
* Make git hooks work under Linux
* Improved resource loading in extension projects
* git auto prune now works as well when stashes are present
* Extension importer usability and performance improvements

##Version 1.1.0.2021-05-18##
* git-batch-pull button: Projects are now pulled in a specific order to speed up the build process.
* ecl1 now handles linked work trees gracefully.

##Version 1.0.1.2021-03-25##
* Fixed standalone applications like EntityEnhancer and DtoGenerator running on extensions.
* Fixed resource leak in gitlab http access.

##Version 1.0.0.2021-03-12##
* Fixed runtime classpath for Junit-Tests with Java 11.
* Run 'git lfs prune' at Eclipse startup to remove old LFS files from local storage. (needs external git client)
* Removed dependency on CVS client.

##Version 0.10.0.2020-11-05##
* Added commit exporter for webapps in git repository.
* Do not offer AUTOCOMMIT_EXTENSION_JAR for extension import.
* Refactored ecl1 update check.

##Version 0.9.0.2019-05-20##
* Fixed infinite loop at extension import when there are cyclic dependencies between extensions.
* Fixed exceptions in new enroll checker wizard when no or a wrong project has been selected.
* Added Activator classes to all plugins, which allowed to improve logs.

##Version 0.8.9.2019-04-01##
* Add classes.instr folder to the runtime classpath of JUnit tests even if it is not in the compile classpath.

##Version 0.8.8.2019-03-20##
* Add webapps project to the runtime classpath for JUnit tests only if it is a dependency of the project containing the JUnit test.
* Do not add projects twice to the runtime classpath for JUnit tests.

##Version 0.8.7.2019-03-18##
* Runtime classpath for JUnit tests: Full recurrent iteration through projects, classpath containers and classpath variables.

##Version 0.8.6.2019-03-12##
* Fixed another bug in the runtime classpath for JUnit tests.

##Version 0.8.5.2019-03-06##
* Fixed order of runtime classpath entries for JUnit tests.

##Version 0.8.4.2019-03-04##
* Now ecl1 constructs the runtime classpath for JUnit launch configurations. This fixes JUnit tests that need HisInOne extensions under Java 11.

##Version 0.8.3.2018-11-19##
* Fixed false error message "The selected ChangeSet contains no files!" when change set title and comment were different

##Version 0.8.2.2018-07-09##
* Adapted modification in gitlab URL scheme.
* Catch and log exception when build server is not accessible.

##Version 0.8.1.2018-06-13##
* Fixed deadlock while logging occurring on some systems

##Version 0.8.0.2018-06-04##
* Prepared ExtensionImporter for transition from git to gitlab
* Improved ChangeSetExporter input validation
* ChangeSetExporter adds new dbUpdate-attribute to hotfix snippet
* Complete overhaul of ecl1 logging; one can now choose a log level in the preference page

##Version 0.7.4.2017-04-19##
* fixed NPE when there is no webapps project in the workspace
* fixed NPE when adding a project nature not known in the workspace (e.g. Macker nature without Macker plugin installed)

##Version 0.7.3.2017-03-09##
* new project template URLs are now a list and configurable via preferences; the first choice is devtools.his.de (much faster than SF)

##Version 0.7.2.2017-03-06##
* cleaned up build, removed old artifacts
* now all artifacts are signed
* better key file names

##Version 0.7.1.2017-03-02##
* ecl1 is now built with Java 8
* Compiler compliance level for new H1 extension projects is Java 8
* Added Macker nature to new H1 extension projects
* Build scripts in new H1 extension projects support Angular2

##Version 0.7.0.2016-09-26##
* Feature: Import dependencies of imported projects
* Feature: Improved calculation of hotfix numbers for change set exporter
* Some minor code improvements

##Version 0.6.6.2016-09-13##
* Bugfix: Update Site was broken

##Version 0.6.5.2016-04-19##
* Bugfix: Broken dependencies of import wizard fixed

##Version 0.6.4.2016-04-12##
* Feature: Signed features and plugins

##Version 0.6.3.2016-04-11##
* Bugfix: TemplateManager computed the download URL for templates wrong

##Version 0.6.2.2016-03-01##
* Bugfix: Projects containing a non-empty file named config lead to an error

##Version 0.6.1.2016-02-29##
* Feature: Increase logging of batch pull
* Bugfix: Catch previously uncaught exceptions in batch pull

##Version 0.6.0.2016-02-29##
* Feature: Add a batch pull button performing a pull on all git repos in workspace having a remote

##Version 0.5.1.2016-02-26##
* Bugfix: Due to Sourceforge switching to HTTPS template downloads need to handle HTTP redirects

##Version 0.5.0.2016-02-24##
* Feature: Make base URL for templates configurable

##Version 0.4.21.2015-12-01##
* Bugfix: Create missing parent folders for templates

##Version 0.4.20.2015-11-04##
* Bugfix: Auto-update checked full eclipse installation, now only checks ecl1 for updates

##Version 0.4.17.2015-11-02##
* Bugfix: Update check on startup of eclipse
* Versions in between were test releases

##Version 0.4.7.2015-10-30##
* Feature: Update check on startup of eclipse

##Version 0.4.6.2015-10-28##
* Feature: New export wizard to create HIS hotfix definition snippets

##Version 0.4.5.2015-08-26##
* Bugfix: Consistent ordering of contributors for extension.ant.properties https://sourceforge.net/p/ecl1/tickets/17/

##Version 0.4.2.2015-07-09##
* Improvement: Use table component for classpath container configuration

##Version 0.4.0.2015-07-09##
* Improvement: Extension jars embedded via container found in code completion

##Version 0.3.2.2015-06-10##
* Improvement: Validation of URLs in preferences

##Version 0.3.0.2015-06-09##
* Improvement: Determine templates for new extensions via sf.net

##Version 0.2.7.2015-06-05##
* Improvement: Use table view for import assistant

##Version 0.2.5.2015-06-03##

* Feature: Add import wizard for HISinOne-Extensions

##Version 0.1.1.2014-08-13##

* Bugfix: Remove empty "Package Layout" group from new extension wizard

##Version 0.1.1.2014-08-13##

* Bugfix: Extensions only requiring core do not get additional build infrastructure
* Bugfix: Added dummy files to two yet unconsidered source folders in generated project

##Version 0.1.0.2014-08-11##

* Added classpath container for extensions
* Template files are loaded from sourceforge file releases
