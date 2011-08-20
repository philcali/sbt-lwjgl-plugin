import sbt._

import xml.Elem

object Helpers {

  implicit def xmlContents(xml: Elem) = 
    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + xml.toString

  def ivyParent(org: String, module: String, revision: String, children: Seq[String], pub: String): String = {
<ivy-module version="2.0" xmlns:e="http://ant.apache.org/ivy/extra">
  <info organisation={org} module={module} revision={revision} status="release" publication={pub}/>
  <configurations>
    <conf name="compile" visibility="public" description=""/>
    <conf name="runtime" visibility="public" description=""/>
    <conf name="provided" visibility="public" description=""/>
    <conf name="system" visibility="public" description=""/>
    <conf name="optional" visibility="public" description=""/>
    <conf name="sources" visibility="public" description=""/>
    <conf name="javadoc" visibility="public" description=""/>
  </configurations>
  <publications>
    <artifact name={module} type="pom" ext="pom" conf="compile,runtime,provided,system,optional,sources,javadoc"/>
  </publications>
  <dependencies>
    { children.map { child =>
      <dependency org={org} name={child} rev={revision} conf={scala.xml.Unparsed("compile->default(compile)")}>
      </dependency>
    }}
  </dependencies>
</ivy-module>
  }

  def pomMe(org: String, artifact: String, revision: String, children: Seq[String]): String = {
<project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd" 
         xmlns="http://maven.apache.org/POM/4.0.0" 
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <modelVersion>4.0.0</modelVersion>
    <groupId>{org}</groupId>
    <artifactId>{artifact}</artifactId>
    <packaging>pom</packaging>
    <version>{revision}</version>
    <dependencies>
        { children.map { child =>
          <dependency>
            <groupId>{org}</groupId>
            <artifactId>{child}</artifactId>
            <revision>{revision}</revision>
            <scope>compile</scope>
          </dependency>
        }}
    </dependencies>
    <repositories>
        <repository>
            <id>ScalaToolsMaven2Repository</id>
            <name>Scala-Tools Maven2 Repository</name>
            <url>http://scala-tools.org/repo-releases/</url>
        </repository>
    </repositories>
</project>
  }

  def ivyMe(org: String, module: String, revision: String, artifact: String, pub: String): String = {
<ivy-module version="1.0" xmlns:e="http://ant.apache.org/ivy/extra">
  <info organisation={org} module={module} revision={revision} status="release" publication={pub}/>
  <configurations>
    <conf name="compile" visibility="public" description=""/>
    <conf name="runtime" visibility="public" description=""/>
    <conf name="provided" visibility="public" description=""/>
    <conf name="system" visibility="public" description=""/>
    <conf name="optional" visibility="public" description=""/>
    <conf name="sources" visibility="public" description=""/>
    <conf name="javadoc" visibility="public" description=""/>
  </configurations>
  <publications>
    <artifact name={artifact} type="jar" ext="jar" conf="compile,runtime,provided,system,optional,sources,javadoc"/>
  </publications>
</ivy-module>
  }
}
