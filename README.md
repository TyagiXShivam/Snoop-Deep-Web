[![storm-crawler](http://stormcrawler.net/img/Logo-small.jpg)](http://stormcrawler.net/)
=============

[![license](http://www.apache.org/licenses/LICENSE-2.0)
![Build Status]


StormCrawler is an open source collection of resources for building low-latency, scalable web crawlers on [Apache Storm](http://storm.apache.org/). It is provided under [Apache License](http://www.apache.org/licenses/LICENSE-2.0) and is written mostly in Java.

## Quickstart

NOTE: These instructions assume that you have [Apache Maven](https://maven.apache.org/install.html) installed. You will need to install [Apache Storm](http://storm.apache.org/) to run the crawler.

StormCrawler requires Java 11 or above.

The version of Storm to use must match the one defined in the pom.xml file of your topology. The major version of StormCrawler mirrors the one from Apache Storm, i.e whereas StormCrawler 1.x used Storm 1.2.3, the current version now requires Storm 2.4.0.

Once Storm is installed, the easiest way to get started is to generate a brand new StormCrawler project using \: 

`mvn archetype:generate -DarchetypeGroupId=com.deepweb.stormcrawler -DarchetypeArtifactId=storm-crawler-archetype -DarchetypeVersion=2.7`

You'll be asked to enter a groupId (e.g. com.mycompany.crawler), an artefactId (e.g. stormcrawler), a version and package name.

This will not only create a fully formed project containing a POM with the dependency above but also the default resource files, a default CrawlTopology class and a configuration file. Enter the directory you just created (should be the same as the artefactId you specified earlier) and follow the instructions on the README file.


.

## Thanks

![alt tag](https://www.yourkit.com/images/yklogo.png)

YourKit supports open source projects with its full-featured Java Profiler.
YourKit, LLC is the creator of <a href="https://www.yourkit.com/java/profiler/index.jsp">YourKit Java Profiler</a>
and <a href="https://www.yourkit.com/.net/profiler/index.jsp">YourKit .NET Profiler</a>,
innovative and intelligent tools for profiling Java and .NET applications.
.
