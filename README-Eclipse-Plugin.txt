Importing Expeditee into Eclipse
---------------------------------
New -> Other -> Java Project from Existing Ant Buildfile
Set the build file as Expeditees build.xml
Change the project name as required
Done

Convert to Eclipse Plugin
--------------------------
Plugins cannot have dependant projects that are not plugins themselves.  Therefore if you writing a eclipse plugin or RCP application that involes Expeditee you will need to convert Expeditee to a plugin project.

Right click on your Expeditee project in the package explorer.
Configure Menu -> Convert to Plug-in Projects..
Open up your MANIFEST.MF file in eclipse (Inside META-INF folder)
Goto the 'Runtime' tab.
In the 'Classpath' area remove all entries except the expeditee jar file
Open up your buildpath for the project.
Right Click project -> Build Path -> Configure build path
Remove all .jar entries from the libraries tab.
Go back to the class path area of the runtime tab in MANIFEST.MF
Select 'Add...' and proceed to add all the jars listed in releases\jars and releases\jars\ext

You should now be able to run your eclipse plugin/rcp that depends on Expeditee