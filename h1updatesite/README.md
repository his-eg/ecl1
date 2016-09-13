#Release Notes#

* Release Notes starting with version 0.1.0.2014-08-11
* Missing version numbers between 0.4.7 and 0.4.20 inclusive indicate intermediate testing releases

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
