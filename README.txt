SpringWebServicesRestApiDoclet 
Licensed under The MIT License (MIT) - http://opensource.org/licenses/MIT
Copyright (c) 2014 RightShift, Cape Town
www.rightshift.biz

About:
======

SpringWebServicesRestApiDoclet is a simple custom doclet for generating documentation of Spring web service API's.
It produce a javadoc describing the REST end-points which includes path, path parameters, URL/POST parameters and request
body.

License:
========

All files are licensed under The MIT License (MIT)
Please see license.txt

Usage:
======

To build:
$ mvn clean package

Once built you can formulate a fairly detailed javadoc command as such:

$javadoc -classpath "./target/lib/*" -docletpath "./target/restApiDoclet-1.0.0.jar:./target/lib/velocity-1.7.jar:./target/lib/commons-lang-2.6.jar:./target/lib/jackson-annotations-2.3.0.jar:./target/lib/jackson-core-2.3.0.jar:./target/lib/jackson-databind-2.3.0.jar:./target/lib/commons-collections-3.2.1.jar" -doclet biz.rightshift.commons.doclet.SpringWebServicesRestApiDoclet YourControllerClass.java

In our case above, we have specified none of the custom options. This will generate documentation for YourControllerClass.java

You may specify any of the custom parameters that we have implemented:
-heading ''  : To customise the html title
-output ''   : Specify the output file name. The default is index.html.
-template '' : Specify a custom template to use. The default is a trivial template, included (and named rest_api_template.vm)
-types ''    : List your own packages to describe. Acts as a filter. If omitted, an attempt is made to describe all packages.

Original Contributors
====================

Barry Jordan <barryj@rightshift.biz>
Marlon van der Linde <marlon@rightshift.biz>

