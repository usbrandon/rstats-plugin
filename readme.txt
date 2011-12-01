Eclipse Build:

1. Run Ant resolve task. 
2. Reconcile classpath in project properties window. Remov any jars not found and add any additional jars in the lib 
and test-lib paths. (Ivy.xml tracks the real classpath, the resolve lays down the jars, and the Eclipse classpath gets tweaked 
based on ivy.xml changes. You also can use IvyDE, if you prefer.
3. Rebuild.

To Install (requires a Pentaho BI Server to be installed first!):

1. In the build.properties file, enter the path to your BI server as the value for the pentaho.dir build parameter. 
2. Run Ant tasks clean-all, resolve, dist, install.
3. Re-start your server. The plugin creates a default action sequence and jasper content under the solutions/bi-developers folder. 

The state of the plugin, and a comparison of this plugin with the legacy JasperReportsComponent exists in the header comments 
of the JasperAction class. 