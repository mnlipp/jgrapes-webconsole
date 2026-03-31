/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2026 Michael N. Lipp
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package jdbld;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.stream.Stream;
import static java.util.jar.Attributes.Name.IMPLEMENTATION_TITLE;
import static java.util.jar.Attributes.Name.IMPLEMENTATION_VENDOR;
import static java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION;
import static jdbld.ExtProps.GitApi;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Scm;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.InvalidPatternException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.jdrupes.builder.api.BuildException;
import org.jdrupes.builder.api.DocumentationDirectory;
import org.jdrupes.builder.api.ExecResult;
import org.jdrupes.builder.api.FileTree;
import static org.jdrupes.builder.api.Intent.*;
import org.jdrupes.builder.api.MergedTestProject;
import org.jdrupes.builder.api.Project;
import org.jdrupes.builder.api.Resource;
import static org.jdrupes.builder.api.Project.Properties.Version;
import org.jdrupes.builder.api.RootProject;
import org.jdrupes.builder.api.TestResult;
import org.jdrupes.builder.core.AbstractRootProject;
import org.jdrupes.builder.core.FileTreeBuilder;
import org.jdrupes.builder.core.FileTreeBuilder.Source;
import org.jdrupes.builder.eclipse.EclipseConfiguration;
import org.jdrupes.builder.eclipse.EclipseConfigurator;
import org.jdrupes.builder.java.JavaCompiler;
import org.jdrupes.builder.java.JavaLibraryProject;
import org.jdrupes.builder.java.JavaProject;
import org.jdrupes.builder.java.JavaResourceCollector;
import org.jdrupes.builder.java.JavaResourceTree;
import org.jdrupes.builder.java.JavadocDirectory;
import static org.jdrupes.builder.java.JavaTypes.JavaResourceTreeType;
import static org.jdrupes.builder.java.JavaTypes.JavaSourceTreeType;
import org.jdrupes.builder.java.LibraryBuilder;
import org.jdrupes.builder.java.LibraryJarFile;
import org.jdrupes.builder.java.ManifestAttributes;
import org.jdrupes.builder.junit.JUnitTestRunner;
import org.jdrupes.builder.mvnrepo.JavadocJarBuilder;
import static org.jdrupes.builder.mvnrepo.MvnProperties.GroupId;
import org.jdrupes.builder.mvnrepo.MvnPublisher;
import org.jdrupes.builder.mvnrepo.MvnRepoLookup;
import org.jdrupes.builder.mvnrepo.PomFile;
import org.jdrupes.builder.mvnrepo.PomFileGenerator;
import org.jdrupes.builder.mvnrepo.SourcesJarGenerator;
import org.jdrupes.builder.ext.bnd.BndAnalyzer;
import org.jdrupes.builder.ext.bnd.BndBaseliner;
import org.jdrupes.builder.ext.nodejs.NpmExecutor;
import org.jdrupes.gitversioning.api.VersionEvaluator;
import org.jdrupes.gitversioning.core.DefaultTagFilter;
import org.jdrupes.gitversioning.core.MavenStyleTagProcessor;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class Root extends AbstractRootProject {

    @Override
    public void prepareProject(Project project) throws Exception {
        project.set(GroupId, "org.jgrapes");
        setupVersion(project);
        setupCommonGenerators(project);
        setupEclipseConfigurator(project);
    }

    public Root() throws IOException {
        super(name("JGrapes-WebConsole"));

        dependency(Expose, project(AashVueComponents.class));
        dependency(Expose, project(Base.class));
        dependency(Expose, project(Bootstrap4.class));
        dependency(Expose, project(Bootstrap4Console.class));
        dependency(Expose, project(ChartJs.class));
        dependency(Expose, project(ConsoleApp.class));
        dependency(Expose, project(Datatables.class));
        dependency(Expose, project(Fontawesome.class));
        dependency(Expose, project(Forkawesome.class));
        dependency(Expose, project(FormTest.class));
        dependency(Expose, project(JgwcVueComponents.class));
        dependency(Expose, project(Jquery.class));
        dependency(Expose, project(JqueryUi.class));
        dependency(Expose, project(JqueryUiConsole.class));
        dependency(Expose, project(JqUiTouchPunch.class));
        dependency(Expose, project(Gridstack.class));
        dependency(Expose, project(HelloSolid.class));
        dependency(Expose, project(HelloWorld.class));
        dependency(Expose, project(LocalLogin.class));
        dependency(Expose, project(Luxon.class));
        dependency(Expose, project(MarkdownDisplay.class));
        dependency(Expose, project(MarkdownIt.class));
        dependency(Expose, project(MessageBox.class));
        dependency(Expose, project(Moment.class));
        dependency(Expose, project(OidcLogin.class));
        dependency(Expose, project(Rbac.class));
        dependency(Expose, project(StyleTest.class));
        dependency(Expose, project(SysInfo.class));
        dependency(Expose, project(Vue.class));
        dependency(Expose, project(VueJs.class));
        dependency(Expose, project(VueJsConsole.class));
        dependency(Expose, project(Vuex.class));

        // For npm init
        dependency(Consume, prepareNpm(new NpmExecutor(this)));

        // Build javadoc
        generator(JGrapesJavadoc::new);

//        // Publish gh-pages
//        generator(GhPagesPublisher::new);

        // Commands
        commandAlias("build").projects("**").without("WebConsoleExample")
            .resources(of(LibraryJarFile.class).using(Supply));
        commandAlias("prepareJs")
            .resources(of(ExecResult.class).withName("prepareJs"));
        commandAlias("javadoc").resources(of(JavadocDirectory.class));
        commandAlias("apidocs").resources(of(DocumentationDirectory.class));
//        commandAlias("pomFile").resources(of(PomFile.class));
//        commandAlias("mavenPublication").resources(of(MvnPublication.class));
        commandAlias("test").resources(of(TestResult.class));
        commandAlias("eclipse").resources(of(EclipseConfiguration.class));
//        commandAlias("baseline").resources(of(BndBaselineEvaluation.class));
//        commandAlias("ghPagesPublication")
//            .resources(of(GhPagesPublication.class));
    }

    private static void setupVersion(Project project)
            throws IOException, GitAPIException, InvalidPatternException {
        if (project instanceof RootProject) {
            project.set(GitApi, Git.open(project.directory().toFile()));
        }

        // Use shortened project name for tags
        var shortened = project.name().startsWith(project.get(GroupId) + ".")
            ? project.name()
                .substring(project.<String> get(GroupId).length() + 1)
            : project.name();
        if ("manager".equals(shortened)) {
            shortened = "manager-app";
        }
        if ("http".equals(shortened)) {
            shortened = "http-base";
        }
        var tagPrefix = shortened.replace('.', '-') + "-";

        var evaluator = VersionEvaluator
            .forRepository(project.<Git> get(GitApi).getRepository())
            .tagFilter(new DefaultTagFilter().prepend(tagPrefix))
            .tagProcessor(new MavenStyleTagProcessor()
                .ignoreBranches("testing/.*", "release/.*", "develop/.*"));
        project.set(Version, evaluator.version());
    }

    private static void setupCommonGenerators(Project project) {
        if (project instanceof JavaProject) {
            if (!(project instanceof MergedTestProject)) {
                setupJavaBuild(project);
            } else {
                setupJavaTest(project);
            }
        }
        if (project instanceof JavaLibraryProject) {
            setupJavaLibraryBuild(project);
        }
    }

    private static void setupJavaBuild(Project project) {
        project.generator(JavaCompiler::new)
            .addSources(Path.of("src"), "**/*.java")
            .options("--release", "21");
        project.generator(JavaResourceCollector::new)
            .add(Path.of("resources"), "**/*");
    }

    private static void setupJavaTest(Project project) {
        project.generator(JavaCompiler::new).addSources(Path.of("test"),
            "**/*.java").options("--release", "21");
        project.generator(JavaResourceCollector::new).add(Path.of(
            "test-resources"), "**/*");
        project.dependency(Consume, new MvnRepoLookup()
            .resolve("junit:junit:4.13.2")
            .bom("org.junit:junit-bom:5.14.2")
            .resolve("org.junit.jupiter:junit-jupiter-api")
            .resolve("org.junit.jupiter:junit-jupiter-params")
            .resolve("org.junit.jupiter:junit-jupiter-engine",
                "org.junit.vintage:junit-vintage-engine",
                "net.jodah:concurrentunit:0.4.2"));
        project.dependency(Supply, JUnitTestRunner::new).syncOn(Root.class);
    }

    private static void setupJavaLibraryBuild(Project project) {
        // Generate POM
        project.generator(PomFileGenerator::new).adaptPom(model -> {
            model.setDescription("See URL.");
            model.setUrl("https://jgrapes.org/WebConsole.html");
            var scm = new Scm();
            scm.setUrl("scm:git@github.com:mnlipp/jgrapes.git");
            scm.setConnection("scm:git@github.com:mnlipp/jgrapes.git");
            scm.setDeveloperConnection("git@github.com:mnlipp/jgrapes.git");
            model.setScm(scm);
            var license = new License();
            license.setName("AGPL 3.0");
            license.setUrl("https://www.gnu.org/licenses/agpl-3.0.en.html");
            license.setDistribution("repo");
            model.setLicenses(List.of(license));
            var developer = new Developer();
            developer.setId("mnlipp");
            developer.setName("Michael N. Lipp");
            model.setDevelopers(List.of(developer));
        });

        // Build the library / bundle
        var gen = project.generator(LibraryBuilder::new)
            .addFrom(project.providers().select(Supply))
            .addAttributeValues(Map.of(
                IMPLEMENTATION_TITLE, project.name(),
                IMPLEMENTATION_VERSION, project.get(Version),
                IMPLEMENTATION_VENDOR, "Michael N. Lipp (mnl@mnl.de)")
                .entrySet().stream())
            .addManifestAttributes(project.resources(
                project.of(ManifestAttributes.class).using(Consume)))
            .addEntries(project.resources(
                project.of(PomFile.class).using(Supply))
                .map(pomFile -> Map.entry(Path.of("META-INF/maven")
                    .resolve((String) project.get(GroupId))
                    .resolve(project.name())
                    .resolve("pom.xml"), pomFile)))
            .add(Path.of("OSGI-OPT/src"), project.resources(
                project.of(JavaSourceTreeType).using(Supply)));
        var git = project.<Git> get(GitApi);
        try {
            gen.attributes(
                Map.entry(new Attributes.Name("Git-Descriptor"),
                    git.describe().setAll(true).call()
                        + (git.status().call().getUncommittedChanges()
                            .isEmpty() ? "" : "-dirty")),
                Map.entry(new Attributes.Name("Git-SHA"),
                    git.getRepository().resolve("HEAD").getName()));
        } catch (GitAPIException | RevisionSyntaxException
                | IOException e) {
            throw new BuildException().from(project).cause(e);
        }

        // Provide OSGi data
        if (project.directory().resolve("bnd.bnd").toFile().exists()) {
            String bundleVersion = project.get(Version);
            if (bundleVersion.endsWith("-SNAPSHOT")) {
                bundleVersion = bundleVersion.replaceFirst("-", ".-")
                    .replaceAll("-SNAPSHOT$", "-\\${tstamp}-SNAPSHOT");
            } else {
                bundleVersion += ".ga";
            }
            project.dependency(Consume, BndAnalyzer::new)
                .instruction("Bundle-SymbolicName", project.name())
                .instructions(project.directory().resolve("bnd.bnd"))
                .instruction("Bundle-Version", bundleVersion)
                .instructions(
                    Map.of("-diffignore", "Git-Descriptor, Git-SHA",
                        "Bundle-Version", bundleVersion));
            if (project.name().startsWith("org.jgrapes.")) {
                project.dependency(Supply, BndBaseliner::new)
                    .instruction("-diffignore", "Git-Descriptor, Git-SHA");
            }
        }

        // Supply sources jar
        project.generator(SourcesJarGenerator::new).addTrees(
            project.resources(project.of(
                JavaSourceTreeType).using(Supply, Expose)));

        // Supply javadoc jar
        project.generator(JavadocJarBuilder::new);

        if (!(project instanceof Unpublishable)) {
            // Publish (deploy). Credentials and signing information is
            // obtained through properties.
            project.generator(MvnPublisher::new);
        }
    }

    /*
     * Called from project configurations.
     */
    public static NpmExecutor prepareNpm(NpmExecutor executor) {
        executor.nodeJsVersion("25.7.0").name("npmInstall");
        var project = executor.project();
        if (!(project instanceof RootProject)) {
            executor.required(project.rootProject()
                .resources(project.of(ExecResult.class).using(Consume)
                    .withName("npmInstall")));
        }
        return executor;
    }

    public static NpmExecutor asBundleBuilder(NpmExecutor executor) {
        return prepareNpm(executor).name("bundleBuilder")
            .provideResources(executor.project().of(JavaResourceTree.class));
    }

    @SafeVarargs
    public static void asProjectBundleBuilder(NpmExecutor executor,
            Stream<? extends Resource>... dependencies) {
        asBundleBuilder(executor).args("run", "build")
            .required(Arrays.stream(dependencies).flatMap(s -> s))
            .required(Path.of("node_modules"), "**/*")
            .required(Path.of("src"), "**/*.ts")
            .required(Path.of("tsconfig.json"))
            .required(Path.of("rollup.config.mjs"))
            .output(p -> Stream.of(JavaResourceTree.of(p,
                p.buildDirectory().resolve("generated/resources"), "**/*")));
    }

    public static void addNpmResourcesBuilder(NpmExecutor executor,
            Path target, FileTree<?>... nodeSources) {
        // Pass NpmExecutor instead of project to make sure that the project
        // has set up an NpmExecutor
        var project = executor.project();
        project.dependency(Supply, FileTreeBuilder::new)
            .into(project.buildDirectory().resolve("generated/npm-resources"))
            // "Derive" sources from NpmExecutor execution result to
            // make sure that npmInstall has been executed
            .add(project.resources(project.of(ExecResult.class)
                .using(Consume, Supply).withName(executor.name()))
                .map(_ -> Arrays.stream(nodeSources)
                    .map(s -> Source.of(s).rename(p -> target.resolve(p))))
                .flatMap(s -> s))
            .provideResources(project.of(JavaResourceTreeType));
    }

    private static void setupEclipseConfigurator(Project project) {
        project.generator(new EclipseConfigurator(project)
            .adaptProjectConfiguration((Document doc,
                    Node buildSpec, Node natures) -> {
                if (project instanceof JavaProject) {
                    var cmd = buildSpec
                        .appendChild(doc.createElement("buildCommand"));
                    cmd.appendChild(doc.createElement("name"))
                        .appendChild(doc.createTextNode(
                            "net.sf.eclipsecs.core.CheckstyleBuilder"));
                    cmd.appendChild(doc.createElement("arguments"));
                    natures.appendChild(doc.createElement("nature"))
                        .appendChild(doc.createTextNode(
                            "net.sf.eclipsecs.core.CheckstyleNature"));
                    cmd = buildSpec
                        .appendChild(doc.createElement("buildCommand"));
                    cmd.appendChild(doc.createElement("name"))
                        .appendChild(doc.createTextNode(
                            "ch.acanda.eclipse.pmd.builder.PMDBuilder"));
                    cmd.appendChild(doc.createElement("arguments"));
                    natures.appendChild(doc.createElement("nature"))
                        .appendChild(doc.createTextNode(
                            "ch.acanda.eclipse.pmd.builder.PMDNature"));
                }
            }).adaptJdtCorePrefs(props -> {
                var formatterPrefs = new java.util.Properties();
                try {
                    formatterPrefs.load(Root.class.getResourceAsStream(
                        "org.eclipse.jdt.core.formatter.prefs"));
                    formatterPrefs.entrySet().stream()
                        .forEach(e -> props.put(e.getKey(), e.getValue()));
                } catch (IOException e) {
                    throw new BuildException().from(project).cause(e);
                }
            }).adaptConfiguration(() -> {
                if (!(project instanceof JavaProject)) {
                    return;
                }
                try {
                    Files.copy(
                        Root.class.getResourceAsStream("net.sf.jautodoc.prefs"),
                        project.directory()
                            .resolve(".settings/net.sf.jautodoc.prefs"),
                        StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(Root.class.getResourceAsStream("checkstyle"),
                        project.directory().resolve(".checkstyle"),
                        StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(Root.class.getResourceAsStream("eclipse-pmd"),
                        project.directory().resolve(".eclipse-pmd"),
                        StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new BuildException().from(project).cause(e);
                }
            }));
    }
}
